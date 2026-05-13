import http from 'k6/http';
import { check, fail } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const LOGIN_ID = __ENV.K6_LOGIN_ID || 'bbosong_load_user';
export const PASSWORD = __ENV.K6_PASSWORD || 'password1234';
export const IMAGE_PATH = __ENV.K6_IMAGE_PATH || 'tests/fixtures/clothes.jpg';
export const TIMEOUT = __ENV.K6_TIMEOUT || '30s';
export const ANALYSIS_AVG_THRESHOLD = __ENV.K6_ANALYSIS_AVG_THRESHOLD || '5000';
export const ANALYSIS_P95_THRESHOLD = __ENV.K6_ANALYSIS_P95_THRESHOLD || '15000';
export const ERROR_RATE_THRESHOLD = __ENV.K6_ERROR_RATE_THRESHOLD || '0.05';

const image = open(IMAGE_PATH, 'b');

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
      image: http.file(image, 'clothes.jpg', 'image/jpeg'),
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

  check(response, {
    'analysis status is 200': (res) => res.status === 200,
    'analysis response is success': (res) => res.json('isSuccess') === true,
    'analysis has material': (res) => Boolean(res.json('result.material')),
    'analysis has washing method': (res) => Boolean(res.json('result.washingMethod')),
  });
}
