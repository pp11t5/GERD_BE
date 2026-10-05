#!/usr/bin/env python3
import json
import os
import sys
import time
from collections import Counter

LOG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "decisions.log")
TS_FORMAT = "%Y-%m-%dT%H:%M:%S"
WINDOW_HOURS = 24
REPEAT_THRESHOLD = 3
BLOCK_THRESHOLD = 10
DANGEROUS_HOOK = "dangerous-command-guard"


def load(since: float) -> list[dict]:
    rows = []
    try:
        with open(LOG_PATH, encoding="utf-8") as f:
            for line in f:
                try:
                    row = json.loads(line)
                    ts = time.mktime(time.strptime(row["ts"], TS_FORMAT))
                except (json.JSONDecodeError, KeyError, ValueError):
                    continue
                if ts >= since:
                    rows.append(row)
    except OSError:
        pass
    return rows


def find_alerts(rows: list[dict]) -> list[str]:
    alerts = []

    repeated = Counter((r["hook"], r["decision"], r["reason"][:60]) for r in rows)
    for (hook, decision, reason), count in repeated.most_common():
        if count >= REPEAT_THRESHOLD:
            alerts.append(f"같은 규칙 반복 {count}회: [{hook}/{decision}] {reason}")

    dangerous = [r for r in rows if r["hook"] == DANGEROUS_HOOK and r["decision"] == "deny"]
    if dangerous:
        alerts.append(f"위험 명령 차단 {len(dangerous)}건: {dangerous[-1]['reason'][:60]}")

    blocked = sum(1 for r in rows if r["decision"] in ("deny", "ask"))
    if blocked >= BLOCK_THRESHOLD:
        alerts.append(f"최근 {WINDOW_HOURS}시간 deny/ask {blocked}건 — 규칙이 과한지 점검 필요")

    return alerts


def report(hours: int) -> None:
    rows = load(time.time() - hours * 3600)
    print(f"최근 {hours}시간 훅 판정 {len(rows)}건")
    if not rows:
        return

    print("\n훅 × 결정")
    for (hook, decision), count in sorted(Counter((r["hook"], r["decision"]) for r in rows).items()):
        print(f"  {hook:<26}{decision:<6}{count}")

    print("\n자주 걸린 사유 TOP 5")
    for (hook, reason), count in Counter((r["hook"], r["reason"][:70]) for r in rows).most_common(5):
        print(f"  {count:>3}회  [{hook}] {reason}")

    print("\n최근 5건")
    for r in rows[-5:]:
        print(f"  {r['ts']}  {r['hook']}/{r['decision']}  {r['reason'][:60]}")

    alerts = find_alerts(rows)
    if alerts:
        print("\n경보")
        for a in alerts:
            print(f"  ! {a}")


def alert() -> None:
    alerts = find_alerts(load(time.time() - WINDOW_HOURS * 3600))
    if alerts:
        message = "훅 판정 로그 경보 (최근 24시간)\n" + "\n".join(f"- {a}" for a in alerts)
        print(json.dumps({"systemMessage": message}, ensure_ascii=False))


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "report"
    if mode == "alert":
        alert()
    else:
        hours = int(sys.argv[2]) if len(sys.argv) > 2 else WINDOW_HOURS
        report(hours)


if __name__ == "__main__":
    main()
