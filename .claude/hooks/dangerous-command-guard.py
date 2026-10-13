#!/usr/bin/env python3
import json
import re
import sys

import _hook_common as hc


def is_force_push(command: str) -> bool:
    if not re.search(r"\bgit\s+push\b", command):
        return False
    if "--force-with-lease" in command:
        return False
    if re.search(r"--force\b", command):
        return True
    return bool(re.search(r"(?<!\S)-f(?!\S)", command))


def is_hard_reset(command: str) -> bool:
    return bool(re.search(r"\bgit\s+reset\s+--hard\b", command))


def is_force_clean(command: str) -> bool:
    if not re.search(r"\bgit\s+clean\b", command):
        return False
    return bool(re.search(r"-[a-z]*f", command))


def is_rm_rf(command: str) -> bool:
    if not re.search(r"\brm\s+", command):
        return False
    return bool(re.search(r"-[a-z]*r[a-z]*f[a-z]*\b|-[a-z]*f[a-z]*r[a-z]*\b", command))


CHECKS = (
    (is_force_push, "git push --force"),
    (is_hard_reset, "git reset --hard"),
    (is_force_clean, "git clean -f"),
    (is_rm_rf, "rm -rf"),
)


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return

    command = payload.get("tool_input", {}).get("command", "")

    for check, label in CHECKS:
        if check(command):
            hc.deny(
                "dangerous-command-guard",
                f"되돌리기 어려운 명령({label})은 차단됩니다. 정말 필요하면 터미널에서 직접 실행하세요.",
                command,
            )
            return


if __name__ == "__main__":
    main()
