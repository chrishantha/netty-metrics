/*
 * Copyright 2018 M. Isuru Tharanga Chrishantha Perera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.chrishantha.netty.metrics.prometheus;

import com.github.chrishantha.netty.metrics.base.AbstractNettyHttpServer;
import com.github.chrishantha.netty.metrics.base.args.ServerArgs;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyHttpServer extends AbstractNettyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);

    private static final short OFFSET = 2;

    private Counter totalRequestCounter;
    private Gauge inprogressRequestsGauge;
    private Histogram requestLatencyHistogram;
    private Summary requestLatencySummary;
    private Summary sleepTimeSummary;
    private Summary requestSizeSummary;
    private Summary responseSizeSummary;

    @Override
    public void init(ServerArgs serverArgs) {
        serverArgs.setPort(serverArgs.getPort() + OFFSET);
        serverArgs.setMetricsPort(serverArgs.getMetricsPort() + OFFSET);

        totalRequestCounter = Counter.build()
                .name("requests_total").help("Requests total").register();
        inprogressRequestsGauge = Gauge.build()
                .name("inprogress_requests").help("Inprogress Requests").register();
        requestLatencyHistogram = Histogram.build()
                .labelNames("method")
                .name("requests_latency_seconds").help("Request latency in seconds.").register();
        requestLatencySummary = Summary.build()
                .quantile(0.1, 0.05)
                .quantile(0.5, 0.05)
                .quantile(0.9, 0.01)
                .quantile(0.99, 0.001)
                .name("requests_latency").help("Request latency").register();
        sleepTimeSummary = Summary.build()
                .name("sleep_time").help("Sleep time").register();
        requestSizeSummary = Summary.build()
                .quantile(0.1, 0.05)
                .quantile(0.5, 0.05)
                .quantile(0.9, 0.01)
                .quantile(0.99, 0.001)
                .name("request_size").help("Request size").register();
        responseSizeSummary = Summary.build()
                .quantile(0.1, 0.05)
                .quantile(0.5, 0.05)
                .quantile(0.9, 0.01)
                .quantile(0.99, 0.001)
                .name("response_size").help("Response size").register();

        try {
            new HTTPServer(serverArgs.getMetricsPort(), true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Counter getTotalRequestCounter() {
        return totalRequestCounter;
    }

    public Gauge getInprogressRequestsGauge() {
        return inprogressRequestsGauge;
    }

    public Histogram getRequestLatencyHistogram() {
        return requestLatencyHistogram;
    }

    public Summary getRequestLatencySummary() {
        return requestLatencySummary;
    }

    public Summary getSleepTimeSummary() {
        return sleepTimeSummary;
    }

    public Summary getRequestSizeSummary() {
        return requestSizeSummary;
    }

    public Summary getResponseSizeSummary() {
        return responseSizeSummary;
    }
}
