package com.example.acer.webview;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import static java.security.AccessController.getContext;

public class BackCameraActivity extends Activity implements SensorEventListener {

    private static FileOutputStream outStream;

    //----degree
    static final float ALPHA = 0.1f; //低通濾波器之閥值, if ALPHA = 1 OR 0, no filter applies.
    int currentLocateH = 0;
    int currentLocateW = 0;
    private float currentDegree = 0f;
    private SensorManager mSensorManager;
    TextView text_degree;
    float[] mGravity;
    float[] mGeomagnetic;
    float[] mOrientation;
    float Rotation[] = new float[9];
    float[] degree = new float[3];
    float[] ort = new float[3];
    //----
    int MAX_ANGLE = 30;
    private int times=0;
    private Camera mCamera;
    private CameraPreview mPreview;
    Button captureButton;
    public static final int MEDIA_TYPE_IMAGE = 1;
    static Context con;
    TextView text_value;
    MyView_back show;
    int height;
    int width;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //---- no action bar
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_back_camera);
        //---- no action bar

        //----findViewById
        captureButton = (Button) findViewById(R.id.button_snap);
        FrameLayout preview = (FrameLayout) findViewById(R.id.mPreview);
        show = (MyView_back) findViewById(R.id.show_back);
        text_value = (TextView) findViewById(R.id.text_valueOfG);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //----findViewById

        //----找出螢幕的長寬
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        Log.d("abc",width+"w"+height+"h");
        //----找出螢幕的長寬


        //----Create an instance of Camera
        con = getApplicationContext();
        try {
            mCamera = getCameraInstance();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //----Create an instance of Camera

        //----Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);

        preview.addView(mPreview);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get an image from the camera
                mSensorManager.registerListener(BackCameraActivity.this,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
            }
        });
    }

    Bitmap bitmap;
    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            System.gc();
            bitmap = null;
            BitmapWorkerTask task = new BitmapWorkerTask(data);
            task.execute(0);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        // 获取触发event的传感器类型
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ORIENTATION:
                // 获取与Y轴的夹角
                float yAngle = -values[2];
                // 获取与Z轴的夹角
                float zAngle = values[1];
                String toast = String.format("Y:%.1f+  Z:%.1f",yAngle,zAngle);
                text_value.setText(toast);

                // 气泡位于中间时（水平仪完全水平），气泡的X、Y座标
                int x = (show.back.getWidth() - show.bubble.getWidth()) / 2;

                int y = (show.back.getHeight() - show.bubble.getWidth())/2;
                // 如果与Z轴的倾斜角还在最大角度之内
                if (Math.abs(zAngle) <= MAX_ANGLE) {
                    // 根据与Z轴的倾斜角度计算X座标的变化值（倾斜角度越大，X座标变化越大）
                    int deltaX = (int) ((show.back.getWidth() - show.bubble
                            .getWidth()) / 2 * zAngle / MAX_ANGLE);
                    x -= deltaX;
                }
                // 如果与Z轴的倾斜角已经大于MAX_ANGLE，气泡应到最左边
                else if (zAngle > MAX_ANGLE) {
                    x = 0;
                }
                // 如果与Z轴的倾斜角已经小于负的MAX_ANGLE，气泡应到最右边
                else {
                    x = show.back.getWidth() - show.bubble.getWidth();
                }
                // 如果与Y轴的倾斜角还在最大角度之内
                if (Math.abs(yAngle) <= MAX_ANGLE) {
                    // 根据与Y轴的倾斜角度计算Y座标的变化值（倾斜角度越大，Y座标变化越大）
                    int deltaY = (int) ((show.back.getHeight() - show.bubble
                            .getHeight()) / 2 * yAngle / MAX_ANGLE);
                    y += deltaY;
                }
                // 如果与Y轴的倾斜角已经大于MAX_ANGLE，气泡应到最下边
                else if (yAngle > MAX_ANGLE) {
                    y = show.back.getHeight() - show.bubble.getHeight();
                }
                // 如果与Y轴的倾斜角已经小于负的MAX_ANGLE，气泡应到最右边
                else {
                    y = 0;
                }
                // 如果计算出来的X、Y座标还位于水平仪的仪表盘内，更新水平仪的气泡座标
                if (isContain(x, y)) {
                    show.bubbleX = y;
                    show.bubbleY = x;
                    Log.d("lct",y+"y"+x+"x");
                }
                // 通知系统重回MyView组件
                show.postInvalidate();
                break;
        }
        float yAngle = -values[2];
        float zAngle = values[1];
        if(Math.abs(yAngle-0)<=0.1  && Math.abs(zAngle-0)<=0.1) { //take photo
            mSensorManager.unregisterListener(this);
            if (mCamera != null)
                mCamera.takePicture(null, null, mPicture);

        }
    }
    private boolean isContain(int x, int y) {
        // 计算气泡的圆心座标X、Y
        int bubbleCx = x + show.bubble.getWidth() / 2;
        int bubbleCy = y + show.bubble.getWidth() / 2;
        // 计算水平仪仪表盘的圆心座标X、Y
        int backCx = show.back.getWidth() / 2;
        int backCy = show.back.getWidth() / 2;
        // 计算气泡的圆心与水平仪仪表盘的圆心之间的距离。
        double distance = Math.sqrt((bubbleCx - backCx) * (bubbleCx - backCx)
                + (bubbleCy - backCy) * (bubbleCy - backCy));
        // 若两个圆心的距离小于它们的半径差，即可认为处于该点的气泡依然位于仪表盘内
        if (distance < (show.back.getWidth() - show.bubble.getWidth()) / 2) {
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    protected float[] lowPass( float[] input, float[] output ) { //低通濾波器
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<byte[]> dataf;
        private int data = 0;

        public BitmapWorkerTask(byte[] imgdata) {
            // Use a WeakReference to ensure the ImageView can be garbage
            // collected
            dataf = new WeakReference<byte[]>(imgdata);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            ResultActivity(dataf.get());
            return mainbitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mainbitmap != null) {

                Intent i = new Intent();
                i.putExtra("BitmapImage", mainbitmap);
                setResult(-1, i);
                // Here I am Setting the Requestcode 1, you can put according to
                // your requirement
                finish();
            }
        }
    }

    Bitmap mainbitmap;

    public void ResultActivity(byte[] data) {
        try {
            outStream = new FileOutputStream(String.format("/sdcard/%d.jpg",
                    System.currentTimeMillis()) );
            outStream.write(data);
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainbitmap = null;
        mainbitmap = decodeSampledBitmapFromResource(data, 200, 200);
        mainbitmap=RotateBitmap(mainbitmap,270);
        mainbitmap=flip(mainbitmap);
    }

    public static Bitmap decodeSampledBitmapFromResource(byte[] data,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // BitmapFactory.decodeResource(res, resId, options);
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        Log.d("No of cameras", Camera.getNumberOfCameras() + "");
        for (int camNo = 0; camNo < Camera.getNumberOfCameras(); camNo++) {
            CameraInfo camInfo = new CameraInfo();
            Camera.getCameraInfo(camNo, camInfo);

            if (camInfo.facing == (CameraInfo.CAMERA_FACING_BACK)) {
                c = Camera.open(camNo);
                c.setDisplayOrientation(90);
            }
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera(); // release the camera immediately on pause event
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        //----degree
        Sensor accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_STATUS_ACCURACY_LOW);
        mSensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_STATUS_ACCURACY_LOW );
        //----degree
        // TODO Auto-generated method stub
        super.onResume();

        if (mCamera == null) {
            setContentView(R.layout.activity_camera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

            // Create an instance of Camera
            con = getApplicationContext();
            try {
                mCamera = getCameraInstance();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Create our Preview view and set it as the content of our
            // activity.
            mPreview = new CameraPreview(this, mCamera);

            preview.addView(mPreview);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    // rotate the bitmap to portrait
    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(),
                source.getHeight(), matrix, true);
    }

    //the front camera displays the mirror image, we should flip it to its original
    Bitmap flip(Bitmap d)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap src = d;
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return dst;
    }


}