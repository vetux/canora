package com.phaseshifter.canora.utils.android.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class AndroidMemoryMeter {
    private final int MESSAGE_MEM_PRINT = 0;
    private final Handler handler;

    private boolean runningPrinter = false;

    public AndroidMemoryMeter() {
        this.handler = new Handler();
    }

    public void reset() {
        stopPrint();
    }

    public void startPrint(long delayMillis, TextView output, Context host) {
        stopPrint();
        runningPrinter = true;
        Runnable fpsPrint = new Runnable() {
            @Override
            public void run() {
                String mem = getMemString(host);
                if (output != null)
                    output.setText(mem);
                if (runningPrinter) {
                    Message m = Message.obtain(handler, this);
                    m.what = MESSAGE_MEM_PRINT;
                    handler.sendMessageDelayed(m, delayMillis);
                }
            }
        };
        Message m = Message.obtain(handler, fpsPrint);
        m.what = MESSAGE_MEM_PRINT;
        handler.sendMessage(m);
    }

    public void stopPrint() {
        runningPrinter = false;
        handler.removeMessages(MESSAGE_MEM_PRINT);
    }

    private String getMemString(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        if (activityManager != null)
            activityManager.getMemoryInfo(memoryInfo);
        return "MEM: " + memoryInfo.availMem / 1000000 + "/" + memoryInfo.totalMem / 1000000 + " " + memoryInfo.threshold / 1000000;
    }
}