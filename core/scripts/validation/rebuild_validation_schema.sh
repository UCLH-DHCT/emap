#!/bin/bash
set -euo pipefail

# Steps that must be performed manually for now:
# - Check out the right commits and (re)build your docker images
# - Truncate star_validation tables including progress
# - Truncate ops_validation tables, including progress but not the mapping tables
# - Truncate hl7 and caboodle progress tables in caboodle_extract_validation schema

date_suffix=$(date +%s)
log_file_prefix=rebuild_log_${date_suffix}

# edit hl7 (vitals+adt) config files in place, keeping a backup named by the timestamp
configure_time_window() {
    window_start_human="$1" # eg. "4 days ago", "2020-04-12", must be >= 1 day ago I think
    window_end_human="$2" # same thing but for end, eg. "today", "2020-04-19"
    # run up to most recent midnight, from N days previous
    export window_start="$(date --iso-8601=date --date="$window_start_human")T00:00:00.000Z"
    export window_end="$(date --iso-8601=date --date="$window_end_human")T00:00:00.000Z"
    echo "Setting config for pipeline to run from $window_start to $window_end"
    perl -pi.$(date --iso-8601=seconds) \
        -e 's/^(IDS_CFG_DEFAULT_START_DATETIME)=.*$/$1=$ENV{"window_start"}/;
            s/^(IDS_CFG_END_DATETIME)=.*$/$1=$ENV{"window_end"}/;
            s/^(CABOODLE_DATE_FROM)=.*$/$1=$ENV{"window_start"}/;
            s/^(CABOODLE_DATE_UNTIL)=.*$/$1=$ENV{"window_end"}/;
        ' \
        ../config/emap-hl7processor-config-envs \
        ../config/emap-core-config-envs \
        ../config/caboodle-envs
}

stop_it_and_tidy_up() {
    bash emap-live.sh ps
    echo "Capturing remaining logs..."
    # capture the logs for services that were run backgrounded
    for cont in emapstar rabbitmq ; do
        bash emap-live.sh logs $cont > ${log_file_prefix}_${cont}.txt
    done
    echo "Shutting down containers..."
    bash emap-live.sh down
    echo "Done tidying up"
    bash emap-live.sh ps
}

# run everything except OMOP
run_pipeline() {
    bash emap-live.sh ps
    bash emap-live.sh down
    bash emap-live.sh ps
    bash emap-live.sh up -d cassandra glowroot-central
    source ../config/glowroot-config-envs && ./emap-live.sh run glowroot-central java -jar "glowroot-central.jar" setup-admin-user "${GLOWROOT_USERNAME}" "${GLOWROOT_PASSWORD}"
    bash emap-live.sh up -d rabbitmq
    bash emap-live.sh ps
    # If this is run after the data sources, it would deadlock if the hl7source generates
    # more messages than can fit in the queue, but currently emapstar doesn't like being started up
    # unless the queues exist, or start existing very quickly.
    # So, start it up just a little after the datasources!
    (sleep 180; bash emap-live.sh up -d emapstar; bash emap-live.sh ps) &

    # wait for each hl7 source to finish filling up the queue
    bash emap-live.sh up --exit-code-from hl7source hl7source > ${log_file_prefix}_hl7source.txt
    bash emap-live.sh ps
    bash emap-live.sh up --exit-code-from caboodle caboodle > ${log_file_prefix}_caboodle.txt
    bash emap-live.sh ps
}


# wait for rabbitmq queue to empty
# If it's still going after 10 hours something's gone very wrong and we should give up
wait_for_queue_to_empty() {
    timeout_secs=36000
    start_time=$(date +%s)
    while true; do
        echo "Checking emptiness of rabbitmq queues"
        # This script gets stopped if backgrounded by the shell and docker-compose exec is used here.
        # Not sure why, but calling docker exec directly works fine, so get the container ID and do that.
        rabbitmq_container_id=$(bash emap-live.sh ps -q rabbitmq)
        # returns empty string if all queues we care about are at 0 messages
        non_empty_queues=$(docker exec $rabbitmq_container_id rabbitmqctl -q list_queues \
            | awk -v RS='\r\n' 'BEGIN {OFS="\t"} {if (($1=="hl7Queue" || $1=="databaseExtracts") && $2!="0") {print $1 $2}  }' )
        if [ -z "$non_empty_queues" ]; then
            echo "Queues are empty, continuing"
            break
        elif expr \( $(date +%s) - $start_time \) \> $timeout_secs; then
            echo "Waiting for queue timed out"
            stop_it_and_tidy_up
            exit 2
        else
            echo "Queues still have contents, waiting"
            sleep 120
        fi
    done
}

run_omop() {
   bash emap-live.sh up --exit-code-from emapops emapops > ${log_file_prefix}_emapops.txt
}

# This script does not yet clear the databases before starting

window_start_arg="${1:-}"
if [ -z "$window_start_arg" ]; then
    window_start_arg="7 days ago"
fi
window_end_arg="${2:-}"
if [ -z "$window_end_arg" ]; then
    window_end_arg="today"
fi
configure_time_window "$window_start_arg" "$window_end_arg"
run_pipeline
wait_for_queue_to_empty
stop_it_and_tidy_up
