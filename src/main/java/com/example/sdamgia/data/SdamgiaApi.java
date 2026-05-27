package com.example.sdamgia.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SdamgiaApi {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Set<String> excludeTopics = new HashSet<>(Arrays.asList(
        "логарифм", "производн", "интеграл", "вероятност",
        "комбинатор", "планиметр", "стереометр"
    ));

    public SdamgiaApi() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor(logging)
            .build();
    }

    public Future<List<CatalogItem>> fetchCatalogAsync() {
        return executor.submit(new Callable<List<CatalogItem>>() {
            @Override
            public List<CatalogItem> call() {
                return fetchCatalog();
            }
        });
    }

    public List<CatalogItem> fetchCatalog() {
        try {
            Request request = new Request.Builder()
                .url("https://sdamgia.ru/api/catalog/moodle/?subject=math")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

            Response response = client.newCall(request).execute();
            String body = response.body() != null ? response.body().string() : null;
            if (body == null) return getFallbackCatalog();

            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray items = json.getAsJsonArray("items");
            if (items == null) items = json.getAsJsonArray("catalog");
            if (items == null) items = json.getAsJsonArray("categories");

            List<CatalogItem> catalog = new ArrayList<>();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JsonObject obj = items.get(i).getAsJsonObject();
                    if (obj.get("id") == null || obj.get("name") == null) continue;
                    int id = obj.get("id").getAsInt();
                    String name = obj.get("name").getAsString();
                    int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
                    catalog.add(new CatalogItem(id, name, count));
                }
            }

            if (catalog.isEmpty()) return getFallbackCatalog();
            return catalog;
        } catch (Exception e) {
            return getFallbackCatalog();
        }
    }

    private List<CatalogItem> getFallbackCatalog() {
        return Arrays.asList(
            new CatalogItem(1, "Вычисления и преобразования", 50),
            new CatalogItem(2, "Простейшие уравнения", 50),
            new CatalogItem(3, "Текстовые задачи", 50),
            new CatalogItem(7, "Финансовая математика", 30),
            new CatalogItem(8, "Задачи с параметром", 20),
            new CatalogItem(10, "Графики функций", 30),
            new CatalogItem(11, "Наибольшее и наименьшее значение", 25)
        );
    }

    public Future<Result<ParsedProblem>> fetchProblemAsync(final String problemId) {
        return executor.submit(new Callable<Result<ParsedProblem>>() {
            @Override
            public Result<ParsedProblem> call() {
                return fetchProblem(problemId);
            }
        });
    }

    public Result<ParsedProblem> fetchProblem(final String problemId) {
        try {
            String url = "https://math-ege.sdamgia.ru/problem?id=" + problemId;
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

            Response response = client.newCall(request).execute();
            String html = response.body() != null ? response.body().string() : null;
            if (html == null) return Result.failure(new Exception("Empty response"));

            Document doc = Jsoup.parse(html);

            Element probDiv = doc.selectFirst("div.prob, div.problem, div.pbody, div.pbtm");
            String text;
            if (probDiv != null) {
                Element probText = probDiv.selectFirst("div.probtext, div.ptext, div.problem_text, div.pcond");
                text = probText != null ? probText.text() : probDiv.text();
            } else {
                text = doc.body() != null ? doc.body().text() : "";
            }

            Element answerEl = doc.selectFirst(
                ".answer, div.answer, span.answer, " +
                "div.prob div.answ, div.solution div.answ, " +
                "[class*=answer], [class*=answ], " +
                "div.AnsWrap"
            );
            String answer = sanitizeAnswer(
                answerEl != null ? answerEl.text() : extractAnswerFromHtml(html)
            );

            Element solutionEl = doc.selectFirst(
                "div.solution, div.soln, div.prob div.solution, " +
                "div.pSolution, [class*=solution]"
            );
            String solution = solutionEl != null && !solutionEl.text().isBlank()
                ? solutionEl.text() : "Решение не найдено";

            return Result.success(new ParsedProblem(problemId, text, answer, solution));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    public Future<Result<ParsedHtmlResult>> fetchProblemHtmlAsync(final String problemId) {
        return executor.submit(new Callable<Result<ParsedHtmlResult>>() {
            @Override
            public Result<ParsedHtmlResult> call() {
                return fetchProblemHtml(problemId);
            }
        });
    }

    public Result<ParsedHtmlResult> fetchProblemHtml(String problemId) {
        try {
            String url = "https://math-ege.sdamgia.ru/problem?id=" + problemId + "&print=true";
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

            Response response = client.newCall(request).execute();
            String html = response.body() != null ? response.body().string() : null;
            if (html == null) return Result.failure(new Exception("Empty"));

            Document doc = Jsoup.parse(html);

            doc.select("script, iframe, nav, header, footer, .menu, " +
                ".header, .footer, .banner, .ad, .ads, .advert, " +
                ".social, .share, style, link, meta").remove();

            Element probMainDiv = doc.selectFirst("div.prob_maindiv, div.prob, div.problem");
            Element pbodyEl = probMainDiv != null
                ? probMainDiv.selectFirst("div.pbody, div.probtext, div.pcond, div.ptext")
                : null;

            String text;
            if (pbodyEl != null) text = pbodyEl.text();
            else if (probMainDiv != null) text = probMainDiv.text();
            else text = doc.body() != null ? doc.body().text() : "";

            Element answerEl = doc.selectFirst(
                ".answer, div.answer, span.answer, div.prob div.answ, [class*=answer], div.AnsWrap"
            );
            String answer = sanitizeAnswer(
                answerEl != null ? answerEl.text() : extractAnswerFromHtml(html)
            );

            Element solutionEl = doc.selectFirst(
                "div.solution, div.soln, div.pSolution, [class*=solution]"
            );
            String solution = solutionEl != null && !solutionEl.text().isBlank()
                ? solutionEl.text() : "Решение не найдено";

            String cleanBody;
            if (pbodyEl != null) {
                cleanBody = pbodyEl.html();
            } else if (probMainDiv != null) {
                Element clone = probMainDiv.clone();
                clone.select("div.solution, div.answer, .solution, .answer, " +
                    "div.prob_ans, .prob_nums, .briefcase, " +
                    "img[src*=star], img[src*=briefcase], " +
                    "img[src*=exclamation], img[src*=chain], " +
                    "img[src*=printer], .nocopy, " +
                    "div[style*=display:none], " +
                    "div.align-left, style").remove();
                cleanBody = clone.html();
            } else {
                cleanBody = text;
            }

            String cleanHtml = buildCleanProblemHtml(cleanBody);

            return Result.success(new ParsedHtmlResult(
                cleanHtml,
                new ParsedProblem(problemId, text, answer, solution)
            ));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    private String buildCleanProblemHtml(String content) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <style>\n" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "        body {\n" +
            "            font-family: -apple-system, 'Noto Serif', serif;\n" +
            "            font-size: 16px;\n" +
            "            line-height: 1.7;\n" +
            "            padding: 8px;\n" +
            "            color: #1a1a1a;\n" +
            "            background: transparent;\n" +
            "        }\n" +
            "        img { max-width: 100%; height: auto; margin: 6px 0; }\n" +
            "        .pbody p { margin: 6px 0; }\n" +
            "        .left_margin { margin-left: 0; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>" + content + "</body>\n" +
            "</html>";
    }

    public Future<String> getRandomProblemIdAsync() {
        return executor.submit(new Callable<String>() {
            @Override
            public String call() {
                return getRandomProblemId();
            }
        });
    }

    public String getRandomProblemId() {
        try {
            List<CatalogItem> catalog = fetchCatalog();
            if (catalog.isEmpty()) return getFallbackId();

            List<CatalogItem> filtered = new ArrayList<>();
            for (CatalogItem item : catalog) {
                if (item.getId() < 13) {
                    boolean excluded = false;
                    for (String keyword : excludeTopics) {
                        if (item.getName().toLowerCase().contains(keyword)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (!excluded) filtered.add(item);
                }
            }

            if (filtered.isEmpty()) return getFallbackId();

            CatalogItem topic = filtered.get((int) (Math.random() * filtered.size()));
            String topicUrl = "https://math-ege.sdamgia.ru/test?theme=" + topic.getId();

            Request request = new Request.Builder()
                .url(topicUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

            Response response = client.newCall(request).execute();
            String html = response.body() != null ? response.body().string() : null;
            if (html == null) return getFallbackId();

            Document doc = Jsoup.parse(html);
            Pattern idPattern = Pattern.compile("problem\\?id=(\\d+)");
            List<String> ids = new ArrayList<>();
            for (Element link : doc.select("a[href*=\"problem?id=\"]")) {
                String href = link.attr("href");
                Matcher m = idPattern.matcher(href);
                if (m.find()) {
                    String id = m.group(1);
                    if (!ids.contains(id)) ids.add(id);
                }
            }

            if (ids.isEmpty()) return getFallbackId();
            return ids.get((int) (Math.random() * ids.size()));
        } catch (Exception e) {
            return getFallbackId();
        }
    }

    private String getFallbackId() {
        String[] fallback = {"506954", "506380", "506314", "506245", "506179"};
        return fallback[(int) (Math.random() * fallback.length)];
    }

    private String sanitizeAnswer(String answer) {
        if (answer == null) return "";
        String trimmed = answer.trim();
        trimmed = trimmed.replaceFirst("(?i)^Ответ\\s*[:\\s]", "").trim();
        if (trimmed.isBlank() || trimmed.equals("-") ||
            trimmed.equalsIgnoreCase("нет") ||
            trimmed.equalsIgnoreCase("нет ответа")) return "";
        return trimmed;
    }

    private String extractAnswerFromHtml(String html) {
        if (html == null) return null;
        Pattern[] patterns = {
            Pattern.compile("Ответ:\\s*[\\\\]?[\\(]?\\s*(.*?)\\s*[\\\\]?[\\)]?"),
            Pattern.compile("answer['\"]?\\s*[:=]\\s*['\"](.*?)['\"]")
        };
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(html);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }

    public String normalizeAnswer(String answer) {
        return answer
            .replace(",", ".")
            .trim()
            .toLowerCase()
            .replaceAll("\\s+", "");
    }

    public boolean checkAnswer(String userAnswer, String correctAnswer) {
        String normalized = normalizeAnswer(userAnswer);
        String normalizedCorrect = normalizeAnswer(correctAnswer);
        return normalized.equals(normalizedCorrect) ||
               normalized.equals(normalizedCorrect.replace(".", ",")) ||
               normalizedCorrect.equals(normalized.replace(".", ","));
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static class ParsedHtmlResult {
        private final String html;
        private final ParsedProblem problem;

        public ParsedHtmlResult(String html, ParsedProblem problem) {
            this.html = html;
            this.problem = problem;
        }

        public String getHtml() { return html; }
        public ParsedProblem getProblem() { return problem; }
    }

    public static class Result<T> {
        private final T value;
        private final Exception error;
        private final boolean isSuccess;

        private Result(T value, Exception error, boolean isSuccess) {
            this.value = value;
            this.error = error;
            this.isSuccess = isSuccess;
        }

        public static <T> Result<T> success(T value) {
            return new Result<>(value, null, true);
        }

        public static <T> Result<T> failure(Exception error) {
            return new Result<>(null, error, false);
        }

        public boolean isSuccess() { return isSuccess; }
        public boolean isFailure() { return !isSuccess; }
        public T getOrNull() { return value; }
        public Exception exceptionOrNull() { return error; }
        public T getOrThrow() throws Exception {
            if (!isSuccess) throw error;
            return value;
        }
    }
}
