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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class NettyHttpServer extends AbstractNettyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);

    private static final short OFFSET = 1;

    private Counter totalRequestCounter;
    private Timer requestLatencyTimer;
    private Timer sleepTimer;
    private DistributionSummary requestSizeSummary;
    private DistributionSummary responseSizeSummary;

    @Override
    public void init(ServerArgs serverArgs) {
        serverArgs.setPort(serverArgs.getPort() + OFFSET);
        serverArgs.setMetricsPort(serverArgs.getMetricsPort() + OFFSET);

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.globalRegistry.add(registry);
        totalRequestCounter = registry.counter("requests_total");
        Counter.builder("requests_total").register(registry);
        requestLatencyTimer = Timer.builder("requests_latency").publishPercentiles(0.5, 0.75, 0.98, 0.99, 0.999)
                .register(registry);
        sleepTimer = Timer.builder("sleep_time").publishPercentiles(0.5, 0.75, 0.98, 0.99, 0.999)
                .register(registry);
        requestSizeSummary = DistributionSummary
                .builder("request_size")
                .tags()
                .register(registry);
        responseSizeSummary = DistributionSummary
                .builder("response_size")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.75, 0.98, 0.99, 0.999)
                .baseUnit("bytes")
                .register(registry);

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new FileDescriptorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        try {
            new HTTPServer(new InetSocketAddress(serverArgs.getMetricsPort()), registry.getPrometheusRegistry(), true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
