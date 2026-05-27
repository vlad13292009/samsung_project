package com.example.sdamgia.data;

import android.content.Context;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProblemCache {

    private final Gson gson = new Gson();
    private final File cacheDir;

    public ProblemCache(Context context) {
        this.cacheDir = new File(context.getCacheDir(), "problem_cache");
        this.cacheDir.mkdirs();
    }

    public CachedProblem get(String id) {
        File file = new File(cacheDir, id + ".json");
        if (!file.exists()) return null;
        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return gson.fromJson(content, CachedProblem.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void put(CachedProblem problem) {
        try {
            String json = gson.toJson(problem);
            java.nio.file.Files.write(
                new File(cacheDir, problem.getId() + ".json").toPath(),
                json.getBytes()
            );
        } catch (Exception ignored) {}
    }

    public List<String> getAllIds() {
        File[] files = cacheDir.listFiles();
        if (files == null) return Collections.emptyList();
        List<String> ids = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().replace(".json", "");
            if (name.matches("\\d+")) ids.add(name);
        }
        return ids;
    }

    public CachedProblem getRandom(Set<String> exclude) {
        List<String> ids = getAllIds();
        List<String> filtered = new ArrayList<>();
        for (String id : ids) {
            if (!exclude.contains(id)) filtered.add(id);
        }
        if (filtered.isEmpty()) return null;
        Collections.shuffle(filtered);
        for (String id : filtered) {
            CachedProblem p = get(id);
            if (p != null && p.getAnswer() != null && !p.getAnswer().isBlank()) return p;
        }
        return null;
    }

    public void delete(String id) {
        new File(cacheDir, id + ".json").delete();
    }

    public int getCacheSizeMb() {
        File[] files = cacheDir.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".json")) {
                total += f.length();
            }
        }
        return (int) (total / (1024 * 1024));
    }

    public void enforceMaxSize(int maxMb) {
        long maxBytes = maxMb * 1024L * 1024L;
        File[] filesArr = cacheDir.listFiles();
        if (filesArr == null) return;
        List<File> files = new ArrayList<>();
        for (File f : filesArr) {
            if (f.isFile() && f.getName().endsWith(".json")) files.add(f);
        }
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= maxBytes) return;
        files.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= maxBytes) break;
            total -= f.length();
            f.delete();
        }
    }

    public void clear() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
    }
}
