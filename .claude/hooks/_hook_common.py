#!/usr/bin/env python3
import json
import os
import sys
import time

LOG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "decisions.log")
MAX_LOG_BYTES = 1_000_000
MAX_DETAIL_CHARS = 300


def rotate_log() -> None:
    if os.path.getsize(LOG_PATH) > MAX_LOG_BYTES:
        os.replace(LOG_PATH, LOG_PATH + ".1")


def log_decision(hook: str, decision: str, reason: str, detail: str = "") -> None:
    detail = detail[:MAX_DETAIL_CHARS]
    try:
        if os.path.exists(LOG_PATH):
            rotate_log()
        with open(LOG_PATH, "a", encoding="utf-8") as f:
            f.write(json.dumps({
                "ts": time.strftime("%Y-%m-%dT%H:%M:%S"),
                "hook": hook,
                "decision": decision,
                "reason": reason,
                "detail": detail,
            }, ensure_ascii=False) + "\n")
    except OSError:
        pass


def deny(hook: str, reason: str, detail: str = "") -> None:
    log_decision(hook, "deny", reason, detail)
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)


def ask(hook: str, reason: str, detail: str = "") -> None:
    log_decision(hook, "ask", reason, detail)
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "ask",
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)


def warn(hook: str, message: str) -> None:
    log_decision(hook, "warn", message)
    print(json.dumps({"systemMessage": message}))
