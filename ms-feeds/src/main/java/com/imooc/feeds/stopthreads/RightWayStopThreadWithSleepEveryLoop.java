package com.imooc.feeds.stopthreads;

public class RightWayStopThreadWithSleepEveryLoop implements Runnable {
    @Override
    public void run() {
        int num = 0;
        try {
            while (num <= 10000) {
                if (num % 100 == 0) {
                    System.out.println(num + "是100的倍数");
                }
                num++;
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new RightWayStopThreadWithSleepEveryLoop());
        thread.start();
        Thread.sleep(5000);
        thread.interrupt();
    }
}
