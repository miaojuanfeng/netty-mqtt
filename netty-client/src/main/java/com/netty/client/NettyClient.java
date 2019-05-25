package com.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NettyClient {

    private static final String address = "localhost";

    private static final int port = 8088;

    private static int retryTimes = 0;

    private static final int retryLimit = 5;

    private static final int sleepTimeout = 2000;

    private static final Bootstrap bootstrap = new Bootstrap();

    private static final EventLoopGroup group = new NioEventLoopGroup();

    public static void main(String[] args){


//        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new NettyClientInitializer());

//            Channel channel = bootstrap.connect("", 8088).sync().channel();
            connect();

//            // 控制台输入
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            for (;;) {
//                String line = in.readLine();
//                if (line == null) {
//                    continue;
//                }
//                channel.writeAndFlush(line);
//            }
//        } catch (InterruptedException e) {
//            group.shutdownGracefully();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static void connect() {
        ++retryTimes;
        System.out.println(retryTimes);
        ChannelFuture channelFuture = bootstrap.connect(address, port);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if( !channelFuture.isSuccess() ){
                    if( retryTimes < retryLimit ) {
                        Thread.sleep(sleepTimeout);
                        connect();
                    }else{
                        group.shutdownGracefully();
                        System.out.println("【ClientHandler：channelActive】无法创建与 " + address + ":" + port +" 的TCP/IP链接");
                    }
                }
            }
        });
    }
}
