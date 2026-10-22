package com.deepfalldrill.game;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowInsets;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Owns the render thread and forwards raw input. All game state is touched by
 * the render thread only; input is queued, so there are no locks in the loop.
 */
public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, Runnable, SensorEventListener {

    public static final int EV_DOWN = 0;
    public static final int EV_MOVE = 1;
    public static final int EV_UP = 2;
    public static final int EV_BACK = 3;
    public static final int EV_APP_PAUSE = 4;

    private static final class TouchEvent {
        int type;
        int id;
        float x;
        float y;
    }

    private final ConcurrentLinkedQueue<TouchEvent> events = new ConcurrentLinkedQueue<>();
    private final SensorManager sensors;
    private final Sensor accelerometer;

    private Thread thread;
    private volatile boolean running;
    private volatile float tiltRaw;
    private volatile boolean tiltSeen;
    private volatile boolean surfaceReady;

    private Game game;
    private int safeTop;
    private int safeBottom;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
        setOnApplyWindowInsetsListener((v, insets) -> {
            safeTop = insetsTop(insets);
            safeBottom = insetsBottom(insets);
            return insets;
        });

        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensors != null ? sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    private int insetsTop(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()).top;
        }
        return insets.getSystemWindowInsetTop();
    }

    private int insetsBottom(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        }
        return insets.getSystemWindowInsetBottom();
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "DeepfallRender");
        thread.start();
    }

    public void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try {
                t.join(1200);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (game == null) {
            game = new Game(getContext(), getWidth(), getHeight());
        }
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (game != null) game.onViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stop();
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            float dt = (frameStart - last) / 1_000_000_000f;
            last = frameStart;
            if (dt > 0.05f) dt = 0.05f;

            TouchEvent ev;
            while ((ev = events.poll()) != null) {
                if (game != null) game.onInput(ev.type, ev.id, ev.x, ev.y);
            }
            if (game != null) {
                game.setTilt(tiltRaw, tiltSeen);
                game.update(dt);
                Canvas canvas = null;
                try {
                    SurfaceHolder holder = getHolder();
                    canvas = Build.VERSION.SDK_INT >= 23
                            ? holder.lockHardwareCanvas()
                            : holder.lockCanvas();
                    if (canvas != null && surfaceReady) {
                        game.draw(canvas, getWidth(), getHeight(), safeTop, safeBottom);
                    }
                } catch (Throwable ignored) {
                    // A bad frame must never kill the render thread.
                } finally {
                    if (canvas != null) {
                        try {
                            getHolder().unlockCanvasAndPost(canvas);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            long worked = System.nanoTime() - frameStart;
            long sleepNs = 16_666_667L - worked;
            if (sleepNs > 1_000_000L) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = event.getActionIndex();
                post(EV_DOWN, event.getPointerId(idx), event.getX(idx), event.getY(idx));
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    post(EV_MOVE, event.getPointerId(i), event.getX(i), event.getY(i));
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = event.getActionIndex();
                post(EV_UP, event.getPointerId(idx), event.getX(idx), event.getY(idx));
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    post(EV_UP, event.getPointerId(i), event.getX(i), event.getY(i));
                }
                return true;
            }
            default:
                return super.onTouchEvent(event);
        }
    }

    private void post(int type, int id, float x, float y) {
        TouchEvent e = new TouchEvent();
        e.type = type;
        e.id = id;
        e.x = x;
        e.y = y;
        events.add(e);
    }

    public boolean handleBack() {
        if (game != null && game.isMenu()) return false;
        post(EV_BACK, -1, 0, 0);
        return true;
    }

    public void onAppPause() {
        post(EV_APP_PAUSE, -1, 0, 0);
    }

    // ----------------------------------------------------------------- sensor
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sensors != null && accelerometer != null) {
            sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (sensors != null) sensors.unregisterListener(this);
        stop();
        if (game != null) game.release();
        super.onDetachedFromWindow();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Portrait: tilting the phone to the right produces negative X.
            tiltRaw = -event.values[0];
            tiltSeen = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
