package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PersonActivity extends AppCompatActivity { //주위사람 체크

    private Button btn_capture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");

        btn_capture = findViewById(R.id.btn_capture);

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PersonActivity.this, Humandetector.class);
                intent.putExtra("userPass",userPass);
                startActivity(intent);
            }
        });
    }
}