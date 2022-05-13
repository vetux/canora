package com.phaseshifter.canora.utils.android.metrics;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Choreographer;
import android.widget.TextView;

/**
 * All Methods and Constructors shall be called only from the activity_main thread.
 */
public class AndroidFPSMeter {
    private final int NANOS_IN_MILIS = 1000000;

    private final String LOG_TAG = "FPS";
    private final int MESSAGE_FPS_PRINT = 0;

    private final Choreographer choreographer;
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            deltaTime = frameTimeNanos - lastFrameTime;
            lastFrameTime = frameTimeNanos;
            if (runningMonitor)
                choreographer.postFrameCallback(this);
        }
    };
    private final Handler mainHandler;

    private long lastFrameTime = 0;
    private long deltaTime = 1;
    private boolean runningMonitor = false;

    private boolean runningPrinter = false;

    public AndroidFPSMeter(Choreographer choreographer) {
        this.choreographer = choreographer;
        this.mainHandler = new Handler();
    }

    public AndroidFPSMeter() {
        this(Choreographer.getInstance());
    }

    public void reset() {
        stopPrint();
        stopMeasure();
    }

    public void startMeasure() {
        runningMonitor = true;
        choreographer.postFrameCallback(frameCallback);
    }

    public void stopMeasure() {
        runningMonitor = false;
        try {
            choreographer.removeFrameCallback(frameCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPrint(long delayMillis, TextView output) {
        stopPrint();
        runningPrinter = true;
        Runnable fpsPrint = new Runnable() {
            @Override
            public void run() {
                long delta = deltaTime / NANOS_IN_MILIS;
                long fps = delta > 0 ? (1000 / delta) : 0;
                String outputString = delta + " ms / " + fps + " fps";
                if (output != null)
                    output.setText(outputString);
                else
                    Log.v(LOG_TAG, outputString);
                if (runningPrinter) {
                    Message m = Message.obtain(mainHandler, this);
                    m.what = MESSAGE_FPS_PRINT;
                    mainHandler.sendMessageDelayed(m, delayMillis);
                }
            }
        };
        Message m = Message.obtain(mainHandler, fpsPrint);
        m.what = MESSAGE_FPS_PRINT;
        mainHandler.sendMessage(m);
    }

    public void startPrint(long delayMillis) {
        startPrint(delayMillis, null);
    }

    public void stopPrint() {
        runningPrinter = false;
        mainHandler.removeMessages(MESSAGE_FPS_PRINT);
    }

    public long getFPS() {
        return (1000 / deltaTime);
    }
}