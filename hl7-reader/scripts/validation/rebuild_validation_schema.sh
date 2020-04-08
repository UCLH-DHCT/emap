#!/bin/bash
set -euo pipefail

# Steps that must be performed manually for now:
# - Truncate star_validation tables including progress
# - Truncate ops_validation tables, including progress but not the mapping tables
# - Truncate hl7 and caboodle progress tables in caboodle_extract_validation schema

# edit hl7 (vitals+adt) config files in place, keeping a backup named by the timestamp
configure_time_window() {
    window_length_ago="$1" # eg. "4 days ago", must be >= 1 day I think
    # run up to most recent midnight, from N days previous
    export window_start="$(date --iso-8601=date --date="$window_length_ago")T00:00:00.000Z"
    export window_end="$(date --iso-8601=date)T00:00:00.000Z"
    echo Setting config for pipeline to run from $window_start to $window_end
    perl -pi.$(date --iso-8601=seconds) \
        -e 's/^(IDS_CFG_DEFAULT_START_DATETIME)=.*$/$1=$ENV{"window_start"}/;
            s/^(IDS_CFG_END_DATETIME)=.*$/$1=$ENV{"window_end"}/;
            s/^(CABOODLE_DATE_FROM)=.*$/$1=$ENV{"window_start"}/;
            s/^(CABOODLE_DATE_UNTIL)=.*$/$1=$ENV{"window_end"}/;
        ' \
        ../config/hl7-vitals-config-envs \
        ../config/emap-core-config-envs \
        ../config/caboodle-envs
}
 
stop_it_and_tidy_up() {
    bash emap-live.sh ps
    echo Tidying up...
    bash emap-live.sh down
    echo Done tidying up
    bash emap-live.sh ps
}

# run everything except OMOP
run_pipeline() {
    bash emap-live.sh ps
    bash emap-live.sh down
    bash emap-live.sh ps
    bash emap-live.sh up -d rabbitmq
    bash emap-live.sh ps
    # If this is run after the data sources, it would deadlock if the hl7source generates
    # more messages than can fit in the queue, but currently emapstar doesn't like being started up
    # unless the queues exist, or start existing very quickly.
    # So, start it up just a little after the datasources!
    (sleep 180; bash emap-live.sh up -d emapstar; bash emap-live.sh ps)
    
    # wait for each hl7 source to finish filling up the queue
    bash emap-live.sh up --exit-code-from hl7source hl7source
    bash emap-live.sh ps
    bash emap-live.sh up --exit-code-from hl7vitals hl7vitals
    bash emap-live.sh ps
    bash emap-live.sh up --exit-code-from caboodle caboodle
    bash emap-live.sh ps
}


# wait for rabbitmq queue to empty
# If it's still going after 10 hours something's gone very wrong and we should give up
wait_for_queue_to_empty() {
    timeout_secs=36000
    start_time=$(date +%s)
    while true; do
        echo "Checking emptiness of rabbitmq queues"
        # returns empty string if all queues we care about are at 0 messages
        non_empty_queues=$(bash emap-live.sh exec rabbitmq rabbitmqctl -q list_queues \
            | awk -v RS='\r\n' 'BEGIN {OFS="\t"} {if (($1=="hl7Queue" || $1=="caboodleQueue") && $2!="0") {print $1 $2}  }' )
        if [ -z "$non_empty_queues" ]; then
            echo Queues are empty, continuing
            break
        elif expr \( $(date +%s) - $start_time \) \> $timeout_secs; then
            echo "Waiting for queue timed out"
            stop_it_and_tidy_up
            exit 2
        else
            echo Queues still have contents, waiting
            sleep 20
        fi
    done
}

run_omop() {
   bash emap-live.sh up --exit-code-from emapops emapops
}

# This script does not yet clear the databases before starting


#configure_time_window '4 days ago'
configure_time_window '1 day ago'
run_pipeline
wait_for_queue_to_empty
run_omop


stop_it_and_tidy_up
