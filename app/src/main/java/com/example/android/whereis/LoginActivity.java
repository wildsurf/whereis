package com.example.android.whereis;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RC_SIGN_IN = 23942;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.initGoogleAuth();
        findViewById(R.id.sign_in_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    public void signIn() {
        Log.v(LoginActivity.class.getSimpleName(), "signIn");
        Intent signInIntent = GoogleSigninUtil.mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void initGoogleAuth() {
        GoogleSigninUtil.initGoogleSigninClient(this);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        this.handleSignInResult(account);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> completedTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = completedTask.getResult(ApiException.class);
                this.handleSignInResult(account);
            } catch (ApiException e) {

                Log.w(LoginActivity.class.getSimpleName(),
                        "signInResult:failed code=" + e.getStatusCode());
            }
        }
    }

    public void handleSignInResult(GoogleSignInAccount account) {
        if (account != null) {
            Log.v(LoginActivity.class.getSimpleName(), account.getDisplayName());
            UserInfoUtil.storeUserInfo(this, account);
            Intent intent = new Intent(this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
