import { sleep } from 'k6';
import {
  ANALYSIS_P95_THRESHOLD,
  ERROR_RATE_THRESHOLD,
  analyzeClothes,
  parseStages,
  setupAuth,
} from './common.js';

export const options = {
  stages: parseStages(__ENV.K6_LOAD_STAGES, '1:30s,2:30s,3:30s,4:30s'),
  thresholds: {
    analysis_latency: [`p(95)<${ANALYSIS_P95_THRESHOLD}`],
    analysis_errors: [`rate<${ERROR_RATE_THRESHOLD}`],
  },
};

export function setup() {
  return setupAuth();
}

export default function (data) {
  analyzeClothes(data);
  sleep(1);
}
