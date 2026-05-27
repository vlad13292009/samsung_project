package com.example.sdamgia.model;

public class ProblemData {
    private String id;
    private String text;
    private String answer;
    private String solution;
    private String html;

    public ProblemData() {
        this.id = "";
        this.text = "";
        this.answer = "";
        this.solution = "";
        this.html = "";
    }

    public ProblemData(String id, String text, String answer, String solution, String html) {
        this.id = id;
        this.text = text;
        this.answer = answer;
        this.solution = solution;
        this.html = html;
    }

    public ProblemData(ProblemData other) {
        this.id = other.id;
        this.text = other.text;
        this.answer = other.answer;
        this.solution = other.solution;
        this.html = other.html;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
}
