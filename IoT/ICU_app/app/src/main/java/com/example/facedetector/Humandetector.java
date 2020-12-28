package com.example.facedetector;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;

import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.Toast;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;


import java.io.File;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.view.Gravity.CENTER;


public class Humandetector extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;
    private final Semaphore writeLock = new Semaphore(1);
    private CustomDialog customDialog;
    ImageButton stopbtn;
    String userPass;
    public int Count = 0;
    public int Count2 = 0;

    private TextToSpeech tts;



    private CameraBridgeViewBase mOpenCvCameraView;
    private final String accessKey = "";
    private final String secretKey = "";


    //public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native long loadCascade(String cascadeFileName );
    public native int detect(long cascadeClassifier_body,long cascadeClassifier_upperbody, long matAddrInput, long matAddrResult);

    public long cascadeClassifier_body = 0;
    public long cascadeClassifier_upperbody = 0;



    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }



    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;


        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }

    }

    private void read_cascade_file(){
        copyFile("haarcascade_fullbody.xml");
        copyFile("haarcascade_upperbody.xml");


        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_body = loadCascade( "haarcascade_fullbody.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_upperbody = loadCascade( "haarcascade_upperbody.xml");

    }
    //AWS 버킷 파일 업로드
    private void onSave() {
        Log.d("save", "saveImage 확인");
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        String timeStemp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = userPass+"_3_"+timeStemp+".jpg";
        path.mkdirs();
        Log.d("save", "path 생성");

        File file = new File(path, imageFileName);

        String filename = file.toString();
        Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGB, 4);
        boolean ret = Imgcodecs.imwrite(filename, matResult);

        if (ret) Log.d(TAG, "SUCESS");
        else Log.d(TAG, "FAIL");

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));

        sendBroadcast(mediaScanIntent);

       onAWS(imageFileName,file);

    }


    private void onAWS(String imageFileName, File file) {
        //AWS버킷
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.US_EAST_1));

        TransferUtility transferUtility = TransferUtility.builder().s3Client(s3Client).context(this).build();
        TransferNetworkLossHandler.getInstance(this);

        TransferObserver uploadObserver = transferUtility.upload("icu-tester-cheat-s3", imageFileName, file);
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: " + id + ", " + state.toString());

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        });
    }




    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_humandetector);




        Intent data = getIntent();
        userPass = data.getStringExtra("userPass");


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //사용할 언어를 설정
                    int result = tts.setLanguage(Locale.KOREA);
                    //언어 데이터가 없거나 혹은 언어가 지원하지 않으면...
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(Humandetector.this, "이 언어는 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        //음성 톤
                        tts.setPitch(0.7f);
                        //읽는 속도
                        tts.setSpeechRate(1.2f);
                    }
                }
            }
        });
    }
    public void Speech(){
        String text = "사람이 감지되었습니다.";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH,null,null);
        else
            tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }
    public void Speech1(){
        String text = "통과되었습니다.";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH,null,null);
        else
            tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }
    @Override
    public void onStop(){
        super.onStop();
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    public void showDlg() { //감지 경고
        CustomActivity dlg = new CustomActivity(this);

        dlg.callDialog0();

    }
    public void showDlg1() { // 다음 검사로 넘어가는 확인
        CustomActivity dlg = new CustomActivity(this);

        dlg.callDialog0();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();

        if (matResult == null)
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type()); //row = heights, cols = widths

        //3번 detecting이 되면
        Core.flip(matInput, matInput, 1);
        int ret = detect(cascadeClassifier_body,cascadeClassifier_upperbody ,matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        //if문으로 반환 값 기준으로 저장 함수 불러와서 사용
        if(ret >= 1) {

            Count = Count + 1; //detecting되는 횟수 Count
            if (Count  ==  5) {
                onSave();
                Speech();
                runOnUiThread(Humandetector.this::showDlg);
                Count = 0;


            }}

            else{
                Count2 = Count2 + 1;
                if(Count2 == 230){ //감지 안됬을 때 자동으로 넘어가게 하기.

                    Speech1();
                    runOnUiThread(Humandetector.this::showDlg1);

                }




        }
        return matResult;
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }
    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
                read_cascade_file();
            }
        }
    }
    @Override
   protected void onStart() {
        super.onStart();
       boolean havePermission = true;
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
       if (havePermission) {
            onCameraPermissionGranted();
        }
    }
    @Override
    @TargetApi(Build.VERSION_CODES.M)
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
       if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
               && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
           onCameraPermissionGranted();
        }else{
           showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

       }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( Humandetector.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
       builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
               requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
           }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

}

