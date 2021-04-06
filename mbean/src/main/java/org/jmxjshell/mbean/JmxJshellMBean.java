package org.jmxjshell.mbean;

public interface JmxJshellMBean {
    void clear();
    Result run(String code);
}
