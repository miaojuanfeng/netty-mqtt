package com.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;


@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        System.out.println("Client said:" + mqttMessage);
        switch (mqttMessage.fixedHeader().messageType()){
            case CONNECT:
                MqttMessageService.replyConnectMessage(ctx, mqttMessage);
                break;
            case PINGREQ:
                MqttMessageService.replyPingReqMessage(ctx);
                break;
            case PUBLISH:
                MqttPublishMessage msg = (MqttPublishMessage)mqttMessage;
                ByteBuf byteBuffer = msg.payload();
                byte[] bytes = new byte[byteBuffer.readableBytes()];
                byteBuffer.readBytes(bytes);
                String content = new String(bytes, "utf-8");
                System.out.println("publish name: "+msg.variableHeader().topicName());
                System.out.println("publish content: "+content);
                switch (msg.fixedHeader().qosLevel()){
                    case AT_MOST_ONCE:
                        break;
                    case AT_LEAST_ONCE:
                        MqttFixedHeader mqttFixedHeader2 = new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,false,0x02);
                        int messageId = msg.variableHeader().messageId();
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
                        int messageId2 = msg.variableHeader().messageId();
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
                break;
            case PUBREL:
                MqttMessageIdVariableHeader msg2 = (MqttMessageIdVariableHeader)mqttMessage.variableHeader();
                MqttFixedHeader mqttFixedHeader4 = new MqttFixedHeader(MqttMessageType.PUBCOMP,false, MqttQoS.AT_MOST_ONCE,false,0x02);
                int messageId4 = msg2.messageId();
                MqttMessageIdVariableHeader from4 = MqttMessageIdVariableHeader.from(messageId4);
                MqttPubAckMessage mqttPubAckMessage4 = new MqttPubAckMessage(mqttFixedHeader4,from4);
                ctx.writeAndFlush(mqttPubAckMessage4).addListener(new ChannelFutureListener() {
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
//        ByteBuf result = (ByteBuf) msg;
//        byte[] result1 = new byte[result.readableBytes()];
//        result.readBytes(result1);
//        String resultStr = new String(result1);
//        System.out.println("Client said:" + resultStr);
//
//        String x = "接收到客户端消息：" + resultStr;
//        ByteBuf firstMessage = Unpooled.buffer();
//        firstMessage.writeBytes(x.getBytes());
//
//        ctx.writeAndFlush("OkOK").addListener(new ChannelFutureListener() {
//                    @Override
//                    public void operationComplete(ChannelFuture future) throws Exception {
//                        if (future.isSuccess()) {
//                            System.out.println("回写成功");
//                        } else {
//                            System.out.println("回写失败");
//                        }
//                    }
//                });
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel注册");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel注册");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel活跃状态");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端与服务端断开连接之后");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel读取数据完毕");
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("用户事件触发");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel可写事件更改");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    //channel发生异常，若不关闭，随着异常channel的逐渐增多，性能也就随之下降
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("捕获channel异常");
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("助手类添加");
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("助手类移除");
        super.handlerRemoved(ctx);
    }
}
