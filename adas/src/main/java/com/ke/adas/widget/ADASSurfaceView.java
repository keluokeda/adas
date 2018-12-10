package com.ke.adas.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import bean.DrawShape;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class ADASSurfaceView extends SurfaceView implements SurfaceHolder.Callback {


    private static final int LINE = 0;
    private static final int RECT = 1;
    //    private static final int TEXT = 2;
    private static final int SLINE = 3;

    private SurfaceHolder mSurfaceHolder;
    private Canvas canvas;
    private Paint paint;
    private Disposable mDisposable;
    private ObservableEmitter<List<DrawShape>> mSubscriber;


    public ADASSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(true);
        setZOrderOnTop(true);
        this.setKeepScreenOn(true);
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        paint = new Paint();
        paint.setAntiAlias(true);

        post(() -> {
            Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;// 得到宽度
            int screenHeight = displayMetrics.heightPixels;// 得到高度
            setScreenWidth(screenWidth);
            setScreenHeight(screenHeight);
        });
    }

    private void initSubscription() {
        this.mDisposable = Observable.create((ObservableOnSubscribe<List<DrawShape>>) e -> ADASSurfaceView.this.mSubscriber = e)
                .subscribeOn(Schedulers.computation())
                .debounce(16, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .subscribe(this::draw);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initSubscription();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mDisposable != null) {
            mDisposable.dispose();
            mDisposable = null;
            mSubscriber = null;
        }
    }


    public void setDrawList(@NonNull List<DrawShape> drawList) {
        if (mSubscriber != null) {
            mSubscriber.onNext(drawList);
        }
    }

    private int screenWidth;
    private int screenHeight;


    private void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    private void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    private void clear(Canvas canvas) {
        if (canvas == null) {
            return;
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.canvas.drawPaint(paint);
        paint.reset();

    }


    private void draw(List<DrawShape> drawList) {
        canvas = mSurfaceHolder.lockCanvas();
        if (drawList.size() > 0 && canvas != null) {
            try {

                int width = 1280;
                float scaleWidth = (float) getWidth() / width;
                int height = 720;
                float scaleHeight = (float) getHeight() / height;
                clear(canvas);
                for (int i = 0; i < drawList.size(); i++) {
                    DrawShape ds = drawList.get(i);
                    int type = ds.getType();

                    float x0 = ds.getX0() * scaleWidth;
                    float y0 = ds.getY0() * scaleHeight;
                    float x1 = ds.getX1() * scaleWidth;
                    float y1 = ds.getY1() * scaleHeight;
                    float stroke_width = ds.getStroke_width();
                    int color = ds.getColor();
                    boolean isDashed = ds.isDashed();

                    drawLineOrRect(canvas, type, x0, y0, x1, y1, stroke_width, color, isDashed, ds.getTextStr(), paint);
                    paint.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        drawList.clear();
        if (canvas != null && mSurfaceHolder != null) {
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }


    }


    void drawLineOrRect(Canvas canvas, int type, float x0, float y0, float x1, float y1, float stroke_width, int color, boolean isDashed, String text, Paint paint) {

        switch (type) {
            // 画线
            case LINE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke_width);
                paint.setColor(color);
                Path path = new Path();
                path.moveTo(x0, y0);
                path.lineTo(x1, y1);
                if (isDashed) {
                    PathEffect effects = new DashPathEffect(new float[]{20, 10}, 1);
                    paint.setPathEffect(effects);
                }
                canvas.drawPath(path, paint);
                break;
            // 画矩形
            case RECT:
                paint.setColor(color);// 设置红色
                paint.setStyle(Paint.Style.FILL);//设置实心
                paint.setAlpha(80);

                paint.setStrokeWidth(stroke_width);
                if (isDashed) {
                    PathEffect effects = new DashPathEffect(new float[]{5, 5, 5, 5}, 1);
                    paint.setPathEffect(effects);
                }

                canvas.drawRect(x0, y0, x1, y1, paint);// 矩形

                //设置画框
                Paint paintB = new Paint();
                paintB.setColor(color);// 设置红色
                paintB.setStyle(Paint.Style.STROKE);//设置空心
                paintB.setStrokeWidth(stroke_width);
                canvas.drawRect(x0, y0, x1, y1, paintB);// 矩形

                Paint paintText = new Paint();
                paintText.setColor(color);// 设置红色
//                paintText.setTypeface(Typeface.create("宋体", Typeface.BOLD));
                paintText.setAntiAlias(true);//去除锯齿
                paintText.setFilterBitmap(true);//对位图进行滤波处理
                paintText.setTextSize(60);

                float tY = y1 + 50;
                canvas.drawText(text, x0, tY, paintText);
                break;
            case SLINE:

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(50);
                paint.setAntiAlias(true);//去除锯齿
                paint.setFilterBitmap(true);//对位图进行滤波处理
                paint.setColor(color);
                Path pathsLine = new Path();
                pathsLine.moveTo(x0, y0);
                pathsLine.lineTo(x1, y1);
                canvas.drawPath(pathsLine, paint);

                break;
            default:
                break;
        }

    }


}
