package com.example.acer.webview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class MyView extends View {
    Bitmap mark;
    // 定义 bar
    int bubbleX=0, bubbleY; //
    static int AbsCenterX;
    static int AbsCenterY;
    static int parentW;
    static int parentH;
    static int CenterX;
    static int CenterY;
    @SuppressLint("NewApi")
    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 加载水平仪图片和气泡图片
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);
        parentH = displayMetrics.heightPixels;
        parentW = displayMetrics.widthPixels;
        mark = BitmapFactory.decodeResource(getResources(), R.drawable.colorless);
        mark = mark.createScaledBitmap(mark,parentW,parentH,false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制水平仪表盘图片


        //canvas.drawBitmap(back,(getRight()/2)-back.getWidth()/2,(getBottom()/2)-back.getHeight()/2, null);
        canvas.drawBitmap(mark,bubbleX,bubbleY, null); //0,0 ==>AbsCenterX,AbsCenterY

    }
}