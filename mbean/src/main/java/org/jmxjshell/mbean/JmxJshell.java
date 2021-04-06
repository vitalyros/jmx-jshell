package org.jmxjshell.mbean;

import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class JmxJshell implements JmxJshellMBean {
    public static final String MBEAN_NAME = "org.jmxjshell:type=JmxJshell";

    private JShell jshell;
    private ByteArrayOutputStream outBaos;
    private ByteArrayOutputStream errBaos;
    private ByteArrayInputStream inBaos;
    private PrintStream outPs;
    private PrintStream errPs;

    public JmxJshell() throws Exception {
        open();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(MBEAN_NAME);
        mbs.registerMBean(this, name);
    }

    private void open() {
        outBaos = new ByteArrayOutputStream();
        errBaos = new ByteArrayOutputStream();
        inBaos = new ByteArrayInputStream(new byte[1024]);
        outPs = new PrintStream(outBaos);
        errPs = new PrintStream(errBaos);
        JShell.Builder builder = JShell.builder();
        builder.executionEngine(new LocalExecutionControlProvider(), null);
        builder.out(outPs);
        builder.err(errPs);
        builder.in(inBaos);
        jshell = builder.build();
    }

    private void close() {
        outPs.close();
        errPs.close();
        try {
            inBaos.close();
        } catch (Exception e) {
        }
        jshell.close();
    }


    @Override
    public void clear() {
        close();
        open();
    }

    @Override
    public Result run(String code) {
        try {
            List<SnippetEvent> events = jshell.eval(code);
            events.forEach(new Consumer<SnippetEvent>() {
                @Override
                public void accept(SnippetEvent snippetEvent) {
                    System.out.println(snippetEvent);
                    if (snippetEvent.status() != Snippet.Status.VALID) {
                        if (snippetEvent.exception() != null) {
                            snippetEvent.exception().printStackTrace(errPs);
                        }
                        if (snippetEvent.snippet() != null) {
                            jshell.diagnostics(snippetEvent.snippet()).forEach(new Consumer<Diag>() {
                                @Override
                                public void accept(Diag diag) {
                                    errPs.println(diag.getMessage(Locale.ENGLISH));
                                }
                            });
                        }
                        if (snippetEvent.causeSnippet() != null) {
                            jshell.diagnostics(snippetEvent.causeSnippet()).forEach(new Consumer<Diag>() {
                                @Override
                                public void accept(Diag diag) {
                                    errPs.println(diag.getMessage(Locale.ENGLISH));
                                }
                            });
                        }
                    } else {
                        if (snippetEvent.value() != null) {
                            outPs.println(snippetEvent.value());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace(errPs);
        }
        errPs.flush();
        outPs.flush();
        try {
            String outStr = outBaos.toString("UTF-8");
            outBaos.reset();
            String errStr = errBaos.toString("UTF-8");
            errBaos.reset();
            return new Result(outStr, errStr);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
