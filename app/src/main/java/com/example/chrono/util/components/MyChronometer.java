package com.example.chrono.util.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class MyChronometer extends androidx.appcompat.widget.AppCompatTextView {
    @SuppressWarnings("unused")
    private static final String TAG = "Chronometer";
    private static final int TICK_WHAT = 2;
    private long mBase;
    private boolean mVisible;
    private boolean mStarted;
    private boolean mRunning;
    private OnChronometerTickListener mOnChronometerTickListener;
    private long timeElapsed;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(@NotNull Message m) {
            if (mRunning) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                sendMessageDelayed(Message.obtain(this, TICK_WHAT),
                        10);
            }
        }
    };

    public MyChronometer(Context context) {
        super(context);
    }

    public MyChronometer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyChronometer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mBase = SystemClock.elapsedRealtime();
        updateText(mBase);
    }

    public long getBase() {
        return mBase;
    }

    public void setBase(long base) {
        mBase = base;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    public OnChronometerTickListener getOnChronometerTickListener() {
        return mOnChronometerTickListener;
    }

    public void setOnChronometerTickListener(
            OnChronometerTickListener listener) {
        mOnChronometerTickListener = listener;
    }

    public void start() {
        mStarted = true;
        updateRunning();
    }

    public void stop() {
        mStarted = false;
        updateRunning();
    }

    public void setStarted(boolean started) {
        mStarted = started;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        updateRunning();
    }

    private synchronized void updateText(long now) {
        timeElapsed = now - mBase;

        DecimalFormat df = new DecimalFormat("00");

        int hours = (int) (timeElapsed / (3600 * 1000));
        int remaining = (int) (timeElapsed % (3600 * 1000));

        int minutes = (remaining / (60 * 1000));
        remaining = (remaining % (60 * 1000));

        int seconds = (remaining / 1000);
        remaining = (remaining % (1000));

        int milliseconds = (((int) timeElapsed % 1000) / 100);
        remaining = (remaining % (100));

        int tenthmillisecond = (remaining % 10);

        String text = "";

        if (hours > 0) {
            text += df.format(hours) + ":";
        }

        text += df.format(minutes) + ":";
        text += df.format(seconds) + ":";
        text += milliseconds + Integer.toString(tenthmillisecond);

        setText(text);
    }

    private void updateRunning() {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                mHandler.sendMessageDelayed(Message.obtain(mHandler,
                        TICK_WHAT), 10);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            mRunning = running;
        }
    }

    void dispatchChronometerTick() {
        if (mOnChronometerTickListener != null) {
            mOnChronometerTickListener.onChronometerTick(this);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

    public interface OnChronometerTickListener {
        void onChronometerTick(MyChronometer chronometer);
    }
}