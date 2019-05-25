package com.netty.server;

import com.netty.server.bean.MqttTopic;
import com.netty.server.util.MessageId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import io.netty.util.IllegalReferenceCountException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MqttMessageService {

    private static final AttributeKey<String> _deviceId = AttributeKey.valueOf("deviceId");

    private static ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Set<MqttTopic>> topics = new ConcurrentHashMap<>();

    public static void replyConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        Channel channel = ctx.channel();
        /**
         * Mqtt协议规定，在一个网络连接上，客户端只能发送一次CONNECT报文。服务端必须将客户端发送的第二个CONNECT报文当作协议违规处理并断开客户端的连接
         */
        String deviceId = mqttConnectMessage.payload().clientIdentifier();
        System.out.println(deviceId);
        Channel existChannel = channels.get(deviceId);
        if( existChannel != null ){
            System.out.println("exist channel");
            sendDisConnectMessage(ctx, mqttConnectMessage);
            channel.close();
            return;
        }
        channel.attr(_deviceId).set(deviceId);
        channels.put(deviceId, channel);


//        System.out.println(channel.attr(_deviceId).get());

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
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
                pushPublishTopic(ctx, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
                sendPubAckMessage(ctx, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
                sendPubRecMessage(ctx, mqttPublishMessage);
                break;
        }
    }

    public static void replyPubRelMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        sendPubCompMessage(ctx, mqttMessage);
    }

    public static void replyDisConnectMessage(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        String deviceId = channel.attr(_deviceId).get();
        channels.remove(deviceId);
    }

    public static void sendPubAckMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        int messageId = mqttPublishMessage.variableHeader().messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubAckMessage);
    }

    public static void sendPubRecMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC,false, MqttQoS.AT_LEAST_ONCE,false,0x02);
        int messageId = mqttPublishMessage.variableHeader().messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubRecMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubRecMessage);
    }

    public static void sendPubCompMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubCompMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubCompMessage);
    }

    public static void sendDisConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
        MqttMessage mqttMessage = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, mqttMessage);
    }

    public static void replySubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage mqttSubscribeMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, mqttSubscribeMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttSubscribeMessage.fixedHeader().isRetain(), 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(mqttSubscribeMessage.variableHeader().messageId());
        int num = mqttSubscribeMessage.payload().topicSubscriptions().size();
        List<Integer> grantedQoSLevels = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            grantedQoSLevels.add(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService().value());
            //
            String topicName = mqttSubscribeMessage.payload().topicSubscriptions().get(i).topicName();
            Set<MqttTopic> mqttTopics = topics.get(topicName);
            if( mqttTopics == null ){
                mqttTopics = new HashSet<>();
            }
            MqttTopic mqttTopic = new MqttTopic();
            mqttTopic.setTopicName(topicName);
            mqttTopic.setMqttQoS(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService());
            mqttTopic.setCtx(ctx);
            mqttTopics.add(mqttTopic);
            topics.put(topicName, mqttTopics);
        }
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(mqttFixedHeader, variableHeader, payload);
        writeAndFlush(ctx, mqttSubAckMessage);
    }

    private static void pushPublishTopic(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        String topicName = mqttPublishMessage.variableHeader().topicName();

        ByteBuf byteBuf = Unpooled.copiedBuffer(mqttPublishMessage.payload());
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        System.out.println(new String(bytes));

        Set<MqttTopic> mqttTopics = topics.get(topicName);
        for (MqttTopic mqttTopic : mqttTopics){
            if( mqttTopic.getCtx().channel().isActive() && mqttTopic.getCtx().channel().isWritable() ){
                sendTopicMessage(mqttTopic.getCtx(), topicName, bytes, MessageId.messageId(), mqttTopic.getMqttQoS());
            }
        }
    }

    private static void sendTopicMessage(ChannelHandlerContext ctx, String topicName, byte[] bytes, int messageId, MqttQoS mqttQoS){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,false, mqttQoS,false,0);
        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(topicName, messageId);
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader,mqttPublishVariableHeader,Unpooled.wrappedBuffer(bytes));
        writeAndFlush(ctx, mqttPublishMessage);
    }

    private static void writeAndFlush(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        ctx.writeAndFlush(mqttMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if (future.isSuccess()) {
                        System.out.println("回写成功：" + mqttMessage);
                    } else {
                        System.out.println("回写失败：" + mqttMessage);
                    }
                }catch (IllegalReferenceCountException e){
                    // Do nothing
                }

            }
        });
    }
}
