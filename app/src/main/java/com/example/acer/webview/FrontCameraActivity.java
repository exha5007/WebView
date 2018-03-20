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
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.acer.webview.MainActivity.currentFileName;
import static java.security.AccessController.getContext;

public class FrontCameraActivity extends Activity implements SensorEventListener,KeyEvent.Callback {

    private static FileOutputStream outStream;

    private Animation mAnimationRight;
    private TextView mlblRightPhotoNum;
    //----degree
    static final float ALPHA = 0.3f; // if ALPHA = 1 OR 0, no filter applies.
    int currentLocateH = 0;
    int currentLocateW = 0;
    private float currentDegree = 0f;
    private SensorManager mSensorManager;

    float[] mGravity;
    float[] mGeomagnetic;
    float Rotation[] = new float[9];
    float[] degree = new float[3];
    //----
    private int times=0;
    private Camera mCamera;
    private CameraPreview mPreview;
    public static final int MEDIA_TYPE_IMAGE = 1;
    static Context con;
    MyView show;
    int height;
    int width;
    Sensor sensor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        show = (MyView) findViewById(R.id.show);
        //----degree宣告
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //test

        mAnimationRight = AnimationUtils.loadAnimation(this, R.anim.rotate_right);
        mAnimationRight.setFillAfter(true);

        mlblRightPhotoNum = (TextView) findViewById(R.id.text_right_degree);
        mlblRightPhotoNum.setAnimation(mAnimationRight);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         height = displayMetrics.heightPixels;
         width = displayMetrics.widthPixels;
        Log.d("abc",width+"w"+height+"h");
        //----
        // Create an instance of Camera
        con = getApplicationContext();
        try {
            mCamera = getCameraInstance();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);

        preview.addView(mPreview);

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
    public boolean onKeyDown (int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            times = 1;
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {


        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = lowPass(event.values.clone(),mGravity);
            //mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = lowPass(event.values.clone(),mGeomagnetic);
            //mGeomagnetic = event.values;
        }

        if (mGravity != null && mGeomagnetic != null) {

            SensorManager.getRotationMatrix(Rotation, null, mGravity,
                    mGeomagnetic);
            SensorManager.getOrientation(Rotation, degree);


            degree[0] = (float)Math.toDegrees(degree[0])+180;
            degree[1] = (float)Math.toDegrees(degree[1])+180;
            degree[2] = (float)Math.toDegrees(degree[2])+180;
            currentDegree = degree[0];

            mlblRightPhotoNum.setText((int)degree[2]+"");

            int[] reAngleArray = getReSetAngle();
            currentLocateH = (int)generateArray(height)[(int)currentDegree];

            currentLocateW = (int)generateArray(width)[reAngleArray[Math.round(degree[2])]];
            show.bubbleX = currentLocateW;
            show.postInvalidate();


            if(times == 1) {
                if(Math.abs(degree[2]-270) <= 0.1 || Math.abs(degree[2]-90) <=0.1) {
                    mSensorManager.unregisterListener(this);
                    mCamera.takePicture(null,null,mPicture);
                }
            }
        }
    }
    public float[] generateArray(int pixel) {
        float[] a = new float[360];
        a[0] = 0;
        for(int i=1;i<=359;i++) {
            a[i] = (pixel/359f)*i;
        }
        return a;
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
    public static int[] getReSetAngle() {
        int [] x;
        x =new int[360];
        for (int i=0;i<90;i++){
            x[i]=270-i;}
        for (int i=0;i<90;i++){
            x[i+90]=180-i;}
        for (int i=0;i<90;i++){
            x[i+180]=90+i;}
        for (int i=0;i<90;i++){
            x[i+270]=180+i;}
        return x;
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
            String currentTime = String.format("/sdcard/%d.jpg",
                    System.currentTimeMillis());
            outStream = new FileOutputStream(currentTime);
            currentFileName = currentTime;
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

            if (camInfo.facing == (Camera.CameraInfo.CAMERA_FACING_FRONT)) {
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
        super.onResume();
        Sensor accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME);

        //----degree
        // TODO Auto-generated method stub


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