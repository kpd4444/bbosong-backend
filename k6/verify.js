import { sleep } from 'k6';
import {
  ANALYSIS_AVG_THRESHOLD,
  ANALYSIS_P95_THRESHOLD,
  ERROR_RATE_THRESHOLD,
  analyzeClothes,
  parseStages,
  setupAuth,
} from './common.js';

export const options = {
  stages: parseStages(__ENV.K6_LOAD_STAGES, '50:2m,100:2m,200:2m,300:2m,500:2m'),
  thresholds: {
    analysis_latency: [`avg<${ANALYSIS_AVG_THRESHOLD}`, `p(95)<${ANALYSIS_P95_THRESHOLD}`],
    analysis_errors: [`rate<${ERROR_RATE_THRESHOLD}`],
    'http_req_duration{endpoint:clothes-analysis}': [
      `avg<${ANALYSIS_AVG_THRESHOLD}`,
      `p(95)<${ANALYSIS_P95_THRESHOLD}`,
    ],
  },
};

export function setup() {
  return setupAuth();
}

export default function (data) {
  analyzeClothes(data);
  sleep(1);
}
