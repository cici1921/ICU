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
import android.os.Environment;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class WatchDetectorActivity extends com.example.facedetector.CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    public int Count=0;
    public int Count2=0;

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "watch.tflite"; //이어폰감지
    private static final String TF_OD_API_LABELS_FILE = "labelmapwatch.txt";
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

    private float resultscore; //인식률 받아오기.

    private final String accessKey = "";
    private final String secretKey = "";


    private TextToSpeech tts;

    String userPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //사용할 언어를 설정
                    int result = tts.setLanguage(Locale.KOREA);
                    //언어 데이터가 없거나 혹은 언어가 지원하지 않으면...
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(WatchDetectorActivity.this, "이 언어는 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
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
        String text = "스마트워치가 감지되었습니다.";
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



    //감지된 이미지 저장하기.
    public void saveImage(Bitmap bitmap){

        try {
            File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
            String timeStemp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Intent data = getIntent();
            userPass = data.getStringExtra("userPass");
            String imageFileName = userPass+"_2_"+timeStemp+".jpg";
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

    public void showDlg() { //경고
        CustomDialog5 dlg = new CustomDialog5(this);

        dlg.callDialog5();

    }
    public void showDlg1() {    //확인
        CustomDialog6 dlg = new CustomDialog6(this);

        dlg.callDialog6();

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

                        resultscore = results.get(0).getConfidence()*100; //리스트 0번째 인식률 받아오기

                        Log.d("결과 확인", String.valueOf(resultscore)); // 리스트 0번 빼오기

                        if(resultscore < 50){
                            Count = Count + 1;
                            Log.d("Count", String.valueOf(Count));

                            if(Count == 150) {
                                Speech1();
                                runOnUiThread(WatchDetectorActivity.this::showDlg1);

                            }

                        }







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


                                //95% 이상 디텍팅 됬을 때, 이미지 업로드 및 저장


                                if(resultscore >= 90) {
                                    Count2 = Count2+1;
                                    if(Count2 == 10){
                                        Log.d("Count2", "다이얼로그출력");
                                        saveImage(cropCopyBitmap);
                                        Speech();
                                        runOnUiThread(WatchDetectorActivity.this::showDlg);

                                    }
                                }

                                result.setLocation(location);

                                mappedRecognitions.add(result);





                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);

                        trackingOverlay.postInvalidate();

                        computingDetection = false;

//            runOnUiThread(
//                new Runnable() {
//                  @Override
//                  public void run() {
//            //        showFrameInfo(previewWidth + "x" + previewHeight);
//              //      showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//               //     showInference(lastProcessingTimeMs + "ms");
//                  }
//                });
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
