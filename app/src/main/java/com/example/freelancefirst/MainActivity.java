package com.example.freelancefirst;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Button btnSendOtp,btnVerifyOtp;
    SignInButton btnGoogle;
    TextInputEditText txtPhone,txtPhoneCode;
    LinearLayout layout1, layout2;
    PinView txtOTP;

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    FirebaseAuth mAuth;
    String verificationId;
    ProgressBar progressBar;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor myedit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGoogle = findViewById(R.id.btn_google);
        txtPhone = findViewById(R.id.txt_phone);
        txtOTP = findViewById(R.id.txt_otp);
        btnSendOtp = findViewById(R.id.btn_sendOtp);
        btnVerifyOtp = findViewById(R.id.btn_verifyOtp);
        layout1 = findViewById(R.id.layout1);
        layout2 = findViewById(R.id.layout2);
        progressBar = findViewById(R.id.progress_bar_send);
        txtPhoneCode = findViewById(R.id.txt_phone_code);


        sharedPreferences = getSharedPreferences("mySharePreferences",MODE_PRIVATE);
        myedit = sharedPreferences.edit();
        if (sharedPreferences.getBoolean("Login", false)){
            nextActivity();
        }

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (TextUtils.isEmpty(txtPhone.getText().toString()) && txtPhone.length()!=10){
                    Toast.makeText(MainActivity.this, "Phone not correct", Toast.LENGTH_SHORT).show();
                }else {
                    progressBar();
                    String number = txtPhoneCode.getText().toString()+txtPhone.getText().toString();

                    PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber(number)
                            .setTimeout(60L,TimeUnit.SECONDS)
                            .setActivity(MainActivity.this)
                            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                @Override
                                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                    Toast.makeText(MainActivity.this, "OTP Send Successfully", Toast.LENGTH_SHORT).show();
                                    layout1.setVisibility(View.GONE);
                                    layout2.setVisibility(View.VISIBLE);
                                    progressBar();
                                }

                                @Override
                                public void onVerificationFailed(@NonNull FirebaseException e) {
                                    Toast.makeText(MainActivity.this, "Something Went Wrong"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressBar();
                                }

                                @Override
                                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                    super.onCodeSent(s, forceResendingToken);
                                    verificationId=s;
                                }
                            }).build();
                    PhoneAuthProvider.verifyPhoneNumber(options);
                }
            }
        });

        btnVerifyOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(MainActivity.this, txtOTP.getText().toString(), Toast.LENGTH_SHORT).show();
                if (TextUtils.isEmpty(txtOTP.getText().toString())){
                    Toast.makeText(MainActivity.this, "OTP not valid", Toast.LENGTH_SHORT).show();
                }else{
                    String code = txtOTP.getText().toString();
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,code);

                    mAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                nextActivity();
                            }else{
                                Toast.makeText(MainActivity.this, "Task Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });


    }
    private void signIn() {
        Intent intent = gsc.getSignInIntent();
        startActivityForResult(intent,100);
    }


    private void progressBar(){
        if (progressBar.getVisibility()!=View.VISIBLE){
            progressBar.setVisibility(View.VISIBLE);
            btnSendOtp.setVisibility(View.GONE);
        }else {
            btnSendOtp.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==100){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                task.getResult(ApiException.class);
                nextActivity();
            } catch (ApiException e) {
                Toast.makeText(this, ""+e.getStatus(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void nextActivity(){
        myedit.putBoolean("Login",true);
        myedit.apply();
        layout1.setVisibility(View.VISIBLE);
        layout2.setVisibility(View.GONE);
        Intent intent = new Intent(MainActivity.this, HomePage.class);
        startActivity(intent);
        finish();
    }

}