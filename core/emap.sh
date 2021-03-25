set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
EMAP_CONFIG_FILE="$(dirname "$SCRIPT_DIR")/config/global-config-envs"
set -a # auto-export all vars so docker-compose can see them for opening ports etc.
source "$EMAP_CONFIG_FILE"
set +a
>&2 echo "Emap in a box config file: $EMAP_CONFIG_FILE"

# parse direct options to this script (before the docker-compose subcommand)
FAKE_CLABOODLE=""
while (( "$#" )); do
    case "$1" in
        --fake-claboodle)
            FAKE_CLABOODLE=1
            shift
            ;;
        -*|--*=) # unrecognised flags
            echo "Error: Unrecognised flag $1" >&2
            exit 1
            ;;
        *)
            # bareword - hopefully we've reached a docker-compose subcommand now so stop processing
            break
            ;;
    esac
done


if [ -n "$FAKE_CLABOODLE" ]; then
    CLABOODLE_ARG="-f "$(dirname "$SCRIPT_DIR")"/hoover/docker-compose.fake_services.yml"
else
    CLABOODLE_ARG=""
fi


# If invoked as emap-live.sh then disable the fake UDS and dbfiller
if [ "$( basename "$0" )" = "emap-live.sh" ]; then
    FAKEUDS_ARG=""
    DBFILLER_ARG=""
else
    FAKEUDS_ARG="-f $SCRIPT_DIR/docker-compose.fakeuds.yml"
    DBFILLER_ARG="-f "$(dirname "$SCRIPT_DIR")"/DatabaseFiller/docker-compose.yml"
fi
set -x
# Order of docker-compose files can matter. Eg. env_file location is relative to
# the directory of the *first* -f value.
docker-compose \
    -f "$SCRIPT_DIR/docker-compose.yml" \
    $FAKEUDS_ARG \
    $DBFILLER_ARG \
    -f "$(dirname "$SCRIPT_DIR")"/emap-hl7-processor/docker-compose.yml \
    -f "$(dirname "$SCRIPT_DIR")"/hoover/docker-compose.yml \
    $CLABOODLE_ARG \
    -p $EMAP_PROJECT_NAME \
    "$@"

