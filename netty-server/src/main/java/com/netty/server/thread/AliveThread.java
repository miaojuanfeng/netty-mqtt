package com.netty.server.thread;

public class AliveThread extends Thread{

    private byte[] lock = new byte[0];

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
                    System.out.println("Alive check");
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
