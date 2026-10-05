---
description: 훅 판정 로그(decisions.log) 집계와 경보 확인
argument-hint: "[조회 기간(시간), 기본 24]"
allowed-tools: Bash(python3 .claude/hooks/decisions-report.py *)
---

`python3 .claude/hooks/decisions-report.py report $ARGUMENTS`를 실행하고 결과를 요약한다. 경보가 있으면 어떤 규칙이 왜 반복됐는지, 규칙을 고쳐야 할지 오탐인지 짚어준다.
