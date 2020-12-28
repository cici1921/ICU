package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RuleDetailActivity2 extends AppCompatActivity {

    private Button btn_detailruleconfirm2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_detail2);

        btn_detailruleconfirm2 = findViewById(R.id.btn_detailconfirm2);

        btn_detailruleconfirm2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Intent intent = new Intent(RuleDetailActivity2.this, RuleActivity.class);
                //startActivity(intent);
                onBackPressed();
            }
        });
    }
}