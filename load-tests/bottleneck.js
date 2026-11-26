import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, baseThresholds } from './lib/config.js';
import { issueTokens, tokenFor, authParams } from './lib/auth.js';

const KEYWORDS = ['된장찌개', '김치', '밥', '라면', '치킨', '우유', '커피', '빵'];
const RATE = Number(__ENV.RATE || 20);
const INCLUDE_JUDGMENT = __ENV.INCLUDE_JUDGMENT === 'true';
const FOOD_EXTERNAL_ID = __ENV.FOOD_EXTERNAL_ID || '';

const endpointThresholds = {
  'http_req_duration{name:GET /foods/search}': ['p(95)<800'],
  'http_req_duration{name:GET /timeline/monthly}': ['p(95)<800'],
  'http_req_duration{name:GET /dictionary/safe}': ['p(95)<800'],
  'http_req_duration{name:GET /my-page/reports}': ['p(95)<800'],
};

// 도착률 기반이라 응답이 느려져도 요청 수가 줄지 않고 쌓인다. 병목 API가 드러난다.
export const options = {
  scenarios: {
    bottleneck: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { duration: '1m', target: RATE },
        { duration: '3m', target: RATE },
        { duration: '1m', target: RATE * 2 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: { ...baseThresholds, ...endpointThresholds },
};

export function setup() {
  return { tokens: issueTokens() };
}

function get(path, token, name) {
  const res = http.get(`${BASE_URL}${path}`, authParams(token, name));
  check(res, { [`${name} 200`]: (r) => r.status === 200 });
}

export default function (data) {
  const token = tokenFor(data.tokens);
  const month = new Date().toISOString().slice(0, 7);
  const keyword = KEYWORDS[Math.floor(Math.random() * KEYWORDS.length)];
  const pick = Math.random();

  if (pick < 0.4) {
    get(`/api/v1/foods/search?q=${encodeURIComponent(keyword)}&size=20`, token, 'GET /foods/search');
  } else if (pick < 0.6) {
    get(`/api/v1/timeline/monthly?month=${month}`, token, 'GET /timeline/monthly');
  } else if (pick < 0.8) {
    get('/api/v1/dictionary/safe?size=20', token, 'GET /dictionary/safe');
  } else if (pick < 0.95) {
    get('/api/v1/my-page/reports', token, 'GET /my-page/reports');
  } else if (INCLUDE_JUDGMENT && FOOD_EXTERNAL_ID) {
    // LLM 호출이 걸릴 수 있어 기본은 제외. 캐시 히트 여부에 따라 비용과 지연이 크게 달라진다.
    get(`/api/v1/foods/${FOOD_EXTERNAL_ID}/judgment`, token, 'GET /foods/judgment');
  }
}
