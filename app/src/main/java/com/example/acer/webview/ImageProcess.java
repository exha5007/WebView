package com.example.acer.webview;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.acer.webview.MainActivity.currentFileName;
import static org.opencv.imgproc.Imgproc.contourArea;

public class ImageProcess extends Activity {
    ImageView igv;
    ImageView loading;
    Mat imgRGBA, imgBGA,imgHSV,matrix1,matrix2,matrix3,matrix4;
    double top_left_m=0,top_left_n=0,top_right_m=0,top_right_n=0,
    bottom_left_m=0,bottom_left_n=0,bottom_right_m=0,bottom_right_n=0;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Log.d("LoadImageFromFile:", path.toString());
                    String filename = currentFileName;
                    imgBGA = Imgcodecs.imread(filename);

                    Utils.bitmapToMat(getZippedBitmap(currentFileName),imgBGA);
                    if (imgBGA.empty()) {
                        Toast.makeText(ImageProcess.this, filename, Toast.LENGTH_SHORT).show();
                    } else {

                        igv = (ImageView) findViewById(R.id.im_pocessed);
                        loading = (ImageView) findViewById(R.id.im_loading);
                        igv.setVisibility(View.INVISIBLE);
                        //壓縮圖片 3.縮放並壓縮
                        imgHSV = imgBGA.clone();
                        Imgproc.cvtColor(imgHSV,imgHSV,Imgproc.COLOR_RGB2HSV,3);
                        List<Mat> hsv = new ArrayList<Mat>(3);
                        Core.split(imgHSV,hsv);

                        imgHSV = hsv.get(0).clone();// hsv_s
                        imgHSV = matBinarized(imgHSV); //二值化
                        imgHSV = invGrey(imgHSV); //黑白互換
                        imgHSV = enhanceBright(imgHSV); //增強亮度
                        imgHSV = ExtractNLargestBlobs(imgHSV); //取最大物件
                        Log.d("AAAAA",EthanOCV_Utils.calculateConvexHull(EthanOCV_Utils.getContours(imgHSV).get(0))[0].toString());
                        Log.d("AAAAA",EthanOCV_Utils.calculateConvexHull(EthanOCV_Utils.getContours(imgHSV).get(0))[1].toString());
                        Log.d("AAAAA",EthanOCV_Utils.calculateConvexHull(EthanOCV_Utils.getContours(imgHSV).get(0))[2].toString());
                        Log.d("AAAAA",EthanOCV_Utils.calculateConvexHull(EthanOCV_Utils.getContours(imgHSV).get(0))[3].toString());
                        imgRGBA = removeNoiseGaussianBlur(imgBGA); //Gaussian濾波
                        imgRGBA = grayImage(imgRGBA); //灰階
                        imgRGBA = sobelEdge(imgRGBA); //sobel edge
                        Bitmap bm = Bitmap.createBitmap(imgHSV.cols(), imgHSV.rows(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(imgRGBA, bm);
/*
                        Bitmap bm = Bitmap.createBitmap(imgRGBA.cols(), imgRGBA.rows(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(imgRGBA, bm);

                        */
                        igv.setImageBitmap(bm);
                        igv.setRotation(90);
                        saveBitmapToFile(bm);
                        igv.setVisibility(View.VISIBLE);
                        loading.setVisibility(View.INVISIBLE);
                    }

                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);

    }

    public static Mat enhanceBright(Mat mat) {
        int rows = mat.rows();
        int cols = mat.cols();
        int ch = mat.channels();
        double[] data = {};
        for(int i=0;i <rows;i++) {
            for(int j=0;j <cols;j++) {
                for(int k=0;k <ch;k++) {
                    data = mat.get(i,j);
                    data[k] = data[k] *1.5;
                }
                mat.put(i,j,data);
            }
        }
        return mat;
    }




    public static Mat invGrey(Mat mat) {
        int rows = mat.rows();
        int cols = mat.cols();
        int ch = mat.channels();

        for(int i=0;i <rows;i++) {
            for(int j=0;j <cols;j++) {
                double[] data = mat.get(i,j);
                for(int k=0;k <ch;k++) {
                    data[k] = 255 - data[k] ;
                }
                mat.put(i,j,data);
            }
        }
        return mat;
    }

    public static Mat sobelEdge(Mat srcMat) {
        Mat xDervative=new Mat();
        Mat yDervative=new Mat();
        /*一般输入图片的depth与输出图片的depth值相同，但是当我们一阶导数的时候会出现负数-255—255，使用unsignded 8-bit depth只能包含0-255，因此采用了16-bit*/
        int ddepth= CvType.CV_16S;
        //计算x,y的一阶导数，参数1，0来设置x，y轴
        Imgproc.Sobel(srcMat,xDervative,ddepth,1,0);
        Imgproc.Sobel(srcMat,yDervative,ddepth,0,1);
        Mat absXD=new Mat();
        Mat absYD=new Mat();
        //Mat转换
        Core.convertScaleAbs(xDervative,absXD);
        Core.convertScaleAbs(yDervative,absYD);
        //根号（x*x+y*y）
        Mat edgeImage=new Mat();
        Core.addWeighted(absXD,0.5,absYD,0.5,0,edgeImage);
        return edgeImage;
    }

    public static Mat cannyEdge(Mat srcMat){
        final Mat edgeImage=new Mat();
        Imgproc.Canny(srcMat,edgeImage,100,200);
        return edgeImage;
        //100低阈值 200：高阈值
    }/*
    public Mat filter(final Mat dist) {
    }*/
    public static Mat grayImage(Mat srcMat){
        Mat grayImg=new Mat();
        Imgproc.cvtColor(srcMat,grayImg,Imgproc.COLOR_RGB2GRAY);
        return grayImg;
    }

    public static Mat ExtractNLargestBlobs(Mat mat) {
        //Size size = new Size(3,3);
        //Imgproc.blur(mat,mat,size);
        //mat = sobelEdge(mat);
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();

        List<MatOfInt> hull = new ArrayList<MatOfInt>(contours.size());
        Imgproc.findContours(mat,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        Mat contourImg = new Mat(mat.size(), mat.type());
        int temp=0;
        int flag=0;
        for (int i = 0; i < contours.size(); i++) {
            Integer value = Integer.valueOf(contours.get(i).size().toString().replace("1x",""));
            if(value > temp) {
                flag = i;
                temp = value;
            }
        }
        Imgproc.drawContours(contourImg, contours, flag,new Scalar(255,255,255), -2);
        return contourImg;
    }


    public static Mat removeNoiseGaussianBlur(Mat srcMat){
        final Mat blurredImage=new Mat();
        Size size=new Size(7,7);
        Imgproc.GaussianBlur(srcMat,blurredImage,size,0,0);
        return blurredImage;
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, //計算圖片的縮放值
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
    public static Bitmap getSmallBitmap(String filePath) {  //根據路徑獲得圖片並壓縮，回傳bitmap用於顯示
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 480, 800);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
    public static String bitmapToString(String filePath) { //把bitmap 轉換成 string

        Bitmap bm = getSmallBitmap(filePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
    public static Bitmap getZippedBitmap(String filename) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentFileName,options);
        options.inSampleSize = calculateInSampleSize(options,240,400);
        options.inJustDecodeBounds = false;
        Bitmap zipped_bitmap = BitmapFactory.decodeFile(currentFileName,options);
        return zipped_bitmap;
    }
    public static Mat matBinarized(Mat mat) {
        Mat mat_bi = new Mat(mat.rows(),mat.cols(),CvType.CV_8UC1);
        Imgproc.threshold(mat,mat_bi,0,255,Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);
        return mat_bi;
    }

    public static void saveBitmapToFile(Bitmap bmp){
        try {
            // 取得外部儲存裝置路徑
            String path = Environment.getExternalStorageDirectory().toString ();
            // 開啟檔案
            String nowTime = String.format("/sdcard/%d.jpg",
                    System.currentTimeMillis());
            File file = new File( nowTime);
            // 開啟檔案串流
            FileOutputStream out = new FileOutputStream(file );
            // 將 Bitmap壓縮成指定格式的圖片並寫入檔案串流
            bmp.compress ( Bitmap. CompressFormat.PNG , 90 , out);
            // 刷新並關閉檔案串流
            out.flush ();
            out.close ();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            //Internal OpenCV library not found. Using OpenCV Manager for initialization
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            //OpenCV library found inside package. Using it!
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}