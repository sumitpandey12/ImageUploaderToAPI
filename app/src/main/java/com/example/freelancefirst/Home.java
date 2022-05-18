package com.example.freelancefirst;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.freelancefirst.Network.MyResponse;
import com.example.freelancefirst.Network.UploadAPI;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Home extends AppCompatActivity implements View.OnClickListener {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    ImageCapture imageCapture;

    PreviewView previewView;
    ImageView btn_capture;
    ImageView imageView;
    ImageAnalysis imageAnalysis;
    RelativeLayout relativeLayout;
    TextView progressing;
    ProgressBar progressBar;
    Button btn_upload,btn_close,btn_cancel;
    LinearLayout layout_ok;

    private Bitmap bitmap;

    boolean cameraClick = false;
    boolean galleryClick = false;
    String imagePath;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Init();

        Intent intent = getIntent();
        if (intent.getStringExtra("URI")!=null){
            Toast.makeText(this, ""+intent.getStringExtra("URI"), Toast.LENGTH_SHORT).show();
            uri= Uri.parse(intent.getStringExtra("URI"));
            Context context = Home.this;
            imagePath = RealPathUtil.getRealPath(context, uri);
            bitmap = BitmapFactory.decodeFile(imagePath);
            btn_cancel.setVisibility(View.VISIBLE);
            btn_close.setVisibility(View.GONE);
            showImage();
        }

        btn_capture.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
        btn_close.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(()->{
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCamera(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },getExecutor());

    }

    private void Init() {
        btn_capture = findViewById(R.id.btn_img_camera);
        layout_ok = findViewById(R.id.layoutok);
        btn_upload = findViewById(R.id.btn_ok);
        btn_cancel = findViewById(R.id.btn_cancel);
        previewView = findViewById(R.id.previewView);
        imageView = findViewById(R.id.image_view);
        btn_close = findViewById(R.id.btn_retry);
        progressing = findViewById(R.id.textView1);
        progressBar = findViewById(R.id.progress_bar);
        relativeLayout = findViewById(R.id.relativeLayout);
    }


    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // camera selector use
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //preview view use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        //image analysis use case

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageCapture,imageAnalysis);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_img_camera:
                captureImage();
                break;
            case R.id.btn_ok:
                if (uri==null){
                    Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadImage();
                setProgressBar();
                break;

            case R.id.btn_retry:
                if (cameraClick){
                    File file = new File(imagePath);
                    if (file.exists()){
                        file.delete();
                    }
                    cameraClick=false;
                }
                uri=null;
                imagePath=null;
                showImage();
                break;
            case R.id.btn_cancel:
                btn_close.setVisibility(View.VISIBLE);
                btn_cancel.setVisibility(View.GONE);
                startActivity(new Intent(Home.this, HomePage.class));
                finish();
        }
    }

    private void setProgressBar(){
        if (relativeLayout.getVisibility()!=View.VISIBLE){
            relativeLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            btn_capture.setVisibility(View.GONE);
            layout_ok.setVisibility(View.GONE);

        }else {
            relativeLayout.setVisibility(View.GONE);
            btn_capture.setVisibility(View.VISIBLE);
            layout_ok.setVisibility(View.VISIBLE);
        }

    }

    private void captureImage() {

        long time = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISC_NUMBER, time);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE,  "image/jpeg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues

                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        uri = outputFileResults.getSavedUri();
                        Context context = Home.this;
                        imagePath = RealPathUtil.getRealPath(context, uri);
                        bitmap = BitmapFactory.decodeFile(imagePath);
                        cameraClick=true;
                        showImage();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(Home.this, "Error "+exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showImage() {
        if (previewView.getVisibility() == View.VISIBLE){
            previewView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            if (uri!=null){
                imageView.setImageURI(uri);
            }else {
                Toast.makeText(this, "Bitmap not set", Toast.LENGTH_SHORT).show();
            }
            btn_capture.setVisibility(View.GONE);
            layout_ok.setVisibility(View.VISIBLE);

        }else {
            previewView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            layout_ok.setVisibility(View.GONE);
            btn_capture.setVisibility(View.VISIBLE);
        }
    }

    public void uploadImage(){

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getResources().getString(R.string.BASE_URL))
                .addConverterFactory(GsonConverterFactory.create()).build();

        File finalFile = null;
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            String filename = String.format("%d.jpg",System.currentTimeMillis());
            finalFile = new File(path,filename);
            FileOutputStream fileOutputStream = new FileOutputStream(finalFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG,50,fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),finalFile);

        MultipartBody.Part body = MultipartBody.Part.createFormData("file",finalFile.getName(),requestFile);
        UploadAPI apiService = retrofit.create(UploadAPI.class);

        Call<MyResponse> call = apiService.uploadImage(body);

        try {
            call.enqueue(new Callback<MyResponse>() {
                @Override
                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                    if (response.isSuccessful()){

                        if (cameraClick){
                            File file = new File(imagePath);
                            if (file.exists()){
                                file.delete();
                            }
                            cameraClick=false;
                        }
                    }

                    new AlertDialog.Builder(Home.this)
                            .setTitle("Status")
                            .setMessage(""+response.body().getClass_()+"\n"+response.body().getConfidence())
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    uri=null;
                                    imagePath=null;
                                    setProgressBar();
                                    showImage();
                                    startActivity(new Intent(Home.this, HomePage.class));
                                    finish();
                                }
                            }).show();
                }

                @Override
                public void onFailure(Call<MyResponse> call, Throwable t) {

                    if (cameraClick){
                        File file = new File(imagePath);
                        if (file.exists()){
                            file.delete();
                        }
                        cameraClick=false;
                    }

                    new AlertDialog.Builder(Home.this)
                            .setTitle("Status")
                            .setMessage("API not Connect")
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    setProgressBar();
                                    showImage();
                                    uri=null;
                                    imagePath=null;
                                    startActivity(new Intent(Home.this, HomePage.class));
                                    finish();
                                }
                            }).show();
                }
            });

        }catch (Exception e){
            Toast.makeText(this, "Problem With The Server", Toast.LENGTH_SHORT).show();
            uri=null;
            imagePath=null;
            setProgressBar();
            showImage();
        }
    }
}