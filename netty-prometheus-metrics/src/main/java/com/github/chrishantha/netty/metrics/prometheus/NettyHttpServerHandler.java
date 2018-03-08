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

import com.github.chrishantha.netty.metrics.base.AbstractNettyHttpServerHandler;
import com.github.chrishantha.netty.metrics.base.args.HandlerArgs;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;

public class NettyHttpServerHandler extends AbstractNettyHttpServerHandler<NettyHttpServer> {

    private NettyHttpServer httpServer;

    @Override
    public void init(HandlerArgs handlerArgs, NettyHttpServer httpServer) {
        setHandlerArgs(handlerArgs);
        this.httpServer = httpServer;
    }

    @Override
    protected Object requestStart() {
        httpServer.getInprogressRequestsGauge().inc();
        httpServer.getTotalRequestCounter().inc();
        return new Object[]{httpServer.getRequestLatencyHistogram().startTimer(), httpServer.getRequestLatencySummary().startTimer()};
    }

    @Override
    protected void requestEnd(Object object) {
        httpServer.getInprogressRequestsGauge().dec();
        Object[] objects = (Object[]) object;
        ((Histogram.Timer) objects[0]).observeDuration();
        ((Summary.Timer) objects[1]).observeDuration();
    }

    @Override
    protected Object sleepStart() {
        return httpServer.getSleepTimeSummary().startTimer();
    }

    @Override
    protected void sleepEnd(Object object) {
        ((Summary.Timer) object).observeDuration();
    }

    @Override
    protected void requestSize(int size) {
        httpServer.getRequestSizeHistogram().observe(size);
    }

    @Override
    protected void responseSize(int size) {
        httpServer.getResponseSizeHistogram().observe(size);
    }
}
