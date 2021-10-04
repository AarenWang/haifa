package org.wrj.haifa.jvmstudy;

import java.lang.reflect.Method;

public class MethodInvokeTrace {


    public static void main(String[] args) throws Exception {
        Class<MethodInvokeTraceRunner> clazz = (Class<MethodInvokeTraceRunner>) Class.forName("org.wrj.haifa.jvmstudy.MethodInvokeTraceRunner");
        Method method = clazz.getDeclaredMethod("target", int.class);
        for (int i = 0; i < 20; i++) {
            method.invoke(null, 0);
        }

    }
}

