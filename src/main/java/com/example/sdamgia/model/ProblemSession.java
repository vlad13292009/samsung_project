package com.example.sdamgia.model;

public class ProblemSession {
    private ProblemData problem;
    private int attempts;
    private int maxAttempts;
    private boolean isSolved;
    private boolean isLoading;
    private String error;
    private boolean solutionShown;

    public ProblemSession() {
        this.problem = null;
        this.attempts = 0;
        this.maxAttempts = 2;
        this.isSolved = false;
        this.isLoading = false;
        this.error = null;
        this.solutionShown = false;
    }

    public ProblemSession(ProblemData problem, int attempts, int maxAttempts,
                          boolean isSolved, boolean isLoading, String error, boolean solutionShown) {
        this.problem = problem;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.isSolved = isSolved;
        this.isLoading = isLoading;
        this.error = error;
        this.solutionShown = solutionShown;
    }

    public ProblemSession(ProblemSession other) {
        this.problem = other.problem != null ? new ProblemData(other.problem) : null;
        this.attempts = other.attempts;
        this.maxAttempts = other.maxAttempts;
        this.isSolved = other.isSolved;
        this.isLoading = other.isLoading;
        this.error = other.error;
        this.solutionShown = other.solutionShown;
    }

    public ProblemData getProblem() { return problem; }
    public void setProblem(ProblemData problem) { this.problem = problem; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public boolean isSolved() { return isSolved; }
    public void setSolved(boolean solved) { isSolved = solved; }
    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public boolean isSolutionShown() { return solutionShown; }
    public void setSolutionShown(boolean solutionShown) { this.solutionShown = solutionShown; }
}
