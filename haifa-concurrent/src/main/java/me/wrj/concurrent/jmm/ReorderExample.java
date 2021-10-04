package me.wrj.concurrent.jmm;

/**
 * Created by wangrenjun on 2018/1/14.
 */
public class ReorderExample {

    int a = 0;

    boolean flag = false;

    public void writer(){
        a = 1;  //操作1
        flag = true; //操作2
    }

    public void reader(){
        if(flag){  //操作3
            int i = a * a; // 操作4
        }
    }

    public  boolean conflict(){
        return  flag && (a == 0);
    }

    public static void main(String[] args) {
        ReorderExample  reorderExample = new ReorderExample();

        Thread writerThread = new Thread(new WriterThread(reorderExample));
        Thread readerThread = new Thread(new ReaderThread(reorderExample));


        while (!reorderExample.conflict()){
            writerThread.start();
            readerThread.start();
        }
    }




    static  class WriterThread implements  Runnable{

        private ReorderExample reorderExample;

        public WriterThread(ReorderExample reorderExample){
            this.reorderExample = reorderExample;
        }

        @Override
        public void run() {
            reorderExample.writer();
        }
    }

    static class ReaderThread implements  Runnable{

        private ReorderExample reorderExample;

        public ReaderThread(ReorderExample reorderExample){
            this.reorderExample = reorderExample;
        }

        @Override
        public void run() {
            reorderExample.reader();
        }
    }


}
