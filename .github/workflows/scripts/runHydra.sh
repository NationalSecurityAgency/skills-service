#!/bin/bash
# exit if a command returns non-zero exit code
set -e
set -o pipefail

DB=${DB:-postgres}
TRACING=${TRACING:-false}
PROMETHEUS=${PROMETHEUS:-false}
echo "PWD: "
echo $(pwd)
cd ./skills-client-integration/skills-int-e2e-test/hydra
DC="docker-compose -f quickstart.yml"
if [[ $DB == "mysql" ]]; then
    DC+=" -f quickstart-mysql.yml"
fi
if [[ $DB == "postgres" ]]; then
    DC+=" -f quickstart-postgres.yml"
fi
if [[ $TRACING == true ]]; then
    DC+=" -f quickstart-tracing.yml"
fi
if [[ $PROMETHEUS == true ]]; then
    DC+=" -f quickstart-prometheus.yml"
fi
DC+=" up --build -d"

$DC

declare -r HOST="localhost:4445/clients"

wait-for-url() {
  echo "Waiting for hydra service $1 ..."
  timeout -s TERM 45 bash -c \
  'while [[ "$(curl -X GET -s -o /dev/null -w ''%{http_code}'' ${0})" != "200" ]];\
  do echo "Waiting for ${0}" && sleep 2;\
  done' ${1}
  echo "OK!"
}
wait-for-url http://${HOST}
