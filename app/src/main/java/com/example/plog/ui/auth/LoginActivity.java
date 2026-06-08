package com.example.plog.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plog.MainActivity;
import com.example.plog.R;
import com.example.plog.network.ApiClient;
import com.example.plog.network.auth.EmailRequest;
import com.example.plog.network.auth.LoginRequest;
import com.example.plog.util.SessionManager;
import com.example.plog.network.auth.LoginResponse;
import com.example.plog.network.auth.RegisterRequest;
import com.example.plog.network.auth.VerifyRequest;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // 로그인 화면 뷰
    EditText username, password;

    // 회원가입 다이얼로그 뷰
    TextInputEditText reg_username, reg_password, reg_email;

    Button login, signUp, reg_register;
    TextInputLayout txtInLayoutUsername, txtInLayoutPassword, txtInLayoutRegPassword;
    CheckBox rememberMe;

    private TextInputEditText reg_verificationCode;
    private Button btnSendCode;
    private Button btnVerifyCode;

    private boolean emailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("plog_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        username       = findViewById(R.id.username);
        password       = findViewById(R.id.password);
        login          = findViewById(R.id.login);
        signUp         = findViewById(R.id.signUp);
        txtInLayoutUsername = findViewById(R.id.txtInLayoutUsername);
        txtInLayoutPassword = findViewById(R.id.txtInLayoutPassword);
        rememberMe     = findViewById(R.id.rememberMe);

        clickLogin();

        signUp.setOnClickListener(view -> clickSignUp());
    }

    private void clickLogin() {
        login.setOnClickListener(view -> {

            boolean valid = true;
            if (username.getText().toString().trim().isEmpty()) {
                txtInLayoutUsername.setError("닉네임을 입력하세요");
                valid = false;
            } else { txtInLayoutUsername.setError(null); }

            if (password.getText().toString().trim().isEmpty()) {
                txtInLayoutPassword.setError("비밀번호를 입력하세요");
                valid = false;
            } else { txtInLayoutPassword.setError(null); }

            if (valid) {
                String email = username.getText().toString().trim();
                String pw    = password.getText().toString().trim();

                LoginRequest request = new LoginRequest(email, pw);

                ApiClient.getApiService()
                        .login(request)
                        .enqueue(new Callback<LoginResponse>() {
                            @Override
                            public void onResponse(Call<LoginResponse> call,
                                                   Response<LoginResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    int uid = response.body().getUserId();
                                    SharedPreferences prefs =
                                            getSharedPreferences("plog_prefs", MODE_PRIVATE);
                                    prefs.edit()
                                            .putBoolean("isLoggedIn", true)
                                            .putString("token", response.body().getData().getAccessToken())
                                            .putInt("userId", uid)
                                            .apply();
                                    new SessionManager(LoginActivity.this).saveUserId(uid);

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Snackbar.make(view, "이메일 또는 비밀번호가 틀렸습니다",
                                            Snackbar.LENGTH_LONG).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<LoginResponse> call, Throwable t) {
                                Snackbar.make(view, "서버 연결 실패: " + t.getMessage(),
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private void clickSignUp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register, null);
        builder.setView(dialogView);

        reg_username     = dialogView.findViewById(R.id.reg_username);
        reg_password     = dialogView.findViewById(R.id.reg_password);
        reg_email        = dialogView.findViewById(R.id.reg_email);
        reg_register     = dialogView.findViewById(R.id.reg_register);
        txtInLayoutRegPassword = dialogView.findViewById(R.id.txtInLayoutRegPassword);

        btnSendCode = dialogView.findViewById(R.id.btnSendCode);
        btnVerifyCode = dialogView.findViewById(R.id.btnVerifyCode);
        reg_verificationCode =
                dialogView.findViewById(R.id.reg_verificationCode);

        AlertDialog alertDialog = builder.create();

        btnSendCode.setOnClickListener(v -> {

            String email =
                    reg_email.getText().toString().trim();

            if(email.isEmpty()) {

                reg_email.setError("이메일 입력");
                return;
            }

            ApiClient.getApiService()
                    .sendEmailCode(
                            new EmailRequest(email))
                    .enqueue(new Callback<Void>() {

                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response
                        ) {

                            if(response.isSuccessful()) {

                                Toast.makeText(
                                        LoginActivity.this,
                                        "인증코드 발송 완료",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        LoginActivity.this,
                                        "발송 실패",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<Void> call,
                                Throwable t
                        ) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "서버 오류",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

        });

        btnVerifyCode.setOnClickListener(v -> {

            String email =
                    reg_email.getText().toString().trim();

            String code =
                    reg_verificationCode
                            .getText()
                            .toString()
                            .trim();

            ApiClient.getApiService()
                    .verifyEmailCode(
                            new VerifyRequest(
                                    email,
                                    code
                            )
                    )
                    .enqueue(new Callback<Void>() {

                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response
                        ) {

                            if(response.isSuccessful()) {

                                emailVerified = true;

                                Toast.makeText(
                                        LoginActivity.this,
                                        "인증 완료",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        LoginActivity.this,
                                        "인증 실패",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<Void> call,
                                Throwable t
                        ) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "서버 오류",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

        });

        reg_register.setOnClickListener(view -> {
            boolean valid = true;

            if (reg_username.getText().toString().trim().isEmpty()) {
                reg_username.setError("아이디를 입력하세요"); valid = false;
            }
            if (reg_password.getText().toString().trim().isEmpty()) {
                reg_password.setError("비밀번호를 입력하세요"); valid = false;
            }
            if (reg_email.getText().toString().trim().isEmpty()) {
                reg_email.setError("이메일을 입력하세요"); valid = false;
            }

            if(!emailVerified){

                Toast.makeText(
                        LoginActivity.this,
                        "이메일 인증을 완료하세요",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
            if (valid) {
                RegisterRequest request = new RegisterRequest(
                        reg_username.getText().toString().trim(),
                        reg_email.getText().toString().trim(),
                        reg_password.getText().toString().trim()
                );

                ApiClient.getApiService()
                        .register(request)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "회원가입 완료! 로그인해주세요",
                                            Toast.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "회원가입 실패", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(LoginActivity.this,
                                        "서버 연결 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        alertDialog.show();
    }

    private void showErrorSnackbar(View view) {
        Snackbar.make(view, "필드를 모두 채워주세요", Snackbar.LENGTH_LONG).show();
    }


}