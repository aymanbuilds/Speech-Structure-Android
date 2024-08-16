package com.example.speechstruct;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    WebView webView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView1 = findViewById(R.id.webview1);
        webView1.getSettings().setDomStorageEnabled(true);
        webView1.getSettings().setDomStorageEnabled(true);
        webView1.getSettings().setJavaScriptEnabled(true);

        webView1.addJavascriptInterface(new LoginActivity.WebAppInterface(this), "Android");

        webView1.loadUrl("file:///android_asset/UI/login.html");
    }

    private class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void checkUser(String username, String password, boolean rememberMe) {
            new Thread(() -> {
                try {
                    URL url = new URL(Utilities.DOMAIN + "/api/login");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("username", username);
                    jsonParam.put("password", password);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = jsonParam.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = connection.getResponseCode();
                    InputStream is = responseCode == HttpURLConnection.HTTP_OK
                            ? connection.getInputStream() : connection.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line.trim());
                    }
                    reader.close();

                    String responseString = response.toString();
                    //((Activity) mContext).runOnUiThread(() -> Toast.makeText(mContext, responseString, Toast.LENGTH_LONG).show());

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        JSONObject jsonResponse = new JSONObject(responseString);
                        String status = jsonResponse.getString("status");
                        String role = jsonResponse.getString("role");

                        if ("active".equals(status)) {
                            if ("user".equals(role)) {
                                Intent intent = new Intent(mContext, HomeActivity.class);
                                ((Activity) mContext).startActivity(intent);
                                finish();
                            } else if("admin".equals(role)){
                                Intent intent = new Intent(mContext, AdminActivity.class);
                                ((Activity) mContext).startActivity(intent);
                                finish();
                            }

                            if (rememberMe) {
                                SharedPreferences prefs = mContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("username", username);
                                editor.putString("role", role);
                                editor.putString("status", status);
                                editor.apply();
                            }
                        } else {
                            ((Activity) mContext).runOnUiThread(() -> Toast.makeText(mContext, "User is inactive.", Toast.LENGTH_LONG).show());
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        ((Activity) mContext).runOnUiThread(() -> Toast.makeText(mContext, "Invalid username or password.", Toast.LENGTH_LONG).show());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) mContext).runOnUiThread(() -> Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }
}