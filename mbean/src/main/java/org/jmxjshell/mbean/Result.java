package org.jmxjshell.mbean;
import java.beans.ConstructorProperties;
import java.io.Serializable;

public class Result implements Serializable {
    final String outLog;
    final String errLog;

    @ConstructorProperties({"outLog", "errLog"})
    public Result(String outLog, String errLog) {
        this.outLog = outLog;
        this.errLog = errLog;
    }

    public String getOutLog() {
        return outLog;
    }

    public String getErrLog() {
        return errLog;
    }
}
