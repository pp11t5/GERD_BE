import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, baseThresholds } from './lib/config.js';
import { issueTokens, tokenFor, authParams } from './lib/auth.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: baseThresholds,
};

export function setup() {
  return { tokens: issueTokens() };
}

export default function (data) {
  const token = tokenFor(data.tokens);

  const health = http.get(`${BASE_URL}/actuator/health`, { tags: { name: 'GET /actuator/health' } });
  check(health, { 'health 200': (r) => r.status === 200 });

  const me = http.get(`${BASE_URL}/api/v1/auth/me`, authParams(token, 'GET /auth/me'));
  check(me, { 'me 200': (r) => r.status === 200 });

  sleep(1);
}
