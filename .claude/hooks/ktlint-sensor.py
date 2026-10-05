#!/usr/bin/env python3
import json
import os
import subprocess
import sys

import _hook_common as hc


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except json.JSONDecodeError:
        return

    file_path = payload.get("tool_input", {}).get("file_path", "")
    if not file_path.endswith(".kt"):
        return
    if not os.path.isfile(file_path):
        return

    try:
        result = subprocess.run(
            ["ktlint", "--relative", file_path],
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return

    if result.returncode != 0:
        hc.log_decision("ktlint-sensor", "flag", "ktlint violation", file_path)
        print(result.stdout.strip(), file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
