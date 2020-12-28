package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class RuleActivity extends AppCompatActivity {

    private CheckBox cb_rulemain1, cb_rulemain2;
    private Button btn_ruleconfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);

        cb_rulemain1 = findViewById(R.id.cb_rulesub1);
        cb_rulemain2 = findViewById(R.id.cb_rulesub2);
        btn_ruleconfirm = findViewById(R.id.btn_ruleconfirm);

        Intent data = getIntent();
        String userPass = data.getStringExtra("userPass");
        Log.d("userPass",userPass);

        cb_rulemain1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RuleActivity.this, RuleDetailActivity1.class);
                startActivity(intent);
            }
        });

        cb_rulemain2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RuleActivity.this, RuleDetailActivity2.class);
                startActivity(intent);
            }
        });

        btn_ruleconfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RuleActivity.this, testingDetecting.class);
                intent.putExtra("userPass",userPass); //수험번호
                startActivity(intent);

                overridePendingTransition(R.anim.in_right, R.anim.out_left);
            }
        });
    }
}