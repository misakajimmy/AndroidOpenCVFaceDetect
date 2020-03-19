package com.misakajimmy;

import android.app.Activity;

import android.content.Context;
import android.content.pm.PackageManager;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.FaceDetect;
import org.opencv.samples.facedetect.FdActivity;

import java.io.File;

import java.io.IOException;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_COMPLEX;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    private static String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA"
    };
    FaceDetect faceDetect = new FaceDetect();


    private int mCameraIndexCount = 0;


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.w("MainActivity", "初始化失败");
        } else {
            Log.w("MainActivity", "初始化success");
        }
    }


    private CameraBridgeViewBase mCVCamera;
    //缓存相机每帧输入的数据
    private Mat mRgba;
    private Mat mGray;
    private Button button;

    private int timeStamp1 = getSecondTimestampTwo(new Date());
    private int timeStamp2 = getSecondTimestampTwo(new Date());

    /**
     * 通过OpenCV管理Android服务，初始化OpenCV
     **/
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mCVCamera.enableView();
                }
                break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        verifyStoragePermissions(this);
        verifyCameraPermissions(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_swap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCVCamera.disableView();
                mCameraIndexCount = mCameraIndexCount == 0 ? 1 : 0;
                playAssetsAudio(mCameraIndexCount == 0 ? "back_camera.mp3" : "front_camera.mp3");
                mCVCamera.setCameraIndex(mCameraIndexCount);
                mCVCamera.enableView();
            }
        });


        //初始化并设置预览部件
        mCVCamera = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCVCamera.setCvCameraViewListener(this);

        //拍照按键
        button = (Button) findViewById(R.id.deal_btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAssetsAudio("take_photo.mp3`");
                if (mRgba != null) {
                    if (!mRgba.empty()) {
                        Mat inter = new Mat(mRgba.width(), mRgba.height(), CvType.CV_8UC4);
                        Log.e("Mat", "...............1...............");
                        //将四通道的RGBA转为三通道的BGR，重要！！
                        Imgproc.cvtColor(mRgba, inter, Imgproc.COLOR_RGBA2BGR, 3);
                        Log.e("Mat", "...............2...............");
                        File sdDir = null;
                        //判断是否存在机身内存
                        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                        if (sdCardExist) {
                            //获得机身储存根目录
                            sdDir = Environment.getExternalStorageDirectory();
                            Log.e("Mat", "...............3...............");
                        }
                        //将拍摄准确时间作为文件名
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                        String filename = sdf.format(new Date());
                        String savepath = sdDir + "/Pictures/OpenCV/";
                        File f = new File(savepath);
                        Log.i(TAG, savepath);
                        if (!f.exists()) {
                            boolean res = f.mkdirs();
                            Log.i(TAG, "!exist" + res);
                        }
                        String filePath = sdDir + "/Pictures/OpenCV/" + filename + ".png";
                        Log.e("Mat", "..............." + filePath + "...............");
                        //将转化后的BGR矩阵内容写入到文件中
                        Imgcodecs.imwrite(filePath, inter);
                        Toast.makeText(MainActivity.this, "图片保存到: " + filePath, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onRemuse");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library not found!");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        faceDetect.onResume(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mCVCamera != null) {
            mCVCamera.disableView();
        }
        super.onDestroy();
    }

    //对象实例化及基本属性的设置，包括长度、宽度和图像类型标志
    public void onCameraViewStarted(int width, int height) {
        Log.e("Mat", "...............4...............");
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }


    /**
     * 图像处理都写在这里！！！
     **/
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = mCameraIndexCount == 0 ? inputFrame.rgba() : transposemRgba(inputFrame.rgba());  //一定要有！！！不然数据保存不进MAT中！！！
        mGray = mCameraIndexCount == 0 ? inputFrame.gray() : transposemGray(inputFrame.gray());  //一定要有！！！不然数据保存不进MAT中！！！

        mRgba = faceDetect.onCameraFrame(mRgba, mGray);

        Rect[] facesArray = faceDetect.getFaacesArray(mRgba, mGray);

        Log.i("size", String.valueOf(mRgba.width()));
//        Imgproc.rectangle(mRgba,
//                new Point(mRgba.width() / 8, mRgba.height() / 6),
//                new Point(mRgba.width() - mRgba.width() / 8, mRgba.height() - mRgba.height() / 6),
//                new Scalar(0, 0, 255),
//                3
//        );
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.putText(mRgba,
                    String.valueOf(facesArray[i].x + facesArray[i].width / 2) + ' ' + String.valueOf(facesArray[i].y + facesArray[i].height / 2),
                    new Point(facesArray[i].x + facesArray[i].width / 2, facesArray[i].y + facesArray[i].height / 2),
                    FONT_HERSHEY_COMPLEX,
                    2,
                    new Scalar(0, 0, 255), 4, 8
            );
            Log.i("size", String.valueOf(mRgba.width()) + ' ' + String.valueOf(mRgba.height()));
        }
        timeStamp2 = getSecondTimestampTwo(new Date());
        if (timeStamp2 - timeStamp1 >= 5) {
            timeStamp1 = timeStamp2;
            if (facesArray.length < 1) {
                playAssetsAudio("face404.mp3");
            } else {
                for (int i = 0; i < facesArray.length; i++) {
                    if (facesArray[i].x + facesArray[i].width / 2 < 240) {
                        playAssetsAudio("face_too_left.mp3");
                    } else if (facesArray[i].x + facesArray[i].width / 2 > (mRgba.width() - 240)) {
                        playAssetsAudio("face_too_right.mp3");
                    }else {
                        playAssetsAudio("nice_face.mp3");
                    }
                }
            }
        }
        //直接返回输入视频预览图的RGB数据并存放在Mat数据中
        Log.e("Mat", "...............5...............");
        return mRgba;
    }


    public Mat transposemRgba(Mat trmRgba) {
        Core.flip(trmRgba, trmRgba, 1);
        return trmRgba;
    }

    public Mat transposemGray(Mat trmGray) {
        Core.flip(trmGray, trmGray, 1);
        return trmGray;
    }

    //结束时释放
    @Override
    public void onCameraViewStopped() {
        Log.e("Mat", "...............6...............");
        mRgba.release();
        // mTmp.release();
    }

    //获取储存权限
    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取摄像头权限
    public static void verifyCameraPermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.CAMERA");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playAssetsAudio(final String name) {
        Log.d(TAG, "playAssetWordSound: try to play assets sound file. -> " + name);
        AssetFileDescriptor fd = null;
        try {
            MediaPlayer mediaPlayer;
            Log.v(TAG, "Looking in assets.");
            fd = this.getApplicationContext().getAssets().openFd(name);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d(TAG, "onPrepared: " + name);
                    mediaPlayer.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    Log.d(TAG, "onCompletion: " + name);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int i, int i1) {
                    mp.release();
                    return true;
                }
            });
        } catch (Exception e) {
            try {
                if (fd != null) {
                    fd.close();
                }
            } catch (Exception e1) {
                Log.e(TAG, "Exception close fd: ", e1);
            }
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e) {
                    Log.e(TAG, "Finally, close fd ", e);
                }
            }
        }
    }

    public static int getSecondTimestampTwo(Date date) {
        if (null == date) {
            return 0;
        }
        String timestamp = String.valueOf(date.getTime() / 1000);
        return Integer.valueOf(timestamp);
    }
}
