#!/usr/bin/env bash
# PostToolUse hook: auto-format whatever Claude just edited.
#
# Claude Code pipes the tool call to us as JSON on stdin, e.g.
#   { "tool_name": "Edit",
#     "tool_input": { "file_path": "/repo/src/main/java/.../BookingService.java" } }
#
# Exit 0  -> fine, continue.
# Exit 2  -> stderr is fed BACK to Claude so it can fix the problem itself.
#
# This is the whole point of hooks: CLAUDE.md *asks* Claude to format.
# This *guarantees* it, whether Claude cooperates or not.

set -uo pipefail

payload=$(cat)
file=$(printf '%s' "$payload" | jq -r '.tool_input.file_path // empty')

[ -z "$file" ] && exit 0
[ -f "$file" ] || exit 0

cd "${CLAUDE_PROJECT_DIR:-$(pwd)}" || exit 0

case "$file" in
  *.java)
    echo "[hook] spotless -> $(basename "$file")" >&2
    if ! ./mvnw -q spotless:apply -DspotlessFiles="$(printf '%s' "$file" | sed 's/[.[\*^$]/\\&/g')" 2>/tmp/fmt.err; then
      echo "[hook] spotless failed:" >&2
      cat /tmp/fmt.err >&2
      exit 2          # hand the error back to Claude to fix
    fi
    ;;

  *.ts|*.tsx|*.jsx|*.css|*.json|*.md)
    echo "[hook] prettier -> $(basename "$file")" >&2
    if ! npx --no-install prettier --write "$file" >/dev/null 2>/tmp/fmt.err; then
      cat /tmp/fmt.err >&2
      exit 2
    fi
    ;;

  *) exit 0 ;;
esac

exit 0