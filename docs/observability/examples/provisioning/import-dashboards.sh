#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRAFANA_DIR="${SCRIPT_DIR}/../grafana"

if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  source "${SCRIPT_DIR}/.env"
fi

: "${GRAFANA_URL:?GRAFANA_URL is required}"
: "${GRAFANA_API_TOKEN:?GRAFANA_API_TOKEN is required}"

HTTP_TIMEOUT="${HTTP_TIMEOUT:-20}"
GRAFANA_ORG_ID="${GRAFANA_ORG_ID:-1}"
DS_PROMETHEUS="${DS_PROMETHEUS:-prometheus}"
DS_LOKI="${DS_LOKI:-loki}"
DS_TEMPO="${DS_TEMPO:-tempo}"

auth_header="Authorization: Bearer ${GRAFANA_API_TOKEN}"
org_header="X-Grafana-Org-Id: ${GRAFANA_ORG_ID}"
json_header="Content-Type: application/json"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

require_cmd curl
require_cmd jq

api_get() {
  local path="$1"
  curl -sS --max-time "${HTTP_TIMEOUT}" \
    -H "${auth_header}" -H "${org_header}" \
    "${GRAFANA_URL}${path}"
}

api_post() {
  local path="$1"
  local body="$2"
  curl -sS --max-time "${HTTP_TIMEOUT}" \
    -H "${auth_header}" -H "${org_header}" -H "${json_header}" \
    -X POST "${GRAFANA_URL}${path}" \
    -d "${body}"
}

get_or_create_folder_id() {
  local folder_title="$1"
  local folder_resp folder_id create_body create_resp

  folder_resp="$(api_get "/api/folders")"
  folder_id="$(echo "${folder_resp}" | jq -r --arg t "${folder_title}" '.[] | select(.title==$t) | .id' | head -n 1)"

  if [[ -n "${folder_id}" && "${folder_id}" != "null" ]]; then
    echo "${folder_id}"
    return 0
  fi

  create_body="$(jq -n --arg title "${folder_title}" '{title: $title}')"
  create_resp="$(api_post "/api/folders" "${create_body}")"
  echo "${create_resp}" | jq -r '.id'
}

render_dashboard() {
  local input_file="$1"
  local output_file="$2"

  sed \
    -e "s|\${DS_PROMETHEUS}|${DS_PROMETHEUS}|g" \
    -e "s|\${DS_LOKI}|${DS_LOKI}|g" \
    -e "s|\${DS_TEMPO}|${DS_TEMPO}|g" \
    "${input_file}" > "${output_file}"
}

import_dashboard() {
  local file="$1"
  local folder="$2"
  local folder_id rendered payload resp status

  folder_id="$(get_or_create_folder_id "${folder}")"
  rendered="$(mktemp)"
  render_dashboard "${GRAFANA_DIR}/${file}" "${rendered}"

  payload="$(jq -n \
    --argjson dashboard "$(cat "${rendered}")" \
    --argjson folderId "${folder_id}" \
    '{dashboard:$dashboard, folderId:$folderId, overwrite:true}')"

  resp="$(api_post "/api/dashboards/db" "${payload}")"
  status="$(echo "${resp}" | jq -r '.status // "unknown"')"
  echo "Imported ${file} -> folder=${folder} status=${status}"

  rm -f "${rendered}"
}

declare -a ENTRIES=(
  "l1-platform-global.json|00-Global"
  "l2-service-catalog.json|02-Service-Catalog"
  "l3-service-health-template.json|03-Services"
  "l3-api-gateway.json|01-Edge"
  "l3-k8s-workload.json|04-Kubernetes"
  "l3-k8s-pod-runtime.json|04-Kubernetes"
  "l3-k8s-node-infra.json|04-Kubernetes"
  "l3-jvm-runtime.json|03-Services"
  "l3-database.json|05-Data-Platforms"
  "l3-cache.json|05-Data-Platforms"
  "l3-messaging.json|05-Data-Platforms"
  "l3-logs-exceptions.json|03-Services"
  "l3-tracing-dependencies.json|03-Services"
  "l2-slo-error-budget.json|00-Global"
  "l2-release-impact.json|00-Global"
  "l3-auth-iam-security.json|01-Edge"
  "l2-business-journey.json|06-Business"
)

for entry in "${ENTRIES[@]}"; do
  file="${entry%%|*}"
  folder="${entry##*|}"
  import_dashboard "${file}" "${folder}"
done

echo "Dashboard import complete."
