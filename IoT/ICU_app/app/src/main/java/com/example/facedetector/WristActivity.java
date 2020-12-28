package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WristActivity extends AppCompatActivity {

    private Button btn_capture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrist);
        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");

        btn_capture = findViewById(R.id.btn_capture);


        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WristActivity.this, WatchDetectorActivity.class);

                intent.putExtra("userPass",userPass);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_left);
            }
        });
    }
}