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
package com.github.chrishantha.netty.metrics.base.args;

import com.beust.jcommander.Parameter;

public class HandlerArgs {

    @Parameter(names = "--random-sleep",
            description = "Sleep for a random time. Maximum value is specified using --sleep-time.", arity = 0)
    private boolean randomSleep = false;

    @Parameter(names = "--sleep-time", description = "Sleep Time in milliseconds")
    private int sleepTime = 0;

    @Parameter(names = "--random-payload",
            description = "Generate random payloads. Maximum value is specified using --payload-size.", arity = 0)
    private boolean randomPayload = false;

    @Parameter(names = "--payload-size", description = "Payload Size. Default 100KiB")
    private int payloadSize = 1024 * 100;

    @Parameter(names = "--random-status-code",
            description = "Return random HTTP status codes", arity = 0)
    private boolean randomStatusCode = false;

    public boolean isRandomSleep() {
        return randomSleep;
    }

    public void setRandomSleep(boolean randomSleep) {
        this.randomSleep = randomSleep;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public boolean isRandomPayload() {
        return randomPayload;
    }

    public void setRandomPayload(boolean randomPayload) {
        this.randomPayload = randomPayload;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public boolean isRandomStatusCode() {
        return randomStatusCode;
    }

    public void setRandomStatusCode(boolean randomStatusCode) {
        this.randomStatusCode = randomStatusCode;
    }
}
