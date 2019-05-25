package com.netty.server.util;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageId {

    private static AtomicInteger index = new AtomicInteger(1);
    /**
     * 获取messageId
     * @return id
     */
    public static int messageId(){
        for (;;) {
            int current = index.get();
            int next = (current >= Integer.MAX_VALUE ? 0: current + 1);
            if (index.compareAndSet(current, next)) {
                return current;
            }
        }
    }
}