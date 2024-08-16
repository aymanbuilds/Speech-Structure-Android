package com.example.speechstruct;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InsertTemplateTask extends AsyncTask<String, Void, String> {
    private Context mContext;

    private TaskCallback mCallback;

    InsertTemplateTask(Context context, TaskCallback callback) {
        mContext = context;
        this.mCallback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            String jsonInputString = String.format(
                    "{\"category\": \"%s\", \"question_number\": \"%s\", \"question_media\": \"%s\", \"question\": \"%s\", " +
                            "\"response_type\": \"%s\", \"answer_number\": \"%s\", \"answer_including_media\": \"%s\", \"next_question\": \"%s\"}",
                    params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7]
            );

            URL url = new URL(Utilities.DOMAIN + "/api/template/write");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                return "Template added successfully";
            } else {
                return "Failed to add template";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred";
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