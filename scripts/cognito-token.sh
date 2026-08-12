#!/usr/bin/env bash
set -euo pipefail

USERNAME="${1:-}"
if [[ -z "$USERNAME" ]]; then
    echo "uso: $0 <usuario-de-cognito>   (por ejemplo: manager_josue)" >&2
    exit 64
fi

ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
if [[ -f "$ENV_FILE" ]]; then
    REGION="$(grep -E '^COGNITO_REGION=' "$ENV_FILE" | tail -1 | cut -d= -f2- || true)"
    CLIENT_ID="$(grep -E '^COGNITO_APP_CLIENT_ID=' "$ENV_FILE" | tail -1 | cut -d= -f2- || true)"
fi
REGION="${COGNITO_REGION:-${REGION:-us-east-1}}"
CLIENT_ID="${COGNITO_APP_CLIENT_ID:-${CLIENT_ID:-1n48tn47edu17qg12cj1hsohfm}}"
ENDPOINT="https://cognito-idp.${REGION}.amazonaws.com/"

if [[ -z "${COGNITO_CLIENT_SECRET:-}" ]]; then
    read -rsp "client secret de ${CLIENT_ID}: " COGNITO_CLIENT_SECRET; echo >&2
fi
if [[ -z "${COGNITO_PASSWORD:-}" ]]; then
    read -rsp "clave de ${USERNAME}: " COGNITO_PASSWORD; echo >&2
fi

SECRET_HASH="$(printf '%s' "${USERNAME}${CLIENT_ID}" \
    | openssl dgst -sha256 -hmac "$COGNITO_CLIENT_SECRET" -binary \
    | base64)"

call_cognito() {
    curl -sS -X POST "$ENDPOINT" \
        -H 'Content-Type: application/x-amz-json-1.1' \
        -H "X-Amz-Target: AWSCognitoIdentityProviderService.$1" \
        -d "$2"
}

json_get() {
    python3 -c '
import json, sys
data = json.load(sys.stdin)
for key in sys.argv[1].split("."):
    data = (data or {}).get(key)
print(data if data is not None else "")
' "$1"
}

INIT="$(call_cognito InitiateAuth "$(cat <<JSON
{
  "AuthFlow": "USER_AUTH",
  "ClientId": "${CLIENT_ID}",
  "AuthParameters": {
    "USERNAME": "${USERNAME}",
    "PREFERRED_CHALLENGE": "PASSWORD",
    "SECRET_HASH": "${SECRET_HASH}"
  }
}
JSON
)")"

SESSION="$(printf '%s' "$INIT" | json_get Session)"
if [[ -z "$SESSION" ]]; then
    echo "Cognito no devolvio Session en InitiateAuth:" >&2
    printf '%s\n' "$INIT" >&2
    exit 1
fi

RESP="$(call_cognito RespondToAuthChallenge "$(cat <<JSON
{
  "ChallengeName": "PASSWORD",
  "ClientId": "${CLIENT_ID}",
  "Session": "${SESSION}",
  "ChallengeResponses": {
    "USERNAME": "${USERNAME}",
    "PASSWORD": "${COGNITO_PASSWORD}",
    "SECRET_HASH": "${SECRET_HASH}"
  }
}
JSON
)")"

TOKEN="$(printf '%s' "$RESP" | json_get AuthenticationResult.AccessToken)"
if [[ -z "$TOKEN" ]]; then
    echo "Cognito no devolvio access_token:" >&2
    printf '%s\n' "$RESP" >&2
    exit 1
fi

printf '%s\n' "$TOKEN"
