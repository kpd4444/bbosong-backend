import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
  BASE_URL,
  ERROR_RATE_THRESHOLD,
  TIMEOUT,
  mimeTypeFromPath,
  setupAuth,
} from './common.js';

const EXPECTED_PATH = __ENV.K6_EXPECTED_PATH || 'tests/fixtures/expected.json';
const SCORE_THRESHOLD = Number(__ENV.K6_ACCURACY_SCORE_THRESHOLD || '80');
const CATEGORY_MATCH_THRESHOLD = Number(__ENV.K6_CATEGORY_MATCH_THRESHOLD || '0.8');

const testCases = JSON.parse(open(EXPECTED_PATH));
const fixtures = {};

for (const testCase of testCases) {
  const caseId = testCase.caseId || testCase.id;
  const imagePath = testCase.image.includes('/') ? testCase.image : `tests/fixtures/${testCase.image}`;
  fixtures[caseId] = {
    ...testCase,
    caseId,
    imageBytes: open(imagePath, 'b'),
  };
}

export const analysisErrors = new Rate('analysis_errors');
export const accuracyScore = new Trend('accuracy_score');
export const categoryMatchRate = new Rate('category_match_rate');
export const accuracyPassRate = new Rate('accuracy_pass_rate');
export const criticalErrorRate = new Rate('critical_error_rate');
export const missingWarningCount = new Counter('missing_warning_count');

export const options = {
  vus: Number(__ENV.K6_ACCURACY_VUS || '1'),
  iterations: Number(__ENV.K6_ACCURACY_ITERATIONS || String(testCases.length)),
  thresholds: {
    analysis_errors: [`rate<${ERROR_RATE_THRESHOLD}`],
    accuracy_score: [`avg>=${SCORE_THRESHOLD}`],
    category_match_rate: [`rate>=${CATEGORY_MATCH_THRESHOLD}`],
    accuracy_pass_rate: [`rate>=${CATEGORY_MATCH_THRESHOLD}`],
    critical_error_rate: ['rate==0'],
  },
};

export function setup() {
  return setupAuth();
}

export default function (data) {
  const sourceCase = testCases[__ITER % testCases.length];
  const testCase = fixtures[sourceCase.caseId || sourceCase.id];
  const response = analyzeExpectedClothes(data.accessToken, testCase);
  const body = parseJsonBody(response);
  const result = body?.result;
  const prediction = buildPrediction(result);
  const evaluation = evaluatePrediction(testCase, prediction);
  const categoryMatches = result?.categoryName === testCase.groundTruth.categoryName;
  const passed =
    response.status === 200
    && body?.isSuccess === true
    && evaluation.score >= SCORE_THRESHOLD
    && evaluation.criticalErrors.length === 0;

  analysisErrors.add(response.status !== 200);
  accuracyScore.add(evaluation.score);
  categoryMatchRate.add(categoryMatches);
  accuracyPassRate.add(passed);
  criticalErrorRate.add(evaluation.criticalErrors.length > 0);
  missingWarningCount.add(evaluation.missingWarnings.length);

  if (!passed) {
    console.error(
      `[label-safety failed] caseId=${testCase.caseId}, status=${response.status}, score=${evaluation.score}, criticalErrors=${JSON.stringify(evaluation.criticalErrors)}, missingWarnings=${JSON.stringify(evaluation.missingWarnings)}, prediction=${JSON.stringify(prediction)}, body=${response.body || '<empty>'}`,
    );
  }

  check(response, {
    'analysis status is 200': (res) => res.status === 200,
    'analysis response is success': () => body?.isSuccess === true,
    'label safety result exists': () => Boolean(result),
    'label safety category matches': () => categoryMatches,
    'label safety score is passing': () => evaluation.score >= SCORE_THRESHOLD,
    'label safety has no critical errors': () => evaluation.criticalErrors.length === 0,
  });
}

function analyzeExpectedClothes(accessToken, testCase) {
  return http.post(
    `${BASE_URL}/api/clothes/analysis`,
    {
      image: http.file(testCase.imageBytes, testCase.image, mimeTypeFromPath(testCase.image)),
    },
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: TIMEOUT,
      tags: { endpoint: 'clothes-analysis', accuracy_case: testCase.caseId },
    },
  );
}

function parseJsonBody(response) {
  if (!response.body) {
    return null;
  }

  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function buildPrediction(result) {
  if (!result) {
    return null;
  }

  const washingText = `${result.washingMethod || ''} ${result.caution || ''}`;
  const fallback = {
    waterWash: inferWaterWash(washingText),
    maxWaterTemperature: inferMaxWaterTemperature(washingText),
    bleachAllowed: inferAllowed(washingText, ['표백'], ['금지', '피', '사용하지', '사용 금지', '변색'], ['가능', '사용 가능']),
    dryerAllowed: inferAllowed(washingText, ['건조기'], ['금지', '피', '사용하지', '자연 건조', '그늘', '수축'], ['가능', '사용 가능']),
    ironAllowed: inferAllowed(washingText, ['다림질'], ['금지', '피', '필요하지', '낮은 온도'], ['가능', '사용 가능']),
    dryCleanAllowed: inferAllowed(washingText, ['드라이클리닝', '드라이'], ['금지', '피', '필요하지'], ['가능', '권장']),
    handWashRequired: inferRequired(washingText, ['손세탁'], ['필요', '권장', '하는 것이 좋', '해야']),
    separateWashRequired: inferRequired(washingText, ['단독 세탁', '분리 세탁', '따로 세탁'], ['필요', '권장', '하는 것이 좋', '해야']),
  };

  return {
    categoryName: result.categoryName || null,
    ...fallback,
    ...(result.washRules || {}),
  };
}

function evaluatePrediction(testCase, prediction) {
  const groundTruth = testCase.groundTruth;
  const riskItems = testCase.riskItems || [];
  const missingWarnings = [];
  const criticalErrors = [];
  let score = 0;

  if (!prediction) {
    return { score, criticalErrors: ['responseMissing'], missingWarnings };
  }

  score += compareBoolean(prediction.waterWash, groundTruth.waterWash, 20);
  score += compareTemperature(prediction.maxWaterTemperature, groundTruth.maxWaterTemperature, 20);
  score += compareBoolean(prediction.bleachAllowed, groundTruth.bleachAllowed, 15);
  score += compareBoolean(prediction.dryerAllowed, groundTruth.dryerAllowed, 15);
  score += compareBoolean(prediction.ironAllowed, groundTruth.ironAllowed, 10);
  score += compareBoolean(prediction.dryCleanAllowed, groundTruth.dryCleanAllowed, 10);
  score += compareBoolean(prediction.handWashRequired, groundTruth.handWashRequired, 5);
  score += compareBoolean(prediction.separateWashRequired, groundTruth.separateWashRequired, 5);

  for (const item of riskItems) {
    if (prediction[item] === null || prediction[item] === undefined) {
      missingWarnings.push(item);
      continue;
    }

    if (isCriticalError(item, prediction[item], groundTruth[item])) {
      criticalErrors.push(item);
    }
  }

  return { score, criticalErrors, missingWarnings };
}

function compareBoolean(predicted, expected, points) {
  if (expected === null || expected === undefined) {
    return points;
  }

  return predicted === expected ? points : 0;
}

function compareTemperature(predicted, expected, points) {
  if (expected === null || expected === undefined) {
    return points;
  }

  if (predicted === null || predicted === undefined) {
    return 0;
  }

  return predicted <= expected ? points : 0;
}

function isCriticalError(item, predicted, expected) {
  if (item === 'maxWaterTemperature') {
    return typeof predicted === 'number' && typeof expected === 'number' && predicted > expected;
  }

  return expected === false && predicted === true;
}

function inferWaterWash(text) {
  if (containsAny(text, ['물세탁 금지', '물 세탁 금지', '세탁 금지'])) {
    return false;
  }

  if (containsAny(text, ['세탁', '미온수', '찬물', '30도', '40도'])) {
    return true;
  }

  return null;
}

function inferMaxWaterTemperature(text) {
  const matches = [...text.matchAll(/(\d{2})\s*도/g)].map((match) => Number(match[1]));

  if (matches.length === 0) {
    return null;
  }

  return Math.max(...matches);
}

function inferAllowed(text, subjects, denyWords, allowWords) {
  const subjectIndexes = subjects.flatMap((subject) => indexesOf(text, subject));

  if (subjectIndexes.length === 0) {
    return null;
  }

  if (subjectIndexes.some((index) => hasNearby(text, index, denyWords))) {
    return false;
  }

  if (subjectIndexes.some((index) => hasNearby(text, index, allowWords))) {
    return true;
  }

  return null;
}

function inferRequired(text, subjects, requiredWords) {
  const subjectIndexes = subjects.flatMap((subject) => indexesOf(text, subject));

  if (subjectIndexes.length === 0) {
    return false;
  }

  return subjectIndexes.some((index) => hasNearby(text, index, requiredWords));
}

function hasNearby(text, index, words) {
  const window = text.slice(Math.max(0, index - 20), index + 40);
  return containsAny(window, words);
}

function indexesOf(text, keyword) {
  const indexes = [];
  let index = text.indexOf(keyword);

  while (index !== -1) {
    indexes.push(index);
    index = text.indexOf(keyword, index + keyword.length);
  }

  return indexes;
}

function containsAny(value, keywords = []) {
  if (!value) {
    return false;
  }

  return keywords.some((keyword) => value.includes(keyword));
}
