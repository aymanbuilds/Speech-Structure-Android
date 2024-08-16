package com.example.speechstruct;

import com.example.speechstruct.QuestionTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FetchDataTask {

    private ExecutorService executorService;

    public FetchDataTask() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void fetchQuestionsTemplate(String urlString, Callback callback) {
        Future<List<QuestionTemplate>> future = executorService.submit(new Callable<List<QuestionTemplate>>() {
            @Override
            public List<QuestionTemplate> call() throws Exception {
                List<QuestionTemplate> resultList = new ArrayList<>();
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlString);  // Use urlString here
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<QuestionTemplate>>() {}.getType();
                    resultList = gson.fromJson(response.toString(), listType);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return resultList;
            }
        });

        new Thread(() -> {
            try {
                List<QuestionTemplate> result = future.get();
                callback.onSuccess(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public interface Callback {
        void onSuccess(List<QuestionTemplate> result);
    }
}
