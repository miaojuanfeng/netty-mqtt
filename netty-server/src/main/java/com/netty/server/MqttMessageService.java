package com.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.springframework.stereotype.Service;

public class MqttMessageService {

    public static void replyConnectMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, mqttMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttMessage.fixedHeader().isRetain(), 0x02);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
        MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
        writeAndFlush(ctx, connAck);
    }

    public static void replyPingReqMessage(ChannelHandlerContext ctx){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pingResp = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, pingResp);
    }

    public static void replyPublishMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
//        ByteBuf byteBuffer = mqttPublishMessage.payload();
//        byte[] bytes = new byte[byteBuffer.readableBytes()];
//        byteBuffer.readBytes(bytes);
//        String content = new String(bytes);
//        System.out.println("publish name: "+mqttPublishMessage.variableHeader().topicName());
//        System.out.println("publish content: "+content);
        MqttQoS qosLevel = mqttPublishMessage.fixedHeader().qosLevel();
        switch (qosLevel){
            case AT_MOST_ONCE:
                break;
            case AT_LEAST_ONCE:
                MqttFixedHeader mqttFixedHeader2 = new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,false,0x02);
                int messageId = mqttPublishMessage.variableHeader().messageId();
                MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
                MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(mqttFixedHeader2,from);
                ctx.writeAndFlush(mqttPubAckMessage).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("回写成功");
                        } else {
                            System.out.println("回写失败");
                        }
                    }
                });
                break;
            case EXACTLY_ONCE:
                MqttFixedHeader mqttFixedHeader3 = new MqttFixedHeader(MqttMessageType.PUBREC,false, MqttQoS.AT_LEAST_ONCE,false,0x02);
                int messageId2 = mqttPublishMessage.variableHeader().messageId();
                MqttMessageIdVariableHeader from2 = MqttMessageIdVariableHeader.from(messageId2);
                MqttPubAckMessage mqttPubAckMessage2 = new MqttPubAckMessage(mqttFixedHeader3,from2);
                ctx.writeAndFlush(mqttPubAckMessage2).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("回写成功");
                        } else {
                            System.out.println("回写失败");
                        }
                    }
                });
                break;
        }
    }

    private static void writeAndFlush(ChannelHandlerContext ctx, Object o){
        ctx.writeAndFlush(o).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("回写成功");
                } else {
                    System.out.println("回写失败");
                }
            }
        });
    }
}
