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
package com.github.chrishantha.netty.metrics.base;

import com.github.chrishantha.netty.metrics.base.args.HandlerArgs;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

import java.nio.charset.Charset;
import java.util.Random;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Netty HTTP Server Handler implementing Netty ChannelInboundHandler
 */
public abstract class AbstractNettyHttpServerHandler<T extends NettyHttpServer>
        extends SimpleChannelInboundHandler<FullHttpRequest> implements NettyHttpServerHandler<T> {

    private static final Random random = new Random();

    private HandlerArgs handlerArgs;

    protected void setHandlerArgs(HandlerArgs handlerArgs) {
        this.handlerArgs = handlerArgs;
    }

    @Override
    public final void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    protected abstract Object requestStart(String method, String uri);

    protected abstract void requestEnd(String method, String uri, int statusCode, Object object);

    protected abstract Object sleepStart();

    protected abstract void sleepEnd(Object object);

    protected abstract void requestSize(int size);

    protected abstract void responseSize(int size);

    @Override
    protected final void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        Object requestStart = requestStart(msg.method().name(), msg.uri());
        HttpResponseStatus status = HttpResponseStatus.OK;
        try {
            requestSize(msg.content().readableBytes());
            if (handlerArgs.getSleepTime() > 0) {
                long sleepTime = handlerArgs.isRandomSleep() ? random.nextInt(handlerArgs.getSleepTime())
                        : handlerArgs.getSleepTime();
                Object sleepStart = sleepStart();
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // Ignore
                } finally {
                    sleepEnd(sleepStart);
                }
            }

            if (handlerArgs.isRandomStatusCode()) {
                int statusCode = 100 + random.nextInt(500);
                status = HttpResponseStatus.valueOf(statusCode);
                if (status == null) {
                    status = new HttpResponseStatus(statusCode, "Random Status Code");
                }
            }

            boolean keepAlive = HttpUtil.isKeepAlive(msg);

            HttpMethod method = msg.method();
            FullHttpResponse response;
            if (HttpMethod.GET.equals(method)) {
                response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.wrappedBuffer(generatePayload()));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, status, msg.content().copy());
                String contentType = msg.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if (contentType != null) {
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                }
            }
            responseSize(response.content().readableBytes());
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.write(response);
            }
        } finally {
            requestEnd(msg.method().name(), msg.uri(), status.code(), requestStart);
        }
    }

    private byte[] generatePayload() {
        int payloadSize = handlerArgs.isRandomPayload() ? random.nextInt(handlerArgs.getPayloadSize())
                : handlerArgs.getPayloadSize();
        StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append('{').append('"').append("size").append('"');
        payloadBuilder.append(':').append('"').append(payloadSize).append('B').append('"');
        payloadBuilder.append(',').append('"').append("payload").append('"');
        payloadBuilder.append(':').append('"');

        int limit = payloadSize - (payloadBuilder.toString().getBytes(Charset.forName("UTF-8")).length + 2);

        int c = '0';
        for (int i = 0; i < limit; i++) {
            payloadBuilder.append((char) c);
            if (c == '9') {
                c = 'A' - 1;
            } else if (c == 'Z') {
                c = 'a' - 1;
            } else if (c == 'z') {
                c = '0' - 1;
            }
            c++;
        }

        payloadBuilder.append('"').append('}');

        return payloadBuilder.toString().getBytes(Charset.forName("UTF-8"));
    }
}
