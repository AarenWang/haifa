package org.wrj.haifa.jvmstudy;

public class MethodInvokeTraceRunner {

    public static void target(int i) {
        new Exception("#" + i).printStackTrace();
    }
}
