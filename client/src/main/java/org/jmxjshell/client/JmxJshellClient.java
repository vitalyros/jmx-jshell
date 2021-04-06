package org.jmxjshell.client;

import org.jmxjshell.mbean.JmxJshellMBean;
import org.jmxjshell.mbean.Result;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import static org.jmxjshell.mbean.JmxJshell.MBEAN_NAME;

public class JmxJshellClient {
    private static PrintStream out = System.out;
    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final static String EXIT_SEQUENCE = "/exit";

    private static String inputCode() throws IOException {
        int openCurlyBracesCount = 0;
        int closedCurlyBracesCount = 0;
        boolean firstLine = true;
        StringBuilder acc = new StringBuilder();

        while (firstLine || closedCurlyBracesCount < openCurlyBracesCount) {
            String identation = "";
            if (firstLine) {
                out.print("jmx-jshell> ");
            } else {
                int identationDepth = openCurlyBracesCount - closedCurlyBracesCount;
                identation = new String(new char[identationDepth]).replace("\0", "    ");
                out.print("       ...> " +  identation);
            }
            String newLine = in.readLine();
            for (char c : newLine.toCharArray()) {
                if (c == '{') {
                    openCurlyBracesCount++;
                } else if (c == '}') {
                    closedCurlyBracesCount++;
                }
            }
            if (firstLine) {
                firstLine = false;
            } else {
                acc.append("\n");
                acc.append(identation);
            }
            acc.append(newLine);
        }
        return acc.toString();
    }

    private static void outputResult(Result result) throws IOException {
        String outLog = result.getOutLog();
        String errLog = result.getErrLog();
        if (!outLog.isEmpty()) {
            out.println(outLog);
        }
        if (!errLog.isEmpty()) {
            String error = "Error:\n" + errLog;
            String[] lines = error.split("\r?\n\r?");
            String formattedError = "|  " + String.join("\n|  ", lines);
            out.println(formattedError);
            out.println();
        }
    }

    private static void runRepl(JmxJshellMBean mBean) throws IOException {
        boolean exit = false;
        while (!exit) {
            String code = inputCode();
            if (code.trim().equals(EXIT_SEQUENCE)) {
                exit = true;
                out.print("|  Goodbye");
            }
            Result result = mBean.run(code);
            outputResult(result);
        }
    }

    public static void main(String[] args) throws Exception {
        String shortUrl = System.getProperty("jmxshell.url");
        String url = "service:jmx:rmi:///jndi/rmi://" + shortUrl + "/jmxrmi";
        JMXServiceURL serviceUrl = new JMXServiceURL(url);
        JMXConnector jmxConnector;
        MBeanServerConnection mbeanConn;
        try {
            jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
            mbeanConn = jmxConnector.getMBeanServerConnection();
        } catch (Exception e) {
            out.println("Failed to connect by JMX for url " + url);
            throw e;
        }
        Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
        Optional<ObjectName> optionalObjectName =  beanSet.stream().filter(new Predicate<ObjectName>() {
            @Override
            public boolean test(ObjectName objectName) {
                return objectName.getCanonicalName().equals(MBEAN_NAME);
            }
        }).findAny();
        if (optionalObjectName.isPresent()) {
            JmxJshellMBean mBean = JMX.newMBeanProxy(mbeanConn, optionalObjectName.get(), JmxJshellMBean.class);
            try {
                runRepl(mBean);
            } finally {
                jmxConnector.close();
            }
        } else {
            out.println("Connected by JMX but failed to find jmxshell MBean");
        }
    }
}
