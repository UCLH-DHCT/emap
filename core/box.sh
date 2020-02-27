set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
BOX_CONFIG_FILE="$(dirname "$SCRIPT_DIR")/box-config-envs"
set -a # auto-export all vars so docker-compose can see them for opening ports etc.
source "$BOX_CONFIG_FILE"
set +a
echo "Emap in a box config file: $BOX_CONFIG_FILE"
set -x
docker-compose \
    -f "$SCRIPT_DIR/docker-compose.yml" \
    -f "$SCRIPT_DIR/docker-compose.fakeuds.yml" \
    -f "$SCRIPT_DIR/../DatabaseFiller/docker-compose.yml" \
    -p $BOX_PROJECT_NAME \
    "$@"

