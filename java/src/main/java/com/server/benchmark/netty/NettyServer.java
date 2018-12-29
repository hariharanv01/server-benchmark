package com.server.benchmark.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import static io.netty.buffer.Unpooled.copiedBuffer;


public class NettyServer {

    private ChannelFuture channel;
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;

    private final int PORT;

    public NettyServer(int port) {
        masterGroup = new NioEventLoopGroup();
        slaveGroup = new NioEventLoopGroup();
        PORT = port;
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            final ServerBootstrap bootstrap =
                    new ServerBootstrap()
                            .group(masterGroup, slaveGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(final SocketChannel ch)
                                        throws Exception {
                                    ch.pipeline().addLast("codec", new HttpServerCodec());
                                    ch.pipeline().addLast("aggregator",
                                            new HttpObjectAggregator(512 * 1024));
                                    ch.pipeline().addLast("request",
                                            new ChannelInboundHandlerAdapter() {
                                                @Override
                                                public void channelRead(ChannelHandlerContext ctx, Object msg)
                                                        throws Exception {
                                                    if (msg instanceof FullHttpRequest) {
                                                        String res = new QueryStringDecoder(
                                                                ((FullHttpRequest) msg).uri()
                                                        ).path();
                                                        ByteBuf content = null;
                                                        FullHttpResponse response = null;
                                                        try {
                                                            content = copiedBuffer(res.getBytes());
                                                            response = new DefaultFullHttpResponse(
                                                                    HttpVersion.HTTP_1_1,
                                                                    HttpResponseStatus.OK,
                                                                    content
                                                            );

                                                            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                                                                    res.length());
                                                            ctx.write(response);
                                                        } finally {
                                                            if (content != null)
                                                                content.release();
                                                        }

                                                    } else {
                                                        super.channelRead(ctx, msg);
                                                    }

                                                }

                                                @Override
                                                public void channelReadComplete(ChannelHandlerContext ctx)
                                                        throws Exception {
                                                    ctx.flush();
                                                }

                                                @Override
                                                public void exceptionCaught(ChannelHandlerContext ctx,
                                                                            Throwable cause) throws Exception {
                                                    ctx.writeAndFlush(new DefaultFullHttpResponse(
                                                            HttpVersion.HTTP_1_1,
                                                            HttpResponseStatus.INTERNAL_SERVER_ERROR
                                                    ));
                                                }
                                            });
                                }
                            })
                            .option(ChannelOption.SO_BACKLOG, 1000)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
            channel = bootstrap.bind(PORT).sync();
            System.out.println("Netty server started");
        } catch (final InterruptedException e) {
        }
    }

    public void shutdown() {
        slaveGroup.shutdownGracefully();
        masterGroup.shutdownGracefully();

        try {
            channel.channel().closeFuture().sync();
            System.out.println("Netty server stopped");
        } catch (InterruptedException e) {
        }
    }

    public static void main(String[] args) {
        new NettyServer(8002).start();
    }
}
