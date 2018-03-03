package com.example.android.whereis;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.InputStream;

/**
 * Created by veronikahillebrand on 01/03/2018.
 */

public final class UserInfoUtil {
    private static final String USER_PREFERENCES = "user_preferences";

    private static final String USER_DISPLAY_NAME = "user_display_name";
    private static final String USER_PHOTO_URL = "user_photo_url";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_ID = "user_id";

    static void storeUserInfo(Context context, GoogleSignInAccount account) {
        int mode = Activity.MODE_PRIVATE;

        SharedPreferences mySharedPreferences = context.getSharedPreferences(USER_PREFERENCES, mode);
        SharedPreferences.Editor editor = mySharedPreferences.edit();

        editor.putString(USER_DISPLAY_NAME, account.getDisplayName());
        editor.putString(USER_EMAIL, account.getEmail());
        editor.putString(USER_ID, account.getId());
        if (account.getPhotoUrl() != null) {
            editor.putString(USER_PHOTO_URL, account.getPhotoUrl().toString());
        }

        editor.apply();
    }

    public static String getUserDisplayName(Context context) {
        int mode = Activity.MODE_PRIVATE;

        SharedPreferences mySharedPreferences = context.getSharedPreferences(USER_PREFERENCES, mode);
        return mySharedPreferences.getString(USER_DISPLAY_NAME, "");
    }

    public static String getUserPhotoUrl(Context context) {
        int mode = Activity.MODE_PRIVATE;

        SharedPreferences mySharedPreferences = context.getSharedPreferences(USER_PREFERENCES, mode);
        return mySharedPreferences.getString(USER_PHOTO_URL, "");
    }

    public static String getUserEmail(Context context) {
        int mode = Activity.MODE_PRIVATE;

        SharedPreferences mySharedPreferences = context.getSharedPreferences(USER_PREFERENCES, mode);
        return mySharedPreferences.getString(USER_EMAIL, "");
    }

    public static String getUserId(Context context) {
        int mode = Activity.MODE_PRIVATE;

        SharedPreferences mySharedPreferences = context.getSharedPreferences(USER_PREFERENCES, mode);
        return mySharedPreferences.getString(USER_ID, "");
    }
}
