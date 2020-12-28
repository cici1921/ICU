package com.example.facedetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;



import java.util.List;
;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class QuitActivity extends AppCompatActivity {


    private Button btn_quit;
    public interface RetrofitAPI{
        @GET("icu-func-sns")
        Call<List<POST>> getData(@Query("test_num") String id);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quit);


                btn_quit = findViewById(R.id.btn_quit);

        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Intent data = getIntent();
                String userPass = data.getStringExtra("userPass");

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("AWS-SNS-URL")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);
                retrofitAPI.getData(userPass).enqueue(new Callback<List<POST>>() {
                    @Override
                    public void onResponse(Call<List<POST>> call, @NonNull Response<List<POST>> response) {
                        if(response.isSuccessful()){
                            List<POST> data = response.body();
                            Log.d("TEST","성공");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<POST>> call, Throwable t) {
                        t.printStackTrace();
                    }
                });


                finishAffinity();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}