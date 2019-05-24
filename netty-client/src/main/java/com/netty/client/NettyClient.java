package com.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NettyClient {
    public static void main(String[] args){
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new NettyClientInitializer());

            Channel channel = bootstrap.connect("localhost", 8088).sync().channel();

//            // 控制台输入
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            for (;;) {
//                String line = in.readLine();
//                if (line == null) {
//                    continue;
//                }
//                channel.writeAndFlush(line);
//            }
        } catch (InterruptedException e) {
            group.shutdownGracefully();
//        } catch (IOException e) {
//            e.printStackTrace();
        }
    }
}
