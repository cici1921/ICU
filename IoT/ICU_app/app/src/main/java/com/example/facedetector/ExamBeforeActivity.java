package com.example.facedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class ExamBeforeActivity extends AppCompatActivity {

    private Button btn_next;
    private CheckBox cd_comfirml;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_before);

        btn_next = findViewById(R.id.btn_next);

        cd_comfirml = findViewById(R.id.cb_confirm1);


        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(cd_comfirml.isChecked()) {

                    Intent data = getIntent();
                    String userPass = data.getStringExtra("userPass");
                    Log.d("userPass", userPass);
                    Intent intent = new Intent(ExamBeforeActivity.this, FaceActivity.class);
                    intent.putExtra("userPass", userPass);
                    startActivity(intent);
                    overridePendingTransition(R.anim.in_right, R.anim.out_left);
                }else{
                    Toast.makeText(getApplicationContext(), "체크박스를 체크해주시기 바랍니다.", Toast.LENGTH_LONG).show();
                    return;
                }

            }
        });
    }

    @Override
    public void onBackPressed() {

        // 버튼 클릭시 Custom Dialog 호출
        CustomDialog dlg = new CustomDialog(this);
        dlg.callDialog();

    }
}
