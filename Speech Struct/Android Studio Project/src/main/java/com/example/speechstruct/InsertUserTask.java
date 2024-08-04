package com.example.speechstruct;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InsertUserTask extends AsyncTask<String, Void, String> {
    private Context mContext;

    private TaskCallback mCallback;

    public InsertUserTask(Context context, TaskCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        String username = params[0];
        String email = params[1];
        String password = params[2];

        try {
            URL url = new URL(Utilities.DOMAIN + "/users/add");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("username", username);
            jsonParam.put("email", email);
            jsonParam.put("password", password);

            OutputStream os = urlConnection.getOutputStream();
            os.write(jsonParam.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                return "Template added successfully";
            } else {
                return "Failed to add template";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
        if (mCallback != null) {
            mCallback.onTaskCompleted();
        }
    }
}