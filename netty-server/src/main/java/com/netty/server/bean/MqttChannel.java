package com.netty.server.bean;

import io.netty.channel.ChannelHandlerContext;

public class MqttChannel {

    private String deviceId;

    private ChannelHandlerContext ctx;

    private MqttWill mqttWill;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public MqttWill getMqttWill() {
        return mqttWill;
    }

    public void setMqttWill(MqttWill mqttWill) {
        this.mqttWill = mqttWill;
    }
}
