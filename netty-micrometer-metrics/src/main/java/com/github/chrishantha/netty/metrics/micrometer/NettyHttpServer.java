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
package com.github.chrishantha.netty.metrics.micrometer;

import com.github.chrishantha.netty.metrics.base.AbstractNettyHttpServer;
import com.github.chrishantha.netty.metrics.base.args.ServerArgs;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;

public class NettyHttpServer extends AbstractNettyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);

    private static final short OFFSET = 1;

    private static final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private Counter totalRequestCounter;
    private Timer requestLatencyTimer;
    private Timer sleepTimer;
    private DistributionSummary requestSizeSummary;
    private DistributionSummary responseSizeSummary;

    @Override
    public void init(ServerArgs serverArgs) {
        serverArgs.setPort(serverArgs.getPort() + OFFSET);
        serverArgs.setMetricsPort(serverArgs.getMetricsPort() + OFFSET);

        totalRequestCounter = registry.counter("requests_total");
        requestLatencyTimer = registry.timer("requests_latency");
        sleepTimer = registry.timer("sleep_time");
        requestSizeSummary = DistributionSummary
                .builder("request_size")
                .publishPercentileHistogram()
                .baseUnit("bytes")
                .register(registry);
        responseSizeSummary = DistributionSummary
                .builder("response_size")
                .baseUnit("bytes")
                .register(registry);

        //expose prometheus metrics
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(serverArgs.getMetricsPort()), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/metrics", httpExchange -> {
            logger.info("Request received for /metrics");
            String response = registry.scrape();
            httpExchange.getResponseHeaders().put("Content-Type", Collections.singletonList("text/plain; charset=UTF-8"));
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        server.start();
    }

    public Counter getTotalRequestCounter() {
        return totalRequestCounter;
    }

    public Timer getRequestLatencyTimer() {
        return requestLatencyTimer;
    }

    public Timer getSleepTimer() {
        return sleepTimer;
    }

    public DistributionSummary getRequestSizeSummary() {
        return requestSizeSummary;
    }

    public DistributionSummary getResponseSizeSummary() {
        return responseSizeSummary;
    }
}
