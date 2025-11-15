package org.uvccamera.flutter;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.serenegiant.usb.IFrameCallback;

import java.nio.ByteBuffer;

import io.flutter.plugin.common.EventChannel;

/**
 * Camera frame event stream handler
 */
/* package-private */ class UvcCameraFrameEventStreamHandler implements EventChannel.StreamHandler {

    /**
     * Log tag
     */
    private static final String TAG = UvcCameraFrameEventStreamHandler.class.getCanonicalName();

    /**
     * The event sink
     */
    private EventChannel.EventSink eventSink;

    /**
     * Lock for {@link #eventSink}
     */
    private final Object eventSinkLock = new Object();

    /**
     * Handler for posting to main thread
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Returns the event sink
     *
     * @return the event sink
     */
    public EventChannel.EventSink getEventSink() {
        synchronized (eventSinkLock) {
            return eventSink;
        }
    }

    /**
     * Send frame data to Flutter
     *
     * @param frameData the frame data as byte array
     */
    public void sendFrame(final byte[] frameData) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (eventSinkLock) {
                    if (eventSink != null) {
                        eventSink.success(frameData);
                    }
                }
            }
        });
    }

    /**
     * Create IFrameCallback that sends frames via this handler
     *
     * @return IFrameCallback instance
     */
    public IFrameCallback createFrameCallback() {
        return new IFrameCallback() {
            @Override
            public void onFrame(ByteBuffer frame) {
                if (frame != null && frame.hasRemaining()) {
                    byte[] frameData = new byte[frame.remaining()];
                    frame.get(frameData);
                    sendFrame(frameData);
                }
            }
        };
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink eventSink) {
        Log.v(TAG, "onListen: arguments=" + arguments + ", eventSink=" + eventSink);

        synchronized (eventSinkLock) {
            this.eventSink = eventSink;
        }
    }

    @Override
    public void onCancel(Object arguments) {
        Log.v(TAG, "onCancel: arguments=" + arguments);

        synchronized (eventSinkLock) {
            this.eventSink = null;
        }
    }

}
