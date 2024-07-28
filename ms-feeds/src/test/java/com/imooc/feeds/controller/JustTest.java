package com.imooc.feeds.controller;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class JustTest implements Runnable {

    private static JustTest instance = new JustTest();

    public static synchronized void method1() {
        System.out.println(Thread.currentThread().getName() + "m1开始执行");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().getName() + "m1执行完毕");

    }

    public static synchronized void method2() {
        System.out.println(Thread.currentThread().getName() + "m2开始执行");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().getName() + "m2执行完毕");
    }

    public static void main(String[] args) throws InterruptedException {
        Thread m1 = new Thread(instance);
        Thread m2 = new Thread(instance);
        m1.start();
        m1.join();
        m2.start();
        m2.join();
    }

    @Override
    public void run() {
        method1();
        method2();
    }
}
