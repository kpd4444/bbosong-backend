import { sleep } from 'k6';
import {
  ERROR_RATE_THRESHOLD,
  analyzeClothes,
  parseStages,
  setupAuth,
} from './common.js';

export const options = {
  stages: parseStages(__ENV.K6_STRESS_STAGES, '1:1m,3:1m,5:1m,10:1m'),
  thresholds: {
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
