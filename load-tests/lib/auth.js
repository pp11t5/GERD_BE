import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL, ACCESS_TOKEN, NICKNAMES } from './config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// setup()에서 한 번만 호출해 토큰 목록을 만든다.
// ACCESS_TOKEN이 있으면 그 토큰 하나를 쓰고, 없으면 NICKNAMES 계정마다 dev-login으로 발급한다.
export function issueTokens() {
  if (ACCESS_TOKEN) {
    return [ACCESS_TOKEN];
  }
  if (NICKNAMES.length === 0) {
    fail('ACCESS_TOKEN 또는 NICKNAMES 환경변수가 필요합니다. (load-tests/README.md 참고)');
  }
  return NICKNAMES.map((nickname) => {
    const res = http.post(
      `${BASE_URL}/api/v1/auth/dev-login`,
      JSON.stringify({ nickname }),
      { headers: JSON_HEADERS, tags: { name: 'POST /auth/dev-login' } },
    );
    const ok = check(res, { 'dev-login 200': (r) => r.status === 200 });
    if (!ok) {
      fail(`dev-login 실패: nickname=${nickname}, status=${res.status}`);
    }
    return res.json('result.accessToken');
  });
}

// VU마다 토큰 하나를 고정해서 쓴다.
export function tokenFor(tokens) {
  return tokens[(__VU - 1) % tokens.length];
}

export function authParams(token, name) {
  return {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name },
  };
}
