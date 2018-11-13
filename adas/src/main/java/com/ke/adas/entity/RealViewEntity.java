package com.ke.adas.entity;


import bean.DrawShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RealViewEntity {
    public static final int TYPE_FRAME = 0;
    public static final int TYPE_ADAS_INFO = 1;
    public static final int TYPE_ADAS_SENSOR = 2;
    public static final int TYPE_SPEED = 3;
    public static final int TYPE_ERROR = 4;


    public byte[] mBytes;
    public int size;

    public List<DrawShape> mDrawShapes;

    public float x;
    public float y;
    public float z;

    public String speed;

    public int errorCode;

    public final int type;


    public RealViewEntity(byte[] bytes, int size) {
        mBytes = bytes;
        this.size = size;
        type = TYPE_FRAME;
    }

    public RealViewEntity(List<DrawShape> drawShapes) {
        mDrawShapes = drawShapes;
        type = TYPE_ADAS_INFO;
    }

    public RealViewEntity(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        type = TYPE_ADAS_SENSOR;
    }

    public RealViewEntity(String speed) {
        this.speed = speed;
        type = TYPE_SPEED;
    }

    public RealViewEntity(int errorCode) {
        this.errorCode = errorCode;
        type = TYPE_ERROR;
    }

    @Override
    public String toString() {
        return "RealViewEntity{" +
                "mBytes=" + Arrays.toString(mBytes) +
                ", size=" + size +
                ", mDrawShapes=" + mDrawShapes +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", speed='" + speed + '\'' +
                ", errorCode=" + errorCode +
                ", type=" + type +
                '}';
    }
}
