import { sleep } from 'k6';
import {
  ANALYSIS_AVG_THRESHOLD,
  ANALYSIS_P95_THRESHOLD,
  ERROR_RATE_THRESHOLD,
  analyzeClothes,
  setupAuth,
} from './common.js';

export const options = {
  vus: Number(__ENV.K6_BASELINE_VUS || '1'),
  duration: __ENV.K6_BASELINE_DURATION || '1m',
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
