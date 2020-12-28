package com.example.facedetector;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CustomDialog {

    private Context context;
    private Button btnSave;

    public CustomDialog(Context context)
    {
        this.context = context;
    }

    public void callDialog()
    {
        final Dialog dialog = new Dialog(context);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.show();

        btnSave = (Button) dialog.findViewById(R.id.btnSave);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 확인 버튼
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                Toast.makeText(context, "앱을 종료하시겠습니까?", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                ((MainActivity) context).finish();
            }
        });

    }

}