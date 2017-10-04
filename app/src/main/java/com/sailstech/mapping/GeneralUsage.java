package com.sailstech.mapping;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.DeleteCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by richard on 2016/11/25.
 */

public class GeneralUsage {
    public static final int PARSE_LOGIN=0x00;
    public static final int PARSE_LOGOUT=0x01;
    public static void ShowErrorDialog(Context context) {
        new MaterialDialog.Builder(context)
                .title(R.string.dialog_internet_error_title)
                .content(R.string.dialog_internet_error_content)
                .positiveText(R.string.dialog_ok)
                .show();
    }
    public static void ShowErrorDialog(Context context,String msg) {
        new MaterialDialog.Builder(context)
                .content(msg)
                .positiveText(R.string.dialog_ok)
                .show();
    }
    public static MaterialDialog ShowProgressing(Context context,int stringId) {
        return new MaterialDialog.Builder(context)
                .progress(true,100)
                .cancelable(false)
                .content(stringId)
                .show();
    }
    public static MaterialDialog ShowSignInConfirmDialog(Activity activity) {
        return ShowSignInConfirmDialog(activity,R.string.dialog_sign_in_need_contnet);
    }

    public static MaterialDialog ShowSignInConfirmDialog(final Activity activity,int stringId) {
        return new MaterialDialog.Builder(activity)
                .title(R.string.dialog_sign_in_need)
                .content(stringId)
                .negativeText(R.string.dialog_not_now)
                .positiveText(R.string.dialog_yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        ShowSignInActivity(activity);
                    }
                })
                .show();
    }
    public static void ShowSignInActivity(Activity activity) {

        ParseLoginBuilder builder = new ParseLoginBuilder(activity);
        builder.setParseLoginButtonText(R.string.dialog_log_in)
                .setParseSignupButtonText(R.string.dialog_sign_up)
                .setParseSignupSubmitButtonText(R.string.dialog_submit)
                .setParseLoginHelpText(R.string.dialog_forgot_password)
                .setAppLogo(R.drawable.login_logo);


        activity.startActivityForResult(builder.build(), PARSE_LOGIN);

    }

    public static void ShowLogoutConfirmDialog(final Context context, final LogOutCallback logOutCallback) {
        new MaterialDialog.Builder(context)
                .content(R.string.dialog_log_out_content)
                .negativeText(R.string.dialog_no)
                .positiveText(R.string.dialog_yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        LogoutProcedure(context,logOutCallback);
                    }
                })
                .show();

    }
    public static void LogoutProcedure(final Context context, final LogOutCallback logOutCallback) {
        final MaterialDialog md= new MaterialDialog.Builder(context)
                .progress(true,100)
                .cancelable(false)
                .content(R.string.dialog_progressing)
                .show();

        ParseObject.unpinAllInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null) {
                    md.dismiss();
                    ShowErrorDialog(context);
                    return;
                }
                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        md.dismiss();
                        if(e!=null) {
                            ShowErrorDialog(context);
                            return;
                        }
                        if(logOutCallback!=null)
                            logOutCallback.done(null);

                    }
                });
            }
        });

    }
    public static int dpToPx(Context context,int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

}
