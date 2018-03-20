package com.example.acer.webview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;

import static com.example.acer.webview.MainActivity.bitmapFrontCam;
import static com.example.acer.webview.MainActivity.currentFileName;

public class ConfirmActivity extends Activity {

    ImageView imageView;
    Button btn_confirm;
    Button btn_redo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        File imgFile = new  File(currentFileName);

        btn_confirm = (Button) findViewById(R.id.btn_confirm);
        btn_redo = (Button) findViewById(R.id.btn_redo);
        btn_redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File file = new File(currentFileName);
                boolean deleted = file.delete();
                finish();

            }
        });

        Log.d("TAG",currentFileName);
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            ImageView myImage = (ImageView) findViewById(R.id.confirm_image);
            myImage.setImageBitmap(myBitmap);
            myImage.setRotation(90);
        }

    }

}
