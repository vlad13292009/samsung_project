package com.example.sdamgia.data;

public class ParsedProblem {
    private final String id;
    private final String text;
    private final String answer;
    private final String solution;

    public ParsedProblem(String id, String text, String answer, String solution) {
        this.id = id;
        this.text = text;
        this.answer = answer;
        this.solution = solution;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getAnswer() { return answer; }
    public String getSolution() { return solution; }
}
