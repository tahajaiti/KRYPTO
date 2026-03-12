#!/usr/bin/env bash
set -euo pipefail

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# usage:
#   ./run_dev.sh                -> docker compose up --build -d
#   ./run_dev.sh <compose args> -> docker compose <compose args>
if [[ $# -eq 0 ]]; then
  docker compose up --build -d
else
  docker compose "$@"
fi
