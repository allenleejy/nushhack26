#!/usr/bin/env bash
# Puts the server back to the start of the demo (docs/spec.md §2). Works in Git Bash on the laptop.
#
#   bash scripts/reset-demo.sh            deletes every signed handover, alert and relayed offer;
#                                         each camera is "In store" with Siti again. Phones stay
#                                         enrolled: on each, Debug -> Clear local data -> Sync now.
#   bash scripts/reset-demo.sh --reenrol  also withdraws every enrolled phone. On its next sync
#                                         each phone wipes itself and shows Enrol (codes in §8).
#                                         It asks first; add --yes to skip the question.
#
# Reads SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY and DEMO_RESET_TOKEN from local.properties,
# else from environment variables. It calls the demo_reset function
# (supabase/migrations/20261107120000_demo_reset.sql), which checks the token.
set -euo pipefail

withdraw=false
yes=false
for arg in "$@"; do
  case "$arg" in
    --reenrol) withdraw=true ;;
    --yes) yes=true ;;
    *) echo "usage: $0 [--reenrol [--yes]]" >&2; exit 1 ;;
  esac
done

root="$(cd "$(dirname "$0")/.." && pwd)"
prop() { [ -f "$root/local.properties" ] && grep -m1 "^$1=" "$root/local.properties" | cut -d= -f2- | tr -d '\r' || true; }
url="$(prop SUPABASE_URL)";             url="${url:-${SUPABASE_URL:-}}"
key="$(prop SUPABASE_PUBLISHABLE_KEY)"; key="${key:-${SUPABASE_PUBLISHABLE_KEY:-}}"
token="$(prop DEMO_RESET_TOKEN)";       token="${token:-${DEMO_RESET_TOKEN:-}}"
[ -n "$url" ] && [ -n "$key" ] && [ -n "$token" ] || {
  echo "Set SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY and DEMO_RESET_TOKEN in local.properties." >&2; exit 1; }

if [ "$withdraw" = true ] && [ "$yes" = false ]; then
  read -r -p "Withdraw every enrolled phone too? Each will have to enrol again. [y/N] " ok
  [ "$ok" = y ] || [ "$ok" = Y ] || { echo "Nothing changed."; exit 0; }
fi

body=$(printf '{"p_token":"%s","p_withdraw":%s}' "$token" "$withdraw")
out=$(curl -sS -X POST "${url%/}/rest/v1/rpc/demo_reset" \
  -H "apikey: $key" -H "Content-Type: application/json" -d "$body")
echo "$out"

case "$out" in
  *'"result":"reset"'*|*'"result": "reset"'*) ;;
  *) echo "Reset failed." >&2; exit 1 ;;
esac

if [ "$withdraw" = true ]; then
  echo "Now on each phone: open Tenet and let it sync (or Sync now). It shows Enrol: enrol it as its person."
else
  echo "Now on each phone: Me -> tap the version 5 times -> Debug -> Clear local data -> Sync now."
fi
