package com.imooc.feeds.stopthreads;

public class RightWayStopThreadWithoutSleep implements Runnable {
    @Override
    public void run() {
        int num = 0;
        while (!Thread.currentThread().isInterrupted()
                && num <= Integer.MAX_VALUE / 2) {
            if (num % 10000 == 0)
                num++;
        }
        System.out.println("任务运行结束");
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new RightWayStopThreadWithoutSleep());
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
    }
}
