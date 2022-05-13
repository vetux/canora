package com.phaseshifter.canora.soundcloud.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogKeeper {
    private final StringBuffer log = new StringBuffer();
    private final int logMaxLength;

    public LogKeeper() {
        logMaxLength = -1;
    }

    public LogKeeper(int logMaxLength) {
        this.logMaxLength = logMaxLength;
    }

    public synchronized void log(String logTag, String text) {
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + " L/" + logTag + ": ";
        if (logMaxLength != -1 && log.length() > logMaxLength) {
            log.delete(0, text.length() + timestamp.length());
        }
        log.append(timestamp)
                .append(text)
                .append("\n");
    }

    public synchronized void clear() {
        log.delete(0, log.length());
    }

    public synchronized String getLog() {
        return log.toString();
    }

    public synchronized void printLog() {
        System.out.println(log.toString());
    }
}
