/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.facedetector;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.airbnb.lottie.L;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.facedetector.customview.OverlayView;
import com.example.facedetector.customview.OverlayView.DrawCallback;
import com.example.facedetector.env.BorderedText;
import com.example.facedetector.env.ImageUtils;
import com.example.facedetector.env.Logger;
import com.example.facedetector.tflite.Detector;
import com.example.facedetector.tflite.TFLiteObjectDetectionAPIModel;
import com.example.facedetector.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class testingDetecting extends com.example.facedetector.CameraActivity1 implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    public int Count=0;
    public int Count2=0;
    public int C =0;
    public int C1 =0;
    public int f = 0;
    public int f2 = 0;

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "book.tflite"; //이어폰감지
    private static final String TF_OD_API_LABELS_FILE = "labelmaptest.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Detector detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;


    private  String cheatnum;
    private TextView textView;

    private TextToSpeech tts;



    private final String accessKey = "";
    private final String secretKey = "";

    String userPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing_detecting);
        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");




        Button btn_finish = findViewById(R.id.btn_finish);
        textView = findViewById (R.id.textView);

        String conversionTime = "000010"; //countdown 30초 설정
        countDown(conversionTime); // 카운트다운 시작하기



        btn_finish.setOnClickListener(new View.OnClickListener() { 
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert_ex = new AlertDialog.Builder(testingDetecting.this);
                alert_ex.setMessage("시험을 종료하시겠습니까?");

                alert_ex.setPositiveButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alert_ex.setNegativeButton("종료", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(testingDetecting.this, QuitActivity.class);
                        intent.putExtra("userPass",userPass); //수험번호
                        startActivity(intent);
                        overridePendingTransition(R.anim.in_right, R.anim.out_left);
                    }
                });
                alert_ex.setTitle("시험종료");
                AlertDialog alert = alert_ex.create();
                alert.show();
            }
        });
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() { //tts 생성 및 초기화
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //사용할 언어를 설정
                    int result = tts.setLanguage(Locale.KOREA);
                    //언어 데이터가 없거나 혹은 언어가 지원하지 않으면...
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(testingDetecting.this, "이 언어는 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
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

    public void countDown(String time) {

        long conversionTime = 0;

        // 1000 단위가 1초
        // 60000 단위가 1분
        // 60000 * 3600 = 1시간

        String getHour = time.substring(0, 2);
        String getMin = time.substring(2, 4);
        String getSecond = time.substring(4, 6);

        // "00"이 아니고, 첫번째 자리가 0 이면 제거
        if (getHour.substring(0, 1) == "0") {
            getHour = getHour.substring(1, 2);
        }

        if (getMin.substring(0, 1) == "0") {
            getMin = getMin.substring(1, 2);
        }

        if (getSecond.substring(0, 1) == "0") {
            getSecond = getSecond.substring(1, 2);
        }

        // 변환시간
        conversionTime = Long.valueOf(getHour) * 1000 * 3600 + Long.valueOf(getMin) * 60 * 1000 + Long.valueOf(getSecond) * 1000;

        // 첫번쨰 인자 : 원하는 시간 (예를들어 30초면 30 x 1000(주기))
        // 두번쨰 인자 : 주기( 1000 = 1초)
        new CountDownTimer(conversionTime, 1000) {

            // 특정 시간마다 뷰 변경
            public void onTick(long millisUntilFinished) {

                // 시간단위
                String hour = String.valueOf(millisUntilFinished / (60 * 60 * 1000));

                // 분단위
                long getMin = millisUntilFinished - (millisUntilFinished / (60 * 60 * 1000)) ;
                String min = String.valueOf(getMin / (60 * 1000)); // 몫

                // 초단위
                String second = String.valueOf((getMin % (60 * 1000)) / 1000); // 나머지

                // 밀리세컨드 단위
                String millis = String.valueOf((getMin % (60 * 1000)) % 1000); // 몫

                // 시간이 한자리면 0을 붙인다
                if (hour.length() == 1) {
                    hour = "0" + hour;
                }

                // 분이 한자리면 0을 붙인다
                if (min.length() == 1) {
                    min = "0" + min;
                }

                // 초가 한자리면 0을 붙인다
                if (second.length() == 1) {
                    second = "0" + second;
                }

                textView.setText(hour + ":" + min + ":" + second);
            }

            // 제한시간 종료시
            public void onFinish() {

                // 변경 후
                textView.setText("00:00:00");

                Speechfinish();


              //  Intent intent = new Intent(testingDetecting.this, QuitActivity.class);
               // intent.putExtra("userPass",userPass); //수험번호
               // startActivity(intent);
               // overridePendingTransition(R.anim.in_right, R.anim.out_left);


            }
        }.start();
    }
    public void Speech(){
        String text = "부정행위가 감지되었습니다.";
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
    public void Speechfinish(){
        String text = "시험이 종료되었습니다.";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH,null,null);
        else
            tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }



    //감지된 이미지 저장하기.
    public void saveImage(Bitmap bitmap){

        try {
            File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
            String timeStemp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Intent data = getIntent();
            userPass = data.getStringExtra("userPass");
            String imageFileName = userPass+"_"+cheatnum+"_"+timeStemp+".jpg";
            path.mkdirs();
            Log.d("save", "path 생성");

            File file = new File(path, imageFileName);
            FileOutputStream out = new FileOutputStream(file);

            Log.d("save", "FileOutputStream에 저장");


            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Log.d("save", "JPEG 파일 만들기");

            //aws로 이미지 업로드 하기
            onAWS(imageFileName,file);

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));

            sendBroadcast(mediaScanIntent);
            Log.d("save", "겔러리에 저장하기");
            out.close();
        }catch (Exception e)
        { e.printStackTrace();
        }
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
                Log.d("AWS", "onStateChanged: " + id + ", " + state.toString());

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                Log.d("AWS", "ID:" + id + " bytesCurrent: " + bytesCurrent + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("AWS", ex.getMessage());
            }
        });
    }




    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            this,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing Detector!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {//이미지 처리
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();


        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();


                        final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);

                        Log.d("결과값", String.valueOf(results)); // 리스트 0번 빼오기
                        //리스트 0~1번 title, confidence값 받아오기
                        float result0 = results.get(0).getConfidence()*100;
                        float result1 = results.get(1).getConfidence()*100;
                        float result2 = results.get(2).getConfidence()*100;
                        String title0 = results.get(0).getTitle();
                        String title1 = results.get(1).getTitle();
                        String title2 = results.get(2).getTitle();



                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f); //roi 부분 준비

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Detector.Recognition> mappedRecognitions =
                                new ArrayList<Detector.Recognition>();

                        for (final Detector.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {

                                canvas.drawRect(location, paint); //rect 그리기


                                cropToFrameTransform.mapRect(location);//결과 이미지 ROIz

                                //title case별로 조건문 부정행위 조건문
                                switch (title0){
                                    case "book": //인식률이 가장 높은것에 book이 되는 경우 부정행위로 업로드
                                        if(result0 >= 80){
                                            Count2 = Count2+1;
                                            if(Count2 == 5){
                                                cheatnum = "5";
                                                saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                Speech();
                                            }
                                        }
                                        break;
                                    case "left": //왼손이 1번째로 인식되는 경우
                                        if(result0 >= 80){
                                           if(title1.equals("book") && result1 >= 70) { //책이 잡히는 경우
                                               C = C + 1;
                                               if (C == 4) {
                                                   cheatnum = "5";
                                                   saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                   Speech();
                                               }
                                           }
                                           else if(title2.equals("book")&& result2 >= 60){ // 책이 잡히는 경우
                                               C1 = C1 + 1;
                                               if (C == 4) {
                                                   cheatnum = "5";
                                                   saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                   Speech();
                                               }
                                           }
                                           else if(title1.equals("right") && result1 < 40 ) {//오른손이 잡히지 않는 경우
                                                   f = f + 1;
                                                   if (f == 4) {
                                                       cheatnum = "4";
                                                       saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                       Speech();
                                                   }
                                               }
                                           else if(title2.equals("right") && result2 < 40){ //오른손이 잡히지 않는 경우
                                                   f = f + 1;
                                                   if (f == 4) {
                                                       cheatnum = "4";
                                                       saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                       Speech();
                                                   }

                                               }}
                                        break;
                                    case "right": //오른쪽이 최대 인식률일 때
                                        if(result0 >= 80){
                                            if(title1.equals("book") && result1 >= 70) { //책이 잡히는 경우
                                                C = C + 1;
                                                if (C == 4) {
                                                    cheatnum = "5";
                                                    saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                    Speech();
                                                }
                                            }
                                            else if(title2.equals("book")&& result2 >= 60){ // 책이 잡히는 경우
                                                C1 = C1 + 1;
                                                if (C == 4) {
                                                    cheatnum = "5";
                                                    saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                    Speech();
                                                }
                                            }

                                            else if(title1.equals("left") && result1 < 40 ) {//왼손이 잡히지 않는 경우
                                                f = f + 1;
                                                if (f == 4) {
                                                    cheatnum = "4";
                                                    saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                    Speech();
                                                }
                                            }
                                            else if(title2.equals("left") && result2 < 40){ //왼손이 잡히지 않는 경우
                                                f2 = f2 + 1;
                                                if (f2 == 4) {
                                                    cheatnum = "4";
                                                    saveImage(cropCopyBitmap); //부정행위 이미지 업로드
                                                    Speech();
                                                }
                                            }}
                                        break;
                                    default: break; //아무것도 잡히지 않을 때

                                           }
                                result.setLocation(location);
                                mappedRecognitions.add(result);


                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);

                        trackingOverlay.postInvalidate();

                        computingDetection = false;


                    }
                });

    }



    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(
                () -> {
                    try {
                        detector.setUseNNAPI(isChecked);
                    } catch (UnsupportedOperationException e) {
                        LOGGER.e(e, "Failed to set \"Use NNAPI\".");
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                });
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(
                () -> {
                    try {
                        detector.setNumThreads(numThreads);
                    } catch (IllegalArgumentException e) {
                        LOGGER.e(e, "Failed to set multithreads.");
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                });
    }
}
