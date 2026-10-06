#!/usr/bin/env python3
import json
import sys

import _hook_common as hc


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return

    file_path = payload.get("tool_input", {}).get("file_path", "").replace("\\", "/")

    if "/dto/" in file_path:
        hc.ask(
            "dto-guard",
            f"DTO 파일 편집 시도: {file_path} — 사용자가 명시적으로 요청한 게 맞는지 확인하세요.",
            file_path,
        )


if __name__ == "__main__":
    main()
