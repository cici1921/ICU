package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckFace extends AppCompatActivity { //facedetection후, 사진을 띄우고, 얼굴이 맞는지 확인한 후에 aws에 올리기.

    private final int GET_GALLERY_IMAGE = 200;
    private ImageView imageview;
    private Button savebtn;
    private Button cancelbtn;
    private TextView number;
    OutputStream out = null;
    private static final String TAG = "AWS";

    private final String accessKey = "";
    private final String secretKey = "";

    private void onAWS(String imageFileName, File file) {
        //AWS버킷
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials, Region.getRegion(Regions.US_EAST_1));

        TransferUtility transferUtility = TransferUtility.builder().s3Client(s3Client).context(this).build();
        TransferNetworkLossHandler.getInstance(this);

        TransferObserver uploadObserver = transferUtility.upload("icu-tester-face-s3", "userface/"+imageFileName, file);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_face);
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");
        String imageFileName = userPass + ".jpg";
        File file = new File(path, imageFileName);
        imageview = (ImageView) findViewById(R.id.faceuser);
        savebtn = (Button) findViewById(R.id.savebtn);
        cancelbtn = (Button) findViewById(R.id.cancelbtn);
        number = (TextView) findViewById(R.id.usernumber);

        number.setText(userPass+"수험자"); //수험번호 뷰에 띄우기.
        try {//이미지 파일 로드하기
            Uri uri = Uri.parse("file:///" + Environment.getExternalStorageDirectory() + "/Images/" + imageFileName);
            imageview.setImageURI(uri);


        } catch (Exception e) {
            e.printStackTrace();
        }
        //저장버튼
        savebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAWS(imageFileName, file); //AWS로 이미지 업로드하기


                Intent intent = new Intent(CheckFace.this, EarActivity.class);
                intent.putExtra("userPass",userPass);
                startActivity(intent);
            }
        });
        cancelbtn.setOnClickListener(new View.OnClickListener() { //다시 본인 얼굴 찍기.
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckFace.this, FaceDetector.class);
                startActivity(intent);

            }
        });


    }


}