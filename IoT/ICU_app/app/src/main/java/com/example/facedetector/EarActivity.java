package com.example.facedetector;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class EarActivity extends AppCompatActivity {
    private Button btn_capture;
    private CustomDialog customDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ear);
        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");

        btn_capture = findViewById(R.id.btn_capture);


        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(EarActivity.this, DetectorActivity.class);

                intent.putExtra("userPass", userPass);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_left);
            }
        });
    }
}
