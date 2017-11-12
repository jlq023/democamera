package com.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.administrator.democaream.R;
import com.orhanobut.logger.Logger;
import com.util.AutoFitTextureView;
import com.util.FaceHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Administrator on 2017/11/8.
 */

public class CameraAct extends AppCompatActivity implements Camera.PreviewCallback,View.OnClickListener{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.act_camera);
        initView();
        mFaceHelper = FaceHelper.getInstance();
        mFaceHelper.setZoom(1);
        mFaceHelper.setRectFlagOffset(2.0f);
        mFaceHandleThread = new HandlerThread("face");
        mFaceHandleThread.start();
        mFaceHandle = new Handler(mFaceHandleThread.getLooper());
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isFirst=true;
                initCarema();
            } else {
                Toast.makeText(this, "CAMERA PERMISSION DENIED", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case  R.id.xz0:
                setCameraOrien(0);
                break;
            case  R.id.xz90:
                setCameraOrien(90);
                break;
            case  R.id.xz180:
                setCameraOrien(180);
                break;
            case  R.id.xz270:
                setCameraOrien(270);
                break;
            case R.id.startCaream:
                setCameraOrien(-1);
                break;
            case R.id.openCaremaSize:
                if (!isExpend) {
                    // 复原
                    caremaSizeText.setMinLines(0);
                    caremaSizeText.setMaxLines(Integer.MAX_VALUE);
                    openCaremaSize.setText("收缩");
                    isExpend=true;
                } else {
                    caremaSizeText.setLines(3);
                    openCaremaSize.setText("展开");
                    isExpend=false;
                }
                break;
            case R.id.xzzd:
                xzzd=true;
                break;
        }

    }

    private void setCameraOrien(int value){
        if(value>=0){
            mOrienta=value;
        }
        rotation.setText("相机旋转角度："+value);
        stopCamera();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        initCarema();
    }

    private void stopCamera(){
        Logger.e(TAG+"相机关闭："+mCamera);
        if (mCamera != null) {
            //mPreview.setCamera(null);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            xzzd = false;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Logger.i(TAG+"收到相机回调：onpreviewframe()"+index);
        if(data!=null&&data.length>0&&System.currentTimeMillis()-time>200){
            time=System.currentTimeMillis();
            mFaceHandle.post(new FaceThread(data,camera,(++index)));
        }
    }

    private  void initView(){
        textureView = (AutoFitTextureView) findViewById(R.id.mCamera);
        textureView.setScaleX(-1);
        faceStatus = (TextView) findViewById(R.id.faceStatus);
        log = (TextView) findViewById(R.id.log);
        xz0 = (Button) findViewById(R.id.xz0);
        xz0.setOnClickListener(this);
        xz90 = (Button) findViewById(R.id.xz90);
        xz90.setOnClickListener(this);
        xz180 = (Button) findViewById(R.id.xz180);
        xz180.setOnClickListener(this);
        xz270 = (Button) findViewById(R.id.xz270);
        xz270.setOnClickListener(this);
        rotation = (TextView) findViewById(R.id.rotation);
        mImg = (ImageView) findViewById(R.id.mImg);
        sizeIndex = (EditText) findViewById(R.id.sizeIndex);
        startCaream = (Button) findViewById(R.id.startCaream);
        startCaream.setOnClickListener(this);
        openCaremaSize = (Button) findViewById(R.id.openCaremaSize);
        openCaremaSize.setOnClickListener(this);
        caremaSizeText = (TextView) findViewById(R.id.caremaSizeText);
        sizeIndex = (EditText) findViewById(R.id.sizeIndex);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurface = surface;
                initCarema();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void initCarema(){
        try {
            mCamera = Camera.open(1);//可以根据ID使用不同的摄像头
            mCamera.setPreviewTexture(mSurface);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Logger.e(TAG+"相机启动："+mCamera+","+mSurface);
        if(mCamera!=null){
            setCameraDisplayOrientation(CameraAct.this,1,mCamera);
            mCamera.setPreviewCallback(this);
            Camera.Parameters parameters = mCamera.getParameters();
            // parameters.setPreviewFrameRate(3);//设置每秒3帧,没有效果
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();//获得相机预览所支持的大小。
            if(isFirst){
                isFirst = false;
                StringBuffer sb = new StringBuffer();
                int index=0;
                for(Camera.Size cs:previewSizes){
                    sb.append((index++)+"=:");
                    sb.append(cs.width+","+cs.height).append("  ");
                }
                caremaSizeText.setText(sb.toString());
            }else{
                Camera.Size size1 = previewSizes.get(Integer.parseInt(sizeIndex.getText().toString()));//default 2,4
                parameters.setPreviewSize(size1.width, size1.height);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            }
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bun = msg.getData();
            switch (msg.what){
                case 1:
                    int type = bun.getInt("type");
                    if(type==1){
                        faceStatus.setText("识别到人脸");
                    }else{
                        faceStatus.setText("没有识别到人脸");
                    }
                    log.setText(bun.getString("msg")+",act_in:"+index);
                    break;
            }
        }
    };
    int degrees = 0;
    public   void setCameraDisplayOrientation (Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo (cameraId , info);
        int rotation = activity.getWindowManager ().getDefaultDisplay ().getRotation ();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = ( info.orientation - degrees + 360) % 360;
        }
        mOrienta = result;
        camera.setDisplayOrientation (result);
    }

    private class FaceThread implements Runnable {
        private byte[] mData;
        private ByteArrayOutputStream mBitmapOutput;//mUploadOutp1ut
        private Matrix mMatrix;
        private Message mMessage;
        private Camera mtCamera;
        private int index;
        public FaceThread(byte[] data,Camera camera,int index) {
            mData = data;
            mBitmapOutput = new ByteArrayOutputStream();
            mMessage = mHandler.obtainMessage();
            mMatrix = new Matrix();
                switch (mOrienta){
                    case  90:
                        mMatrix.postRotate(270);
                        break;
                    case 270:
                        mMatrix.postRotate(90);
                        break;
                    default:
                        mMatrix.postRotate(mOrienta);
                        break;
                }
           // mMatrix.postScale(-1, 1);//水平
            mtCamera = camera;
            this.index = index;
        }
        @Override
        public void run() {
            Bitmap bitmap =null;
            Bitmap mFaceBitmap=null;
            long startTime =System.currentTimeMillis(),endTime = 0;
            String logMsg = " ";
            int type = -1;
            try{
                Camera.Size size = mtCamera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(mData, ImageFormat.NV21, size.width, size.height, null);
                mData = null;
                yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, mBitmapOutput);
                BitmapFactory.Options options = new BitmapFactory.Options();
//               options.inSampleSize =2;
                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
                // 转换成图片
                bitmap = BitmapFactory.decodeByteArray(mBitmapOutput.toByteArray(), 0, mBitmapOutput.toByteArray().length, options);
                if(bitmap!=null){
                    mBitmapOutput.reset();
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix, false);
                    final Bitmap mBitmap=bitmap;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImg.setImageBitmap(mBitmap);
                        }
                    });
                    endTime=System.currentTimeMillis();
                    logMsg = "识别人脸前耗时时间："+(endTime-startTime);
                    FaceDetector.Face[] faces = mFaceHelper.findFaces(bitmap);
                    logMsg = logMsg + ",==识别人脸时间:"+(System.currentTimeMillis()-endTime)+",mOrienta:"+mOrienta+",w:"+mBitmap.getWidth()+",h:"+mBitmap.getHeight()+",degrees:"+degrees;//width:"+mBitMap.getWidth()+",height:"+mBitMap.getHeight()+",
                    Logger.i(TAG+logMsg);
                    FaceDetector.Face facePostion = null;
                    int index = 0;
                    if(faces!=null){
                        for (FaceDetector.Face face : faces) {
                            if (face == null) {
                                bitmap.recycle();
                                bitmap = null;
                                mBitmapOutput.close();
                                mBitmapOutput = null;
                                Logger.e("无人脸");
                                type = 0;
                                break;
                            } else {
                                Logger.e("有人脸");
                                facePostion = face;
                                type = 1;
                                break;
                            }
                        }
                    }
                    Message message = mHandler.obtainMessage(1);
                    Bundle bun = new Bundle(   );
                    bun.putInt("type",type);
                    bun.putString("msg",logMsg);
                    message.setData(bun);
                    mHandler.sendMessage(message);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally{
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                if (mFaceBitmap != null) {
                    mFaceBitmap.recycle();
                    mFaceBitmap = null;
                }
                if (mBitmapOutput != null ) {
                    try {
                        mBitmapOutput.close();
                        mBitmapOutput = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isFirst=true;
        if (mFaceHandleThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mFaceHandleThread.quitSafely();
            }
            try {
                mFaceHandleThread.join();
                mFaceHandleThread = null;
                mFaceHandle = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private ImageView mImg;
    private   int mOrienta =0,index;
    private Button xz0,xz90,xz180,xz270,startCaream,openCaremaSize;
    private Handler mFaceHandle;
    private HandlerThread mFaceHandleThread;
    private FaceHelper mFaceHelper;
    private String TAG="CameraAct:";
    private SurfaceTexture mSurface;
    private EditText sizeIndex;
    private Camera mCamera;
    private TextView faceStatus,log,rotation,caremaSizeText;
    private AutoFitTextureView textureView;
    private Context mContext;
    private long time;
    private boolean isFirst=true,xzzd=false;
    private boolean isExpend = false;
}
