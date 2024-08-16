package com.example.speechstruct;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    WebView webView1;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private List<QuestionTemplate> questionList;
    private List<QuestionTemplate> finalQuestionList;
    int questionIndex = 0;
    private boolean isListening = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;
    String processStartTime = null;
    String processEndTime = null;

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permission denied. Cannot use speech recognition.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        requestPermissions();

        questionList = new ArrayList<>();
        finalQuestionList = new ArrayList<>();

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("Speech", "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("Speech", "Speech beginning");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("Speech", "Speech ended");
                if (isListening) {
                    //Toast.makeText(HomeActivity.this, "You provided an answer!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(int error) {
                Log.d("Speech", "Error: " + error);
                Toast.makeText(HomeActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                speak("An error occurred, please try again.");
                speechRecognizer.cancel();
                stopListening();
                srError = true;
                isListening = false;
                webView1.evaluateJavascript("javascript:stopListening()", value -> {
                });
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    executor.submit(() -> handleSpeechResults(matches));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        FetchDataTask fetchDataTask = new FetchDataTask();
        fetchDataTask.fetchQuestionsTemplate(Utilities.DOMAIN + "/api/read_template", result -> {
            runOnUiThread(() -> {
                questionList.addAll(result);

                if (!questionList.isEmpty()) {
                    webView1 = findViewById(R.id.webview1);
                    webView1.getSettings().setDomStorageEnabled(true);
                    webView1.getSettings().setJavaScriptEnabled(true);

                    webView1.addJavascriptInterface(new HomeActivity.WebAppInterface(this), "Android");

                    webView1.loadUrl("file:///android_asset/UI/home.html");

                    webView1.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            if (questionIndex >= 0 && questionIndex < questionList.size()) {
                                webView1.evaluateJavascript("javascript:setQuestion(`" + questionList.get(questionIndex).getQuestion() + "`)", value -> {
                                });
                            }
                        }
                    });
                }
            });
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                if (questionIndex < questionList.size()) {
                    if(!srError){
                        runOnUiThread(() -> startListening());
                    }
                }
            }

            @Override
            public void onError(String s) {

            }
        });
    }

    boolean srError = false;

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TID01");
    }

    private void speakQuestion() {
        if (questionIndex < questionList.size()){
            String question = questionList.get(questionIndex).getQuestion();
            speak(question);
            webView1.evaluateJavascript("javascript:setQuestion(`" + question + "`)", value -> {
            });
        }
        else{
            speak("Thank you for your time!");
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            srError = false;
            if (!isListening) {
                Toast.makeText(this, "Start speaking!", Toast.LENGTH_SHORT).show();
                isListening = true;
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                speechRecognizer.startListening(intent);
            }
        } else {
            Toast.makeText(this, "Permission to record audio is not granted.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopListening() {
        if (isListening) {
            //speechRecognizer.stopListening();
            speechRecognizer.cancel();
            isListening = false;
        }
    }

    private void resetListening(){
        isListening = false;
        questionIndex = 0;
        srError = false;
        speechRecognizer.cancel();
    }

    /*private void handleSpeechResults(ArrayList<String> matches) {
        runOnUiThread(() -> {
            stopListening();
            if (matches != null && !matches.isEmpty()) {
                String answer = matches.get(0);
                questionList.get(questionIndex).setUserAnswer(answer);
                questionIndex++;
                if (questionIndex < questionList.size()) {
                    speakQuestion();
                } else {
                    Toast.makeText(HomeActivity.this, "Done", Toast.LENGTH_SHORT).show();
                    speak("Thank you for your time!");
                    stopListening();
                    webView1.evaluateJavascript("javascript:stopListening()", value -> {
                    });
                    srError = false;
                    isListening = false;
                }
            } else {
                Toast.makeText(HomeActivity.this, "No speech recognized", Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    private void handleSpeechResults(ArrayList<String> matches) {
        runOnUiThread(() -> {
            stopListening();

            if (matches != null && !matches.isEmpty()) {
                String answer = matches.get(0);

                // Find the current question
                QuestionTemplate currentQuestion = questionList.get(questionIndex);

                // Check the response type of the current question
                if ("Open Response".equals(currentQuestion.getResponseType())) {
                    // Prepare the JSON object to send to the API for "Open Response" questions
                    JSONObject requestData = new JSONObject();
                    try {
                        JSONArray questionsArray = new JSONArray();
                        for (QuestionTemplate question : questionList) {
                            if ("Open Response".equals(question.getResponseType())) {
                                JSONObject questionJson = new JSONObject();
                                questionJson.put("question", question.getQuestion());
                                questionJson.put("answer", new JSONArray());
                                questionsArray.put(questionJson);
                            }
                        }

                        requestData.put("questions", questionsArray);
                        requestData.put("answer", answer);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Send the request to the API
                    String url = Utilities.DOMAIN + "/match-answers";
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            requestData,
                            response -> {
                                try {
                                    // Process API response
                                    JSONArray responseQuestions = response.getJSONArray("questions");
                                    List<QuestionTemplate> answeredQuestions = new ArrayList<>();


                                    for (int i = 0; i < responseQuestions.length(); i++) {
                                        JSONObject questionJson = responseQuestions.getJSONObject(i);
                                        String questionText = questionJson.getString("question");
                                        JSONArray answersArray = questionJson.getJSONArray("answer");

                                        for (QuestionTemplate question : questionList) {
                                            if (question.getQuestion().equals(questionText) && "Open Response".equals(question.getResponseType())) {
                                                if (answersArray.length() > 0) {
                                                    question.setUserAnswer(answersArray.join(", "));
                                                    finalQuestionList.add(new QuestionTemplate(question.getQuestion(), question.getUserAnswer()));
                                                    answeredQuestions.add(question);
                                                }
                                                break;
                                            }
                                        }
                                    }

                                    questionList.removeAll(answeredQuestions);

                                    questionIndex = 0;
                                    if (!questionList.isEmpty()) {
                                        speakQuestion();
                                    } else {
                                        Toast.makeText(HomeActivity.this, "Done", Toast.LENGTH_SHORT).show();
                                        speak("Thank you for your time!");
                                        stopListening();
                                        webView1.evaluateJavascript("javascript:stopListening()", value -> {});
                                        srError = false;
                                        isListening = false;
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(HomeActivity.this, "Error processing API response", Toast.LENGTH_SHORT).show();
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                String errorMessage = "Error connecting to API";

                                if (error.networkResponse != null) {
                                    try {
                                        String responseBody = new String(error.networkResponse.data, "UTF-8");
                                        JSONObject errorJson = new JSONObject(responseBody);
                                        if (errorJson.has("error")) {
                                            errorMessage = errorJson.getString("error");
                                        } else {
                                            errorMessage = responseBody;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        errorMessage = "Error parsing server response";
                                    }
                                } else if (error.getMessage() != null) {
                                    errorMessage = error.getMessage();
                                }

                                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                speak(errorMessage);
                                speechRecognizer.cancel();
                                stopListening();
                                srError = true;
                                isListening = false;
                                webView1.evaluateJavascript("javascript:stopListening()", value -> {});
                            }
                    );

                    Singleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

                } else {
                    // Prepare the JSON object to send to the API for non-"Open Response" questions
                    JSONObject requestData = new JSONObject();
                    try {
                        requestData.put("question", currentQuestion.getQuestion());
                        requestData.put("answer", answer);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Send the request to the API
                    String url = Utilities.DOMAIN + "/match-question-answer";
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            requestData,
                            response -> {
                                try {
                                    if (response.has("question") && response.has("answer")) {
                                        String matchedQuestion = response.getString("question");
                                        String matchedAnswer = response.getString("answer");

                                        for (int i = 0; i < questionList.size(); i++) {
                                            QuestionTemplate question = questionList.get(i);
                                            if (question.getQuestion().equals(matchedQuestion)) {
                                                question.setUserAnswer(matchedAnswer);
                                                finalQuestionList.add(new QuestionTemplate(matchedQuestion, matchedAnswer));

                                                questionList.remove(i);
                                                break;
                                            }
                                        }

                                        questionIndex = 0;
                                        if (!questionList.isEmpty()) {
                                            speakQuestion();
                                        } else {
                                            Toast.makeText(HomeActivity.this, "Done", Toast.LENGTH_SHORT).show();
                                            speak("Thank you for your time!");
                                            stopListening();
                                            webView1.evaluateJavascript("javascript:stopListening()", value -> {});
                                            srError = false;
                                            isListening = false;
                                        }
                                    } else {
                                        Toast.makeText(HomeActivity.this, "Unexpected response from API", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(HomeActivity.this, "Error processing API response", Toast.LENGTH_SHORT).show();
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                String errorMessage = "Error connecting to API";

                                if (error.networkResponse != null) {
                                    try {
                                        String responseBody = new String(error.networkResponse.data, "UTF-8");
                                        JSONObject errorJson = new JSONObject(responseBody);
                                        if (errorJson.has("error")) {
                                            errorMessage = errorJson.getString("error");
                                        } else {
                                            errorMessage = responseBody;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        errorMessage = "Error parsing server response";
                                    }
                                } else if (error.getMessage() != null) {
                                    errorMessage = error.getMessage();
                                }

                                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                speak(errorMessage);
                                speechRecognizer.cancel();
                                stopListening();
                                srError = true;
                                isListening = false;
                                webView1.evaluateJavascript("javascript:stopListening()", value -> {});
                            }
                    );

                    Singleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
                }
            } else {
                Toast.makeText(HomeActivity.this, "No speech recognized", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentDateTime() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return currentDateTime.format(formatter);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date());
        }
    }

    private class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
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

        @JavascriptInterface
        public void startListening() {
            if (ContextCompat.checkSelfPermission(HomeActivity.this, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
            } else {
                srError = false;
                if (processStartTime == null){
                    processStartTime = getCurrentDateTime();
                }
                runOnUiThread(() -> HomeActivity.this.speakQuestion());
            }
        }

        @JavascriptInterface
        public void stopListening() {
            runOnUiThread(() -> HomeActivity.this.stopListening());
        }

        @JavascriptInterface
        public void resetListening() {
            runOnUiThread(() -> HomeActivity.this.resetListening());
        }

        @JavascriptInterface
        public void concludeConversation() {
            processEndTime = getCurrentDateTime();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String user = sharedPreferences.getString("username", "None");
                    String startDate = processStartTime;
                    String endDate = processEndTime;
                    String date = getCurrentDateTime();
                    for (QuestionTemplate record:finalQuestionList){
                        String question = record.getQuestion();
                        String answer = record.getUserAnswer();

                        addAnswer(user, startDate, endDate, date, question, answer);
                    }

                    //callCorrectAnswersAPI(user, date);

                    speak("The conversation has been concluded successfully.");

                    webView1.evaluateJavascript("javascript:showSuccess()", value -> {
                    });
                }
            });
        }

        private void addAnswer(String username, String start_time, String end_time, String date, String question, String answer) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String urlStr = Utilities.DOMAIN + "/add_answer?username=" + URLEncoder.encode(username, "UTF-8") +
                                "&start_time=" + URLEncoder.encode(start_time, "UTF-8") +
                                "&end_time=" + URLEncoder.encode(end_time, "UTF-8") +
                                "&date=" + URLEncoder.encode(date, "UTF-8") +
                                "&question=" + URLEncoder.encode(question, "UTF-8") +
                                "&answer=" + URLEncoder.encode(answer, "UTF-8");

                        URL url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String inputLine;
                            StringBuffer response = new StringBuffer();

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
                            // Print the response (optional)
                            System.out.println(response.toString());
                        } else {
                            System.out.println("GET request not worked");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void callCorrectAnswersAPI(String username, String date) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(Utilities.DOMAIN + "/correct_answers");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; utf-8");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoOutput(true);

                        JSONObject json = new JSONObject();
                        json.put("username", username);
                        json.put("date", date);

                        OutputStream os = conn.getOutputStream();
                        byte[] input = json.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String inputLine;
                            StringBuffer response = new StringBuffer();

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
                            System.out.println(response.toString());
                        } else {
                            System.out.println("POST request not worked");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
