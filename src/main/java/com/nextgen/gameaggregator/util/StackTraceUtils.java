package com.nextgen.gameaggregator.util;

import java.io.StringWriter;
import java.io.PrintWriter;

public class StackTraceUtils {
    public static String getStackTraceAsString(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        e.printStackTrace(printWriter);

        // Return the stack trace as a string
        return stringWriter.toString();
    }
}
