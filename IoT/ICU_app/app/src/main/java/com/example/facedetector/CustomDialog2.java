package com.example.facedetector;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class CustomDialog2 extends AppCompatActivity {

    private Activity activity;
    private Button btnSave;
    String userPass;


    public CustomDialog2(Activity activity)
    {
        this.activity = activity;
    }

    public void callDialog2()
    {
        final Dialog dialog = new Dialog(activity);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.show();
        Intent data = activity.getIntent();
        userPass = data.getStringExtra("userPass");
        Log.d("userPass","custom2"+userPass);

        btnSave = (Button) dialog.findViewById(R.id.btnSave);


        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 확인 버튼
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, RuleActivity.class);
                intent.putExtra("userPass",userPass); //수험번호
                activity.startActivity(intent);

            }
        });

    }

}