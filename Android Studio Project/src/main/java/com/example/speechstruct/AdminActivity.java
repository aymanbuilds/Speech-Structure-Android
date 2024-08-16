package com.example.speechstruct;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

public class AdminActivity extends AppCompatActivity {

    WebView webView1;
    private ValueCallback<Uri[]> mFilePathCallback;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                results = new Uri[]{data.getData()};
            }
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        webView1 = findViewById(R.id.webview1);
        webView1.getSettings().setDomStorageEnabled(true);
        webView1.getSettings().setDomStorageEnabled(true);
        webView1.getSettings().setJavaScriptEnabled(true);

        webView1.addJavascriptInterface(new AdminActivity.WebAppInterface(this), "Android");

        webView1.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Save the filePathCallback for later use
                mFilePathCallback = filePathCallback;
                // Create an Intent to open the file chooser
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // Accept all file types, you can specify a type if needed
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), 10);
                return true;
            }
        });

        webView1.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView1.evaluateJavascript("javascript:setApiDomain('"+Utilities.DOMAIN+"')", value -> {

                });
            }
        });

        webView1.loadUrl("file:///android_asset/UI/admin-panel.html");
    }

    private class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void updateTerms(String uploadedImage, String welcomeMessage, String terms) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String decodedWelcomeMessage = new String(Base64.decode(welcomeMessage, Base64.DEFAULT), "UTF-8");
                        String decodedTerms = new String(Base64.decode(terms, Base64.DEFAULT), "UTF-8");
                        String decodedUploadedImage = URLDecoder.decode(uploadedImage, "UTF-8");

                        decodedWelcomeMessage = URLDecoder.decode(decodedWelcomeMessage, "UTF-8");
                        decodedTerms = URLDecoder.decode(decodedTerms, "UTF-8");

                        new UpdateTermsTask(AdminActivity.this).execute(decodedUploadedImage, decodedWelcomeMessage, decodedTerms);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @JavascriptInterface
        public void addQuestion(String encodedCategory, String encodedQuestionNumber, String encodedQuestionMedia,
                                String encodedQuestionText, String encodedResponseType,
                                String encodedAnswerNumber, String encodedAnswerIncludingMedia,
                                String encodedNextQuestion) {
            // Decode the values
            String category = decodeBase64(encodedCategory);
            String questionNumber = decodeBase64(encodedQuestionNumber);
            String questionMedia = decodeBase64(encodedQuestionMedia);
            String questionText = decodeBase64(encodedQuestionText);
            String responseType = decodeBase64(encodedResponseType);
            String answerNumber = decodeBase64(encodedAnswerNumber);
            String answerIncludingMedia = decodeBase64(encodedAnswerIncludingMedia);
            String nextQuestion = decodeBase64(encodedNextQuestion);

            // Execute AsyncTask to insert template
            new InsertTemplateTask(mContext, new TaskCallback() {
                @Override
                public void onTaskCompleted() {
                    webView1.evaluateJavascript("javascript:getQuestions()", value -> {

                    });
                }
            }).execute(category, questionNumber, questionMedia, questionText,
                    responseType, answerNumber, answerIncludingMedia, nextQuestion);
        }

        private String decodeBase64(String encoded) {
            try {
                String decoded = new String(Base64.decode(encoded, Base64.DEFAULT), "UTF-8");
                return decodeURIComponent(decoded);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private String decodeURIComponent(String encoded) {
            try {
                return java.net.URLDecoder.decode(encoded, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        @JavascriptInterface
        public void addUser(String encodedUsername, String encodedEmail, String encodedPassword) {
            // Decode the values
            String username = decodeBase64(encodedUsername);
            String email = decodeBase64(encodedEmail);
            String password = decodeBase64(encodedPassword);

            new InsertUserTask(mContext, new TaskCallback() {
                @Override
                public void onTaskCompleted() {
                    webView1.evaluateJavascript("javascript:getUsers()", value -> {

                    });
                }
            }).execute(username, email, password);
        }

        @JavascriptInterface
        public void logout() {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(mContext, LoginActivity.class);
            ((Activity) mContext).startActivity(intent);
            ((Activity) mContext).finish();
            finish();
        }
    }
}