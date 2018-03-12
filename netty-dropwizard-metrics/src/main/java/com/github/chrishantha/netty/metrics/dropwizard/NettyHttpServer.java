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
package com.github.chrishantha.netty.metrics.dropwizard;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.chrishantha.netty.metrics.base.AbstractNettyHttpServer;
import com.github.chrishantha.netty.metrics.base.args.ServerArgs;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collections;

public class NettyHttpServer extends AbstractNettyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);

    private static final short OFFSET = 0;

    private static final MetricRegistry registry = new MetricRegistry();

    private Counter totalRequestCounter;
    private Counter inprogressRequestsCounter;
    private Meter successRate;
    private Timer requestLatencyTimer;
    private Timer sleepTimer;
    private Histogram requestSizeHistogram;
    private Histogram responseSizeHistogram;

    @Override
    public void init(ServerArgs serverArgs) {
        serverArgs.setPort(serverArgs.getPort() + OFFSET);
        serverArgs.setMetricsPort(serverArgs.getMetricsPort() + OFFSET);

        new DropwizardExports(registry).register(CollectorRegistry.defaultRegistry);

        totalRequestCounter = registry.counter("requests_total");
        inprogressRequestsCounter = registry.counter("inprogress_requests");
        successRate = registry.meter("success_rate");
        requestLatencyTimer = registry.timer("requests_latency");
        sleepTimer = registry.timer("sleep_time");
        requestSizeHistogram = registry.histogram("request_size");
        responseSizeHistogram = registry.histogram("response_size");

        //TODO: JVM Gauges?

        try {
            new HTTPServer(serverArgs.getMetricsPort(), true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Counter getTotalRequestCounter() {
        return totalRequestCounter;
    }

    public Counter getInprogressRequestsCounter() {
        return inprogressRequestsCounter;
    }

    public Meter getSuccessRate() {
        return successRate;
    }

    public Timer getRequestLatencyTimer() {
        return requestLatencyTimer;
    }

    public Timer getSleepTimer() {
        return sleepTimer;
    }

    public Histogram getRequestSizeHistogram() {
        return requestSizeHistogram;
    }

    public Histogram getResponseSizeHistogram() {
        return responseSizeHistogram;
    }
}
