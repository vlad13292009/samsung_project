package com.example.sdamgia.data;

public class CachedProblem {
    private final String id;
    private final String answer;
    private final String text;
    private final String solution;
    private final String html;
    private final long cachedAt;

    public CachedProblem(String id, String answer, String text, String solution, String html) {
        this.id = id;
        this.answer = answer;
        this.text = text;
        this.solution = solution;
        this.html = html;
        this.cachedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAnswer() { return answer; }
    public String getText() { return text; }
    public String getSolution() { return solution; }
    public String getHtml() { return html; }
    public long getCachedAt() { return cachedAt; }
}
