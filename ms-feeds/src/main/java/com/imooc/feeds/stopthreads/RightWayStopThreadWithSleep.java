package com.imooc.feeds.stopthreads;

public class RightWayStopThreadWithSleep implements Runnable {
    @Override
    public void run() {
        int num = 0;
        try {
            while (num <= 300
                    && !Thread.currentThread().isInterrupted()) {
                if (num % 100 == 0) {
                    System.out.println(num + "是100的倍数");
                }
                num++;
            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new RightWayStopThreadWithSleep());
        thread.start();
        Thread.sleep(500);
        thread.interrupt();
    }
}
