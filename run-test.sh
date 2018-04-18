#!/bin/bash

script_dir=$(dirname "$0")
#export JAVA_OPTS="-XX:+PreserveFramePointer -XX:InlineSmallCode=500"

function netty_test() {
    jar_name="$1"
    sleep_time="$2"
    port="$3"
    metrics_port="$4"
    cd ${jar_name}/target
    if pgrep -f "${jar_name}" > /dev/null; then
        echo "Shutting down ${jar_name}"
        pkill -f ${jar_name}
    fi

    echo "Starting ${jar_name}"
    nohup java ${JAVA_OPTS} -Xms1g -Xmx1g -jar ${jar_name}.jar --sleep-time ${sleep_time} --random-sleep --random-payload | tee netty.out 2>&1 &
    cd ../../
    jmeter -n -t loadtest.jmx -Jduration=10 -Jhost=localhost -Jport=${port} -l ${jar_name}.jtl
    curl http://localhost:${metrics_port}/metrics | tee ${sleep_time}_${jar_name}.txt
}

sleep_times=(10 500 1000 2000)

for sleep_time in "${sleep_times[@]}"; do
    netty_test netty-dropwizard-metrics $sleep_time 8688 9797
    netty_test netty-micrometer-metrics $sleep_time 8689 9798
    netty_test netty-prometheus-metrics $sleep_time 8690 9799
done
