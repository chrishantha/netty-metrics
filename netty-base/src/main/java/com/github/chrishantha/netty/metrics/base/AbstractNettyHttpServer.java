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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.github.chrishantha.netty.metrics.base.args.HandlerArgs;
import com.github.chrishantha.netty.metrics.base.args.ServerArgs;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Abstract Netty HTTP Server
 */
public abstract class AbstractNettyHttpServer implements NettyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNettyHttpServer.class);

    public static void main(String[] args) throws Exception {
        // There should be only one server
        Iterator<NettyHttpServer> nettyHttpServerIterator = ServiceLoader.load(NettyHttpServer.class).iterator();
        if (!nettyHttpServerIterator.hasNext()) {
            throw new IllegalStateException("Could not load Netty HTTP Server");
        }
        NettyHttpServer nettyHttpServer = nettyHttpServerIterator.next();
        ServerArgs serverArgs = new ServerArgs();
        HandlerArgs handlerArgs = new HandlerArgs();
        final JCommander jcmdr = JCommander.newBuilder()
                .programName(nettyHttpServer.getClass().getSimpleName())
                .addObject(nettyHttpServer)
                .addObject(serverArgs)
                .addObject(handlerArgs)
                .build();

        try {
            jcmdr.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        if (serverArgs.isHelp()) {
            jcmdr.usage();
            return;
        }

        nettyHttpServer.init(serverArgs);
        nettyHttpServer.startServer(serverArgs, handlerArgs);
    }

    @Override
    public final void startServer(ServerArgs serverArgs, HandlerArgs handlerArgs) throws Exception {
        logger.info("Netty HTTP Server. Port: {}, Metrics Port: {}, Boss Threads: {}, Worker Threads: {}," +
                        " SSL Enabled: {}, Sleep Time: {}ms, Random Sleep: {}, Payload Size: {}B, Random Payload: {}",
                serverArgs.getPort(), serverArgs.getMetricsPort(), serverArgs.getBossThreads(),
                serverArgs.getWorkerThreads(), serverArgs.isEnableSSL(), handlerArgs.getSleepTime(),
                handlerArgs.isRandomSleep(), handlerArgs.getPayloadSize(), handlerArgs.isRandomPayload());
        // Print Max Heap Size
        logger.info("Max Heap Size: {}MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        // Print Netty Version
        Version version = Version.identify(this.getClass().getClassLoader()).values().iterator().next();
        logger.info("Netty Version: {}", version.artifactVersion());

        // Configure SSL.
        final SslContext sslCtx;
        if (serverArgs.isEnableSSL()) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(serverArgs.getBossThreads());
        EventLoopGroup workerGroup = new NioEventLoopGroup(serverArgs.getWorkerThreads());
        try {
            final NettyHttpServer nettyHttpServer = this;
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            Iterator<NettyHttpServerHandler> handlerIterator = ServiceLoader.load(NettyHttpServerHandler.class).iterator();
                            if (!handlerIterator.hasNext()) {
                                throw new IllegalStateException("Could not load Netty HTTP Server Handler");
                            }
                            @SuppressWarnings("unchecked")
                            NettyHttpServerHandler<NettyHttpServer> nettyHttpServerHandler = handlerIterator.next();
                            nettyHttpServerHandler.init(handlerArgs, nettyHttpServer);
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new HttpServerCodec());
                            p.addLast("aggregator", new HttpObjectAggregator(1048576));
                            p.addLast(nettyHttpServerHandler);
                        }
                    });

            // Start the server.
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(serverArgs.getPort()).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
