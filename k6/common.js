import http from 'k6/http';
import { check, fail } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const LOGIN_ID = __ENV.K6_LOGIN_ID || 'bbosong_load_user';
export const PASSWORD = __ENV.K6_PASSWORD || 'password1234';
export const IMAGE_PATH = __ENV.K6_IMAGE_PATH || 'tests/fixtures/black_jacket_001.png';
export const TIMEOUT = __ENV.K6_TIMEOUT || '30s';
export const ANALYSIS_AVG_THRESHOLD = __ENV.K6_ANALYSIS_AVG_THRESHOLD || '15000';
export const ANALYSIS_P95_THRESHOLD = __ENV.K6_ANALYSIS_P95_THRESHOLD || '20000';
export const ERROR_RATE_THRESHOLD = __ENV.K6_ERROR_RATE_THRESHOLD || '0.05';
export const FAILURE_LOG_LIMIT = Number(__ENV.K6_FAILURE_LOG_LIMIT || '5');

const image = open(IMAGE_PATH, 'b');
let failureLogCount = 0;

export const analysisLatency = new Trend('analysis_latency', true);
export const analysisErrors = new Rate('analysis_errors');

export function parseStages(value, fallback) {
  return (value || fallback).split(',').map((stage) => {
    const [target, duration] = stage.trim().split(':');
    return {
      target: Number(target),
      duration,
    };
  });
}

export function setupAuth() {
  const firstLoginRes = login();
  if (hasAccessToken(firstLoginRes)) {
    return {
      accessToken: firstLoginRes.json('result.accessToken'),
    };
  }

  const signupPayload = JSON.stringify({
    loginId: LOGIN_ID,
    password: PASSWORD,
    email: `${LOGIN_ID}@example.com`,
  });

  const signupRes = http.post(`${BASE_URL}/api/auth/signup/local`, signupPayload, {
    headers: { 'Content-Type': 'application/json' },
    timeout: TIMEOUT,
    tags: { endpoint: 'signup' },
  });

  check(signupRes, {
    'signup succeeded or account already exists': (res) => res.status === 200 || res.status === 400,
  });

  const loginRes = login();
  const loginOk = check(loginRes, {
    'login succeeded after setup': hasAccessToken,
  });

  if (!loginOk) {
    fail(
      `login failed after auth setup: firstLoginStatus=${firstLoginRes.status}, signupStatus=${signupRes.status}, loginStatus=${loginRes.status}, loginBody=${loginRes.body}`,
    );
  }

  return {
    accessToken: loginRes.json('result.accessToken'),
  };
}

function login() {
  return http.post(
    `${BASE_URL}/api/auth/login/local`,
    JSON.stringify({ loginId: LOGIN_ID, password: PASSWORD }),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: TIMEOUT,
      tags: { endpoint: 'login' },
    },
  );
}

function hasAccessToken(response) {
  return response.status === 200 && Boolean(response.json('result.accessToken'));
}

export function analyzeClothes(data) {
  const response = http.post(
    `${BASE_URL}/api/clothes/analysis`,
    {
      image: http.file(image, fileNameFromPath(IMAGE_PATH), mimeTypeFromPath(IMAGE_PATH)),
    },
    {
      headers: {
        Authorization: `Bearer ${data.accessToken}`,
      },
      timeout: TIMEOUT,
      tags: { endpoint: 'clothes-analysis' },
    },
  );

  analysisLatency.add(response.timings.duration);
  analysisErrors.add(response.status !== 200);

  if (response.status !== 200 && failureLogCount < FAILURE_LOG_LIMIT) {
    failureLogCount += 1;
    console.error(`[analysis failed] status=${response.status}, body=${response.body || '<empty>'}`);
  }

  const body = parseJsonBody(response);

  check(response, {
    'analysis status is 200': (res) => res.status === 200,
    'analysis response is success': () => body?.isSuccess === true,
    'analysis has material': () => Boolean(body?.result?.material),
    'analysis has washing method': () => Boolean(body?.result?.washingMethod),
  });
}

export function fileNameFromPath(path) {
  return path.split('/').pop();
}

export function mimeTypeFromPath(path) {
  const lowerPath = path.toLowerCase();

  if (lowerPath.endsWith('.png')) {
    return 'image/png';
  }
  if (lowerPath.endsWith('.jpg') || lowerPath.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  return 'application/octet-stream';
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
