set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
EMAP_CONFIG_FILE="$(dirname "$SCRIPT_DIR")/config/global-config-envs"
set -a # auto-export all vars so docker-compose can see them for opening ports etc.
source "$EMAP_CONFIG_FILE"
set +a
echo "Emap in a box config file: $EMAP_CONFIG_FILE"
set -x
# Order of docker-compose files can matter. Eg. env_file location is relative to
# the directory of the *first* -f value.
docker-compose \
    -f "$SCRIPT_DIR/docker-compose.yml" \
    -f "$SCRIPT_DIR/docker-compose.fakeuds.yml" \
    -f "$(dirname "$SCRIPT_DIR")"/DatabaseFiller/docker-compose.yml \
    -f "$(dirname "$SCRIPT_DIR")"/Omop-ETL/docker-compose.yml \
    -p $EMAP_PROJECT_NAME \
    "$@"

