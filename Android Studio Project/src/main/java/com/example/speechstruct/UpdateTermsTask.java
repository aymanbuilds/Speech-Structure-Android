package com.example.speechstruct;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateTermsTask extends AsyncTask<String, Void, String> {
    private Context mContext;

    public UpdateTermsTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            String logoImage = params[0];
            String welcomeMessage = params[1];
            String termsText = params[2];

            URL url = new URL(Utilities.DOMAIN + "/api/terms/write");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            String jsonInputString = String.format(
                    "{\"welcome_message\": \"%s\", \"logo_image\": \"%s\", \"terms_text\": \"%s\"}",
                    welcomeMessage, logoImage, termsText
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                return "Terms updated successfully";
            } else {
                return "Failed to update terms";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
    }
}