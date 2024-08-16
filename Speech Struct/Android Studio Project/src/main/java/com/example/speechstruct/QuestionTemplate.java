package com.example.speechstruct;

import com.google.gson.annotations.SerializedName;

public class QuestionTemplate {
    @SerializedName("category")
    private String category;

    @SerializedName("question_number")
    private String questionNumber;

    @SerializedName("question_media")
    private String questionMedia;

    @SerializedName("question")
    private String question;

    @SerializedName("response_type")
    private String responseType;

    @SerializedName("answer_number")
    private String answerNumber;

    @SerializedName("answer_including_media")
    private String answerIncludingMedia;

    @SerializedName("next_question")
    private String nextQuestion;

    @SerializedName("user_answer")
    private String userAnswer;

    public QuestionTemplate(String question,String userAnswer){
        this.question = question;
        this.userAnswer = userAnswer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionMedia() {
        return questionMedia;
    }

    public void setQuestionMedia(String questionMedia) {
        this.questionMedia = questionMedia;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getAnswerNumber() {
        return answerNumber;
    }

    public void setAnswerNumber(String answerNumber) {
        this.answerNumber = answerNumber;
    }

    public String getAnswerIncludingMedia() {
        return answerIncludingMedia;
    }

    public void setAnswerIncludingMedia(String answerIncludingMedia) {
        this.answerIncludingMedia = answerIncludingMedia;
    }

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getUserAnswer() {
        return userAnswer;
    }
}