import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, baseThresholds } from './lib/config.js';
import { issueTokens, tokenFor, authParams } from './lib/auth.js';

const KEYWORDS = ['된장찌개', '김치', '밥', '라면', '치킨', '우유'];
const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);

export const options = {
  stages: [
    { duration: '1m', target: TARGET_VUS },
    { duration: '3m', target: TARGET_VUS },
    { duration: '30s', target: 0 },
  ],
  thresholds: baseThresholds,
};

export function setup() {
  return { tokens: issueTokens() };
}

function get(path, token, name) {
  const res = http.get(`${BASE_URL}${path}`, authParams(token, name));
  check(res, { [`${name} 200`]: (r) => r.status === 200 });
  return res;
}

// 앱 진입부터 음식 검색, 타임라인, 마이페이지까지 이어지는 읽기 중심 흐름
export default function (data) {
  const token = tokenFor(data.tokens);
  const today = new Date().toISOString().slice(0, 10);
  const keyword = KEYWORDS[Math.floor(Math.random() * KEYWORDS.length)];

  group('home', () => {
    get('/api/v1/onboarding/status', token, 'GET /onboarding/status');
    get('/api/v1/home/recommendations', token, 'GET /home/recommendations');
  });
  sleep(1);

  group('search', () => {
    get(`/api/v1/foods/search?q=${encodeURIComponent(keyword)}&size=10`, token, 'GET /foods/search');
    get('/api/v1/foods/recent', token, 'GET /foods/recent');
  });
  sleep(1);

  group('record', () => {
    get(`/api/v1/timeline?date=${today}`, token, 'GET /timeline');
    get('/api/v1/my-page/summary', token, 'GET /my-page/summary');
  });
  sleep(Math.random() * 2 + 1);
}
