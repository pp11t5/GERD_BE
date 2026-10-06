#!/usr/bin/env python3
import json
import re
import sys

import _hook_common as hc

CONVENTIONAL_TYPES = (
    "feat", "fix", "docs", "style", "refactor", "perf", "test", "build", "ci", "chore", "revert",
)

PR_TEMPLATE_SECTIONS = (
    "작업 내용 요약",
    "Issue 번호",
    "작업 목록",
)


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "commit"

    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return

    command = payload.get("tool_input", {}).get("command", "")

    if re.search(r"co-authored-by", command, re.IGNORECASE):
        hc.deny("commit-guard", "Co-Authored-By 트레일러는 금지되어 있습니다. 커밋/PR 메시지에서 제거한 뒤 다시 시도하세요.", command)

    if mode == "commit" and re.search(r"\bgit\s+commit\b", command):
        types = "|".join(CONVENTIONAL_TYPES)
        pattern = rf"({types})(\([^)]*\))?:\s+\S"
        if not re.search(pattern, command):
            hc.deny(
                "commit-guard",
                "커밋 메시지가 conventional commit 형식(예: fix: 설명)이 아닙니다. "
                f"허용 타입: {', '.join(CONVENTIONAL_TYPES)}",
                command,
            )

    if mode == "pr" and re.search(r"\bgh\s+pr\s+create\b", command):
        missing = [s for s in PR_TEMPLATE_SECTIONS if s not in command]
        if missing:
            hc.warn(
                "commit-guard",
                "PR 본문이 .github/PULL_REQUEST_TEMPLATE.md 형식과 다른 것 같습니다. "
                f"누락된 섹션: {', '.join(missing)}",
            )


if __name__ == "__main__":
    main()
