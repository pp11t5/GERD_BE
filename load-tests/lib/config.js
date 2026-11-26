export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';
export const NICKNAMES = (__ENV.NICKNAMES || '')
  .split(',')
  .map((name) => name.trim())
  .filter((name) => name.length > 0);

const P95_MS = Number(__ENV.P95_MS || 500);
const P99_MS = Number(__ENV.P99_MS || 1000);

export const baseThresholds = {
  http_req_failed: ['rate<0.01'],
  http_req_duration: [`p(95)<${P95_MS}`, `p(99)<${P99_MS}`],
  checks: ['rate>0.99'],
};
