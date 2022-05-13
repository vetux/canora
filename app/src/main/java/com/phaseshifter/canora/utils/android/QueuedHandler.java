package com.phaseshifter.canora.utils.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Queued Handler Thread. You can immediately post messages as they are queued internally,
 * and executed by the handler when the looper thread has initialized.
 * <p>
 * Calling quit before the thread is started will immediately quit the thread once it starts up.
 * <p>
 * Calling clear before the thread is started clears the queued runnables.
 * <p>
 * The runnables and messages will be posted in the same order that they arrived on the QueuedHandler, even when posted while the Looper thread has not been initialized.
 */
public class QueuedHandler extends Thread {
    private static class DelayedRunnable {
        private final Runnable runnable;
        private final long delay;

        DelayedRunnable(Runnable runnable, long delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }

    private static class DelayedMessage {
        private final Message message;
        private final long delay;

        DelayedMessage(Message message, long delay) {
            this.message = message;
            this.delay = delay;
        }
    }

    private Handler handler = null;

    private final List<Object> queue = new ArrayList<>();
    private final AtomicReference<Boolean> isQuitted = new AtomicReference<>(false);

    public QueuedHandler() {
    }

    /**
     * Posts the runnable r to the handler. If the underlying thread has not been started yet the runnable will be queued and posted when the thread has started.
     * The order will be retained.
     *
     * @param r The runnable to be posted.
     */
    public synchronized void post(Runnable r) {
        if (handler == null) {
            queue.add(r);
        } else {
            handler.post(r);
        }
    }

    /**
     * Posts the runnable r to the handler with the specified delay. If the underlying thread has not been started yet the runnable will be queued and posted when the thread has started.
     *
     * @param r     The runnable to be posted.
     * @param delay The delay, relative to the start time of the thread if it hasnt started yet.
     */
    public synchronized void postDelayed(Runnable r, long delay) {
        if (handler == null) {
            queue.add(new DelayedRunnable(r, delay));
        } else {
            handler.postDelayed(r, delay);
        }
    }

    public synchronized void send(Message msg) {
        if (handler == null) {
            queue.add(msg);
        } else {
            handler.sendMessage(msg);
        }
    }

    public synchronized void sendDelayed(Message msg, long delay) {
        if (handler == null) {
            queue.add(new DelayedMessage(msg, delay));
        } else {
            handler.sendMessageDelayed(msg, delay);
        }
    }

    public synchronized void clear() {
        if (handler == null) {
            queue.clear();
        } else {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public synchronized void quit() {
        if (handler == null) {
            isQuitted.set(true);
        } else {
            handler.getLooper().quit();
            handler = null;
        }
    }

    public synchronized void quitSafely() {
        if (handler == null) {
            isQuitted.set(true);
        } else {
            handler.getLooper().quitSafely();
            handler = null;
        }
    }

    public synchronized void removeCallbacks(Runnable r) {
        if (handler == null) {
            List<Object> toRemove = new ArrayList<>();
            for (Object obj : queue) {
                if (obj instanceof Runnable && obj.equals(r)) {
                    toRemove.add(obj);
                } else if (obj instanceof DelayedRunnable && obj.equals(r)) {
                    toRemove.add(obj);
                }
            }
            queue.removeAll(toRemove);
        } else {
            handler.removeCallbacks(r);
        }
    }

    public synchronized void removeMessages(int what) {
        if (handler == null) {
            List<Object> toRemove = new ArrayList<>();
            for (Object obj : queue) {
                if (obj instanceof Message && ((Message) obj).what == what) {
                    toRemove.add(obj);
                } else if (obj instanceof DelayedMessage && ((DelayedMessage) obj).message.what == what) {
                    toRemove.add(obj);
                }
            }
            queue.removeAll(toRemove);
        } else {
            handler.removeMessages(what);
        }
    }

    public synchronized Handler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        Looper myLooper = Looper.myLooper();
        if (myLooper == null)
            throw new RuntimeException("Fatal Looper Initialization Error.");
        if (initialize(myLooper))
            Looper.loop();
    }

    private synchronized boolean initialize(Looper looper) {
        if (isQuitted.get())
            return false;
        handler = new Handler(looper) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.obj instanceof Runnable) {
                    ((Runnable) msg.obj).run();
                }
            }
        };
        for (Object obj : queue) {
            if (obj instanceof Runnable) {
                handler.post((Runnable) obj);
            } else if (obj instanceof DelayedRunnable) {
                DelayedRunnable dr = (DelayedRunnable) obj;
                handler.postDelayed(dr.runnable, dr.delay);
            } else if (obj instanceof Message) {
                handler.sendMessage((Message) obj);
            } else if (obj instanceof DelayedMessage) {
                DelayedMessage dm = (DelayedMessage) obj;
                handler.sendMessageDelayed(dm.message, dm.delay);
            } else {
                throw new RuntimeException("Fatal error. Encountered invalid object type in handler queue for object: " + obj);
            }
        }
        queue.clear();
        return true;
    }
}