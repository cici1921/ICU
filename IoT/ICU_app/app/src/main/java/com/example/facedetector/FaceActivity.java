package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.facedetector.tflite.Detector;

public class FaceActivity extends AppCompatActivity {

    private Button btn_capture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        Intent data = getIntent();

        String userPass = data.getStringExtra("userPass");



        btn_capture= findViewById(R.id.btn_capture);
        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            Intent intent = new Intent(FaceActivity.this, FaceDetector.class);


                intent.putExtra("userPass",userPass); //수험번호
                startActivity(intent);


            }
        });


    }
}