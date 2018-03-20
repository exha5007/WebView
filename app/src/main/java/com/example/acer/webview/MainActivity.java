package com.example.acer.webview;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera.CameraInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends ActionBarActivity {
    static Bitmap bitmapFrontCam;
    static String currentFileName;
    ImageView iv;
    //to call our own custom cam
    private final String PERMISSION_WRITE_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private final String PERMISSION_CAMERA = "android.permission.CAMERA";
    private final static int CAMERA_PIC_REQUEST1 = 0;
    Context con;
    Button btn_back;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.imageView1);
        btn_back = (Button) findViewById(R.id.back_camera);
        con=this;

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasPermission()) {
                    if(needCheckPermission()) {

                        return;
                    }
                }
                Intent intent = new Intent(MainActivity.this,BackCameraActivity.class);
                startActivityForResult(intent, CAMERA_PIC_REQUEST1);
            }
        });
    }
    private boolean needCheckPermission() {
        //MarshMallow(API-23)之後要在 Runtime 詢問權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {PERMISSION_WRITE_STORAGE,PERMISSION_CAMERA};
            int permsRequestCode = 200;
            requestPermissions(perms, permsRequestCode);
            return true;
        }
        return false;
    }private boolean hasPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return(ActivityCompat.checkSelfPermission(this, PERMISSION_WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200){
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(">>>", "取得授權，可以執行動作了");

                }
            }
        }
    }

    public void onClick(View view) {
        if (getFrontCameraId() == -1) {
            Toast.makeText(getApplicationContext(),
                    "Front Camera Not Detected", Toast.LENGTH_SHORT).show();
        } else {
            Intent cameraIntent = new Intent();
            cameraIntent.setClass(this, FrontCameraActivity.class);
            startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST1);

            // startActivity(new
            // Intent(MainActivity.this,FrontCameraActivity.class));
        }
    }

    int getFrontCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT)
                return i;
        }
        return -1; // No front-facing camera found
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST1) {
            if (resultCode == RESULT_OK) {
                try {
                    bitmapFrontCam = data
                            .getParcelableExtra("BitmapImage");

                } catch (Exception e) {
                }
                iv.setImageBitmap(bitmapFrontCam);
                Intent intent = new Intent(this,ImageProcess.class);
                startActivity(intent);
            }

        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Picture was not taken", Toast.LENGTH_SHORT)
                    .show();
        }
    }




}