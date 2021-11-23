package com.ke.adas.widget;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import androidx.core.util.Pair;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final byte[] sps = {0, 0, 0, 1, 103, 66, 0, 41, -115, -115, 64, 40, 2, -35, 0, -16, -120, 69, 56};
    private static final byte[] pps = {0, 0, 0, 1, 104, -54, 67, -56};

    SurfaceHolder mSurfaceHolder;
    MediaCodec mMediaCodec;
    int mCount = 0;

    public final PublishSubject<Boolean> initSubject = PublishSubject.create();

    private boolean init = false;


    // Video Constants
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 1280;
    private final static int VIDEO_HEIGHT = 720;
    private final static int TIME_INTERNAL = 30;

    private ObservableEmitter<Pair<byte[], Integer>> subscriber;
    private Disposable mDisposable;


    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void initSubscription() {
        mDisposable = Observable
                .create((ObservableOnSubscribe<Pair<byte[], Integer>>) e -> VideoSurfaceView.this.subscriber = e)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.newThread())
                .subscribe(pair -> onFrame(pair.first, pair.second));
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        Logger.d("surface view create");
        initDecoder(holder);
        initSubscription();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        Logger.d("surface view destroy , count = " + mCount);
        if (mDisposable != null) {
            mDisposable.dispose();
            mDisposable = null;
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    private void initDecoder(SurfaceHolder holder) {

        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, 1280, 720);
            format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
            /* create & config android.media.MediaCodec */
            mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            mMediaCodec.configure(format, holder.getSurface(), null, 0);//blind surfaceView
            mMediaCodec.start(); //start decode thread
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setOnePixData(byte[] buf, int size) {


        byte[] dataBuf = new byte[size];
        System.arraycopy(buf, 0, dataBuf, 0, size);

        Pair<byte[], Integer> pair = Pair.create(dataBuf, size);
        if (mDisposable != null) {
            subscriber.onNext(pair);
        }


    }

    private void onFrame(byte[] buf, int length) {

        if (!init) {
            if (!isFirstFrame(buf)) {
                return;
            } else {
                init = true;
                initSubject.onNext(true);
            }
        }

        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(100);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, 0, length);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
        while (outputBufferIndex >= 0) {
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }


    /**
     * 是否是第一帧
     */
    private boolean isFirstFrame(byte[] buffer) {
        return (buffer[0] == 0x00 && buffer[1] == 0x00 && buffer[2] == 0x00
                && buffer[3] == 0x01 && buffer[4] == 0x67);
    }


}

