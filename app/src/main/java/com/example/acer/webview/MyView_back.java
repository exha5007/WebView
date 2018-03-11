package com.example.acer.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class MyView_back extends View {
    // 定义水平仪仪表盘图片
    Bitmap back;
    // 定义水平仪中的气泡图标
    Bitmap bubble;
    // 定义水平仪中气泡 的X、Y座标
    int bubbleX, bubbleY;
    static int AbsCenterX;
    static int AbsCenterY;
    static int CenterX;
    static int CenterY;
    @SuppressLint("NewApi")
    public MyView_back(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 加载水平仪图片和气泡图片
        back = BitmapFactory.decodeResource(getResources(), R.drawable.back);
        back = back.createScaledBitmap(back,600,600,false);
        bubble = BitmapFactory
                .decodeResource(getResources(), R.drawable.bubble);
        bubble = bubble.createScaledBitmap(bubble,25,25,false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制水平仪表盘图片

        AbsCenterX = (getRight()/2)-back.getWidth()/2;
        AbsCenterY = (getBottom()/2)-back.getHeight()/2;

        //canvas.drawBitmap(back,(getRight()/2)-back.getWidth()/2,(getBottom()/2)-back.getHeight()/2, null);
        canvas.drawBitmap(back,AbsCenterX,AbsCenterY, null); //0,0 ==>AbsCenterX,AbsCenterY
        // 根据气泡座标绘制气泡
        canvas.drawBitmap(bubble, bubbleX+AbsCenterX, bubbleY+AbsCenterY, null);// 將bubble的座標從基礎的(0+bubbleX,0+bubbleY) ==>(AbsCenterX+bubbleX,AbsCenterY+bubbleY)
    }

}