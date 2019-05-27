package com.netty.server.thread;

import com.netty.server.MqttMessageService;

public class AliveThread extends Thread{

    private byte[] lock = new byte[0];

    private final long timeout = 5000;

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
//                    System.out.println("Alive check");
                    MqttMessageService.checkAlive();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
