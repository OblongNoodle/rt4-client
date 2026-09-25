#!/usr/bin/env bash
# Smoke/soak test for party.php: two members in a throwaway room, then one
# member polling once a second to check nothing (ModSecurity, Imunify360)
# starts blocking a steady request rate.
#
# Usage: ./test.sh [url] [seconds]
set -u
URL="${1:-https://party.oblongnoodle.com/party.php}"
SECS="${2:-60}"
UA="RT4-PartyPanel/1.0"

room=$(printf 'party-test-%s' "$RANDOM$RANDOM" | sha256sum | cut -c1-64)
a=$(head -c16 /dev/urandom | od -An -tx1 | tr -d ' \n')
b=$(head -c16 /dev/urandom | od -An -tx1 | tr -d ' \n')

post() {
    curl -s ${CURL_EXTRA:-} -A "$UA" -H 'Content-Type: application/json' -w '\n%{http_code}' --data "$1" "$URL"
}

echo "== health"; curl -s ${CURL_EXTRA:-} -A "$UA" -w '  [%{http_code}]\n' "$URL"
echo "== A joins with inventory"
post "{\"room\":\"$room\",\"member\":\"$a\",\"name\":\"TesterA\",\"since\":0,\"patch\":{\"inv\":[[995,1000],[-1,0]],\"prayers\":5}}"; echo
echo "== B joins, should see A"
post "{\"room\":\"$room\",\"member\":\"$b\",\"name\":\"TesterB\",\"since\":0,\"patch\":{\"skills\":[[99,99,13034431]]}}"; echo
echo "== invalid patch key, expect 400"
post "{\"room\":\"$room\",\"member\":\"$a\",\"name\":\"TesterA\",\"since\":0,\"patch\":{\"click\":1}}"; echo

echo "== soak: $SECS polls at 1/s"
declare -A codes=()
for ((i = 0; i < SECS; i++)); do
    code=$(post "{\"room\":\"$room\",\"member\":\"$a\",\"name\":\"TesterA\",\"since\":0,\"patch\":null}" | tail -n1)
    codes[$code]=$(( ${codes[$code]:-0} + 1 ))
    sleep 1
done
for c in "${!codes[@]}"; do echo "HTTP $c x ${codes[$c]}"; done

post "{\"room\":\"$room\",\"member\":\"$a\",\"name\":\"TesterA\",\"since\":0,\"leave\":true}" >/dev/null
post "{\"room\":\"$room\",\"member\":\"$b\",\"name\":\"TesterB\",\"since\":0,\"leave\":true}" >/dev/null
echo "== done (test members removed)"
