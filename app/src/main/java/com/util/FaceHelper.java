package com.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.support.annotation.Nullable;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by ycxu on 17-2-25.
 *
 * @author ycxu 282331204@qq.com
 * @version 1.1 添加缩放策略
 */

public class FaceHelper {
    private static final String TAG = "FaceHelper";
    private volatile static FaceHelper instance;
    private int maxFaceNum;
    private Paint paint;
    private float rectFlagOffset;
    private int mBitmapWidth, mBitmapHeight;
    private boolean isZoom;
    private float mZoomValue;
    private FaceDetector detector;

    private FaceHelper() {
        //no instance
        maxFaceNum = 1;
        rectFlagOffset = 1;
        mZoomValue = 1;
    }

    public static FaceHelper getInstance() {
        if (null == instance) {
            synchronized (FaceHelper.class) {
                if (null == instance) {
                    instance = new FaceHelper();
                }
            }
        }
        return instance;
    }

    public Bitmap decodeBitmap(byte[] data, int width, int height) {
        if (data == null) {
            return null;
        }
        YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置成565,才能进行人脸识别
        Bitmap raw = BitmapFactory.decodeByteArray(stream.toByteArray(),
                0, stream.size(), options);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return raw;
    }

    /**
     * @param data
     * @param width
     * @param height
     * @return
     */
    @Nullable
    public Face[] findFaces(byte[] data, int width, int height) {
        if (data == null || data.length < 1) {
            return null;
        }
        return findFaces(decodeBitmap(data, width, height));
    }

    public Bitmap faceCrop(Rect rect, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(1, 1);

        bitmap = Bitmap.createBitmap(bitmap,
                rect.left,
                rect.top,
                rect.right - rect.left,
                rect.bottom - rect.top,
                matrix, false);


        return bitmap;
    }

    @Nullable
    public Face[] findFaces(Bitmap source) {
      //  Logger.i("图片尺寸："+source.getWidth()+","+source.getHeight());
        if (source == null) {
            return null;
        }
        if (isZoom) {
            Matrix matrix = new Matrix();
            matrix.postScale(mZoomValue, mZoomValue);
            //matrix.postRotate(180);
            source = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(),
                    source.getHeight(), matrix, true);
            mBitmapWidth = source.getWidth();
            mBitmapHeight = source.getHeight();
        }

        this.detector = new FaceDetector(source.getWidth(),
                source.getHeight(),
                maxFaceNum);
        Face[] faces = new Face[maxFaceNum];
       int detectorFaces = this.detector.findFaces(source, faces);
//        LogUtils.e(TAG, "findFaces: Find face : " + detectorFaces);
//        ArrayList<Face> list = new ArrayList<>();
//        for (int i = 0; i < faces.length; i++) {
//            if (faces[i]!=null&&faces[i].confidence()>0.5F) {
//                list.add(faces[i]);
//            }
//        }
        return faces;
    }


    public Canvas drawFace(Face[] faces, Canvas canvas) {
        for (Face face : faces) {
            drawFace(face, canvas);
        }
        return canvas;
    }

    public Bitmap drawFace(Face[] faces, Bitmap source) {
        for (int i = 0; i < faces.length; i++) {

            source = drawFace(faces[i], source);
        }
        return source;
    }

    public Bitmap drawFace(Face face, Bitmap source) {
        Canvas canvas = new Canvas(source);
        drawFace(face, canvas);
        return source;
    }

    public Canvas drawFace(Face face, Canvas canvas) {
        if (null == canvas || null == face) {
            return null;
        }
        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
        }


        canvas.drawRect(getFaceRect(face,
                canvas.getWidth(),
                canvas.getHeight()),
                paint);

        return canvas;
    }

    /**
     * 需要在{@link #findFaces(Bitmap)}之前调用，否则无效。
     *
     * @param maxFaceNum
     */
    public void setMaxFaceNum(int maxFaceNum) {
        this.maxFaceNum = maxFaceNum;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void setZoom(float value) {
        if (isZoom = value > 0) {
            this.mZoomValue = value;
        }
    }

    public Rect getFaceRect(Face face, int width, int height) {


        PointF pointF = new PointF();
        face.getMidPoint(pointF);

        float eyesDistance = face.eyesDistance();
        float offset = eyesDistance * (rectFlagOffset / mZoomValue);
        if (isZoom) {
            pointF.x = width * (pointF.x / mBitmapWidth);
            pointF.y = height * (pointF.y / mBitmapHeight);
        }
        //边界检测
        int left = (int) (pointF.x - offset < 0 ? 0 : pointF.x - offset);
        int top = (int) (pointF.y - offset + 10 < 0 ? 0 : pointF.y - offset + 10);
        int right = (int) (pointF.x + offset > width ? width : pointF.x + offset);
        int bottom = (int) (pointF.y + offset + 10 > height ? height : pointF.y + offset + 10);
        return new Rect(left, top, right, bottom);
    }

    public boolean isZoom() {
        return isZoom;
    }

    public int getMaxFaceNum() {
        return maxFaceNum;
    }


    public float getRectFlagOffset() {
        return rectFlagOffset;
    }


    public void setRectFlagOffset(float rectFlagOffset) {
        this.rectFlagOffset = rectFlagOffset;
    }


}
