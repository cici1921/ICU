package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RuleDetailActivity1 extends AppCompatActivity {

    private Button btn_detailruleconfirm1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_detail1);

        btn_detailruleconfirm1 = findViewById(R.id.btn_detailconfirm1);

        btn_detailruleconfirm1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onBackPressed();
            }
        });
    }
}