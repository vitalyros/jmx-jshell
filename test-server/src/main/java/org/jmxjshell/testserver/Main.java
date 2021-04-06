package org.jmxjshell.testserver;

import org.jmxjshell.mbean.JmxJshell;

public class Main {
    public static int X = 1;
    public static void main(String[] args)
            throws Exception {
        JmxJshell jmxJshell = new JmxJshell();
        System.out.println("Waiting forever...");
        Thread.sleep(Long.MAX_VALUE);
    }
}
