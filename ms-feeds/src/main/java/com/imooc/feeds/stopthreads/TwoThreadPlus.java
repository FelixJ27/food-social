package com.imooc.feeds.stopthreads;

import java.util.concurrent.atomic.AtomicInteger;

public class TwoThreadPlus implements Runnable {

    static AtomicInteger count = new AtomicInteger(0);
    static TwoThreadPlus twoThreadPlus = new TwoThreadPlus();

    @Override
    public void run() {
//        synchronized (twoThreadPlus) {
            for (int i = 0; i < 10000; i++) {
                count.incrementAndGet();
            }
//        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(twoThreadPlus);
        Thread thread2 = new Thread(twoThreadPlus);
        thread.start();
        thread2.start();
        thread.join();
        thread2.join();
        System.out.println(count);
    }
}
