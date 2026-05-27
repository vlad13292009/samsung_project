package com.example.sdamgia.viewmodel;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sdamgia.data.CachedProblem;
import com.example.sdamgia.data.NotificationHelper;
import com.example.sdamgia.data.ParsedProblem;
import com.example.sdamgia.data.PreferencesManager;
import com.example.sdamgia.data.ProblemCache;
import com.example.sdamgia.data.SdamgiaApi;
import com.example.sdamgia.data.SoundManager;
import com.example.sdamgia.model.AnimationState;
import com.example.sdamgia.model.GameState;
import com.example.sdamgia.model.ProblemData;
import com.example.sdamgia.model.ProblemSession;
import com.example.sdamgia.model.ShopItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameViewModel extends AndroidViewModel {

    public static final int LOW_STAT_THRESHOLD = 31;
    public static final long ANIM_EATING_DURATION = 6000L;
    public static final long ANIM_PLAYING_DURATION = 6000L;
    public static final long ANIM_BATHING_DURATION = 5000L;
    public static final int SHOP_MIN_LEVEL = 5;

    private final PreferencesManager preferencesManager;
    private final SdamgiaApi api;
    private final SoundManager soundManager;
    private final NotificationHelper notificationHelper;
    private final ProblemCache problemCache;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<GameState> _gameState = new MutableLiveData<>(new GameState());
    public LiveData<GameState> getGameState() { return _gameState; }

    private final MutableLiveData<ProblemSession> _problemSession = new MutableLiveData<>(new ProblemSession());
    public LiveData<ProblemSession> getProblemSession() { return _problemSession; }

    private final MutableLiveData<Boolean> _isBlinking = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsBlinking() { return _isBlinking; }

    private final MutableLiveData<Integer> _blinkColor = new MutableLiveData<>(null);
    public LiveData<Integer> getBlinkColor() { return _blinkColor; }

    private final MutableLiveData<Boolean> _showWakeConfirmDialog = new MutableLiveData<>(false);
    public LiveData<Boolean> getShowWakeConfirmDialog() { return _showWakeConfirmDialog; }

    private final Map<String, Integer> statColors = new HashMap<>();

    public GameViewModel(Application application) {
        super(application);

        preferencesManager = new PreferencesManager(application);
        api = new SdamgiaApi();
        soundManager = new SoundManager(application);
        notificationHelper = new NotificationHelper(application);
        problemCache = new ProblemCache(application);

        statColors.put("hunger", 0x80FF7043);
        statColors.put("happiness", 0x80FFD54F);
        statColors.put("hygiene", 0x804DD0E1);
        statColors.put("energy", 0x8081C784);

        Log.w("SDAMGIA_DEBUG", "GameViewModel инициализирован");
        notificationHelper.createChannel();

        mainHandler.post(() -> {
            GameState saved = preferencesManager.loadGameState();
            long now = System.currentTimeMillis();
            GameState updated = applyDecay(saved, now);
            updated = checkAutoSleepWake(updated);
            updated = updateAnimationState(updated);
            updated.setSoundEnabled(saved.isSoundEnabled());
            updated.setMusicEnabled(saved.isMusicEnabled());
            updated.setMusicVolume(saved.getMusicVolume());
            _gameState.setValue(updated);
            preferencesManager.saveGameState(_gameState.getValue());
            if (updated.isSleeping()) soundManager.playSleepSound();
            applyStateFromSaved();
        });

        scheduler.scheduleAtFixedRate(() -> {
            GameState current = _gameState.getValue();
            if (current == null) return;
            long now = System.currentTimeMillis();
            GameState updated = new GameState(current);
            updated = applyDecay(updated, now);
            updated = checkAutoSleepWake(updated);
            updated = updateAnimationState(updated);
            updated.setLowStats(detectLowStats(updated));
            final boolean wasSleeping = current.isSleeping();
            final boolean nowSleeping = updated.isSleeping();
            final GameState finalUpdated = new GameState(updated);
            mainHandler.post(() -> {
                _gameState.setValue(finalUpdated);
                preferencesManager.saveGameState(finalUpdated);
                if (nowSleeping != wasSleeping) {
                    if (nowSleeping) soundManager.playSleepSound();
                    else soundManager.stopSleepSound();
                }
                applyStateFromSaved();
            });
        }, 5000, 5000, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            GameState state = _gameState.getValue();
            if (state != null && !state.isDead() && !state.getLowStats().isEmpty()) {
                List<String> sorted = new ArrayList<>(state.getLowStats());
                java.util.Collections.sort(sorted);
                int index = (int) ((System.currentTimeMillis() / 500) % sorted.size());
                Integer color = statColors.get(sorted.get(index));
                mainHandler.post(() -> {
                    _isBlinking.setValue(!Boolean.TRUE.equals(_isBlinking.getValue()));
                    _blinkColor.setValue(color);
                });
            } else {
                mainHandler.post(() -> {
                    _isBlinking.setValue(false);
                    _blinkColor.setValue(null);
                });
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            GameState state = _gameState.getValue();
            if (state != null && !state.isDead() && !state.getLowStats().isEmpty()) {
                String first = state.getLowStats().iterator().next();
                int value = 0;
                switch (first) {
                    case "hunger": value = (int) state.getHunger(); break;
                    case "happiness": value = (int) state.getHappiness(); break;
                    case "hygiene": value = (int) state.getHygiene(); break;
                    case "energy": value = (int) state.getEnergy(); break;
                }
                notificationHelper.notifyLowStat(first, value);
            }
        }, 60000, 60000, TimeUnit.MILLISECONDS);
    }

    private void applyStateFromSaved() {
        GameState state = _gameState.getValue();
        if (state == null) return;
        soundManager.setSoundEffectsEnabled(state.isSoundEnabled());
        soundManager.setMusicEnabled(state.isMusicEnabled());
        soundManager.setMusicVolume(state.getMusicVolume());
        if (state.isMusicEnabled()) soundManager.playMusic();
        else soundManager.pauseMusic();
    }

    @Override
    public void onCleared() {
        super.onCleared();
        soundManager.release();
        scheduler.shutdown();
        api.shutdown();
    }

    private GameState applyDecay(GameState state, long now) {
        float elapsed = (now - state.getLastUpdateTime()) / 60000f;
        if (elapsed <= 0 || state.isDead()) return state;

        float hungerDecayMod = state.getPurchasedUpgrades().contains(3) ? 1f / 1.5f : 1f;
        float happinessDecayMod = state.getPurchasedUpgrades().contains(1) ? 0.5f : 1f;
        float hygieneDecayMod = state.getPurchasedUpgrades().contains(4) ? 1f / 1.5f : 1f;
        float energyDecayMod = state.getPurchasedUpgrades().contains(2) ? 1f / 1.5f : 1f;

        float h = state.getHunger();
        float ha = state.getHappiness();
        float hy = state.getHygiene();
        float e = state.getEnergy();

        if (state.isSleeping()) {
            h -= elapsed * hungerDecayMod * 0.1f;
            e += elapsed * (state.getPurchasedUpgrades().contains(7) ? 15f : 10f);
        } else {
            h -= elapsed * 1f * hungerDecayMod;
            ha -= elapsed * 1.5f * happinessDecayMod;
            hy -= elapsed * 1f * hygieneDecayMod;
            e -= elapsed * 1.5f * energyDecayMod;
        }

        h = clamp(h, 0f, 100f);
        ha = clamp(ha, 0f, 100f);
        hy = clamp(hy, 0f, 100f);
        e = clamp(e, 0f, 100f);

        boolean dead = h <= 0f || ha <= 0f || hy <= 0f || e <= 0f;

        if (dead && !state.isDead()) {
            soundManager.playSound("pet_die");
            soundManager.vibrate(500);
            notificationHelper.notifyDeath();
        }

        GameState result = new GameState(state);
        result.setHunger(h);
        result.setHappiness(ha);
        result.setHygiene(hy);
        result.setEnergy(e);
        result.setDead(dead);
        result.setLowStats(detectLowStats(result));
        result.setLastUpdateTime(now);
        return result;
    }

    private Set<String> detectLowStats(GameState state) {
        Set<String> low = new HashSet<>();
        if (state.getHunger() <= LOW_STAT_THRESHOLD) low.add("hunger");
        if (state.getHappiness() <= LOW_STAT_THRESHOLD) low.add("happiness");
        if (state.getHygiene() <= LOW_STAT_THRESHOLD) low.add("hygiene");
        if (state.getEnergy() <= LOW_STAT_THRESHOLD) low.add("energy");
        return low;
    }

    private GameState checkAutoSleepWake(GameState state) {
        if (state.isDead()) return state;
        GameState s = new GameState(state);
        if (!s.isSleeping() && s.getEnergy() < 30 && s.getAnimationState() == AnimationState.IDLE) {
            s.setSleeping(true);
            s.setAnimationState(AnimationState.SLEEPING);
            s.setAnimStartTime(System.currentTimeMillis());
        }
        if (s.isSleeping() && s.getEnergy() >= 90) {
            s.setSleeping(false);
            s.setAnimationState(AnimationState.IDLE);
            s.setAnimStartTime(System.currentTimeMillis());
        }
        return s;
    }

    private GameState updateAnimationState(GameState state) {
        if (state.isDead()) {
            GameState s = new GameState(state);
            s.setAnimationState(AnimationState.DEAD);
            return s;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - state.getAnimStartTime();
        GameState s = new GameState(state);

        switch (state.getAnimationState()) {
            case EATING:
                if (elapsed >= ANIM_EATING_DURATION) {
                    s.setAnimationState(AnimationState.IDLE);
                    s.setAnimStartTime(now);
                }
                break;
            case PLAYING:
                if (elapsed >= ANIM_PLAYING_DURATION) {
                    s.setAnimationState(AnimationState.IDLE);
                    s.setAnimStartTime(now);
                }
                break;
            case BATHING:
                if (elapsed >= ANIM_BATHING_DURATION) {
                    s.setAnimationState(AnimationState.IDLE);
                    s.setAnimStartTime(now);
                }
                break;
            case SLEEPING:
                if (!s.isSleeping()) {
                    s.setAnimationState(AnimationState.IDLE);
                    s.setAnimStartTime(now);
                }
                break;
            default:
                if (s.isSleeping()) {
                    s.setAnimationState(AnimationState.SLEEPING);
                    s.setAnimStartTime(now);
                }
                break;
        }
        return s;
    }

    public void feed() {
        GameState state = _gameState.getValue();
        if (state == null || state.isDead() || state.isSleeping() || state.getHunger() >= 100) return;
        soundManager.playSound("feed");
        float amount = 30f;
        if (state.getPurchasedUpgrades().contains(0)) amount *= 1.1f;
        if (state.getPurchasedUpgrades().contains(5)) amount *= 1.1f;
        float newHunger = Math.min(state.getHunger() + amount, 100f);
        GameState newState = new GameState(state);
        newState.setHunger(newHunger);
        newState.setAnimationState(AnimationState.EATING);
        newState.setAnimStartTime(System.currentTimeMillis());
        updateState(newState);
    }

    public void play() {
        GameState state = _gameState.getValue();
        if (state == null || state.isDead() || state.isSleeping() || state.getEnergy() <= 20) return;
        soundManager.playSound("play");
        float hGain = 25f;
        if (state.getPurchasedUpgrades().contains(6)) hGain *= 1.15f;
        GameState newState = new GameState(state);
        newState.setHappiness(Math.min(state.getHappiness() + hGain, 100f));
        newState.setEnergy(Math.max(state.getEnergy() - 20f, 0f));
        newState.setHygiene(Math.max(state.getHygiene() - 10f, 0f));
        newState.setHunger(Math.max(state.getHunger() - 15f, 0f));
        newState.setAnimationState(AnimationState.PLAYING);
        newState.setAnimStartTime(System.currentTimeMillis());
        updateState(newState);
    }

    public void bathe() {
        GameState state = _gameState.getValue();
        if (state == null || state.isDead() || state.isSleeping()) return;
        soundManager.playSound("bath");
        GameState newState = new GameState(state);
        newState.setHygiene(100f);
        newState.setHappiness(Math.min(state.getHappiness() + 10f, 100f));
        newState.setAnimationState(AnimationState.BATHING);
        newState.setAnimStartTime(System.currentTimeMillis());
        updateState(newState);
    }

    public void toggleSleep() {
        GameState state = _gameState.getValue();
        if (state == null || state.isDead()) return;
        long now = System.currentTimeMillis();
        if (state.isSleeping()) {
            if (state.getEnergy() >= 60) {
                soundManager.playSound("wake_up");
                GameState newState = new GameState(state);
                newState.setSleeping(false);
                newState.setAnimationState(AnimationState.IDLE);
                newState.setAnimStartTime(now);
                updateState(newState);
            } else if (state.getEnergy() >= 20) {
                _showWakeConfirmDialog.setValue(true);
            }
        } else {
            if (state.getAnimationState() != AnimationState.IDLE && state.getAnimationState() != AnimationState.SLEEPING) return;
            GameState newState = new GameState(state);
            newState.setSleeping(true);
            newState.setAnimationState(AnimationState.SLEEPING);
            newState.setAnimStartTime(now);
            updateState(newState);
        }
    }

    public void confirmWake() {
        GameState state = _gameState.getValue();
        if (state == null) return;
        _showWakeConfirmDialog.setValue(false);
        soundManager.playSound("wake_up");
        GameState newState = new GameState(state);
        newState.setSleeping(false);
        newState.setAnimationState(AnimationState.IDLE);
        newState.setHappiness(Math.max(state.getHappiness() - 15f, 0f));
        newState.setAnimStartTime(System.currentTimeMillis());
        updateState(newState);
    }

    public void cancelWake() {
        _showWakeConfirmDialog.setValue(false);
    }

    public void revive() {
        GameState state = _gameState.getValue();
        if (state == null || !state.isDead()) return;
        long now = System.currentTimeMillis();
        GameState newState = new GameState(state);
        newState.setHunger(80f);
        newState.setHappiness(80f);
        newState.setHygiene(80f);
        newState.setEnergy(80f);
        newState.setDead(false);
        newState.setSleeping(false);
        newState.setAnimationState(AnimationState.IDLE);
        newState.setAnimStartTime(now);
        newState.setLowStats(new HashSet<>());
        updateState(newState);
    }

    public void renamePet(String name) {
        if (name.isBlank()) return;
        GameState state = _gameState.getValue();
        if (state == null) return;
        GameState newState = new GameState(state);
        newState.setPetName(name.trim());
        updateState(newState);
    }

    public void toggleSoundEffects() {
        GameState state = _gameState.getValue();
        if (state == null) return;
        boolean enabled = !state.isSoundEnabled();
        soundManager.setSoundEffectsEnabled(enabled);
        GameState newState = new GameState(state);
        newState.setSoundEnabled(enabled);
        updateState(newState);
    }

    public void toggleMusic() {
        GameState state = _gameState.getValue();
        if (state == null) return;
        boolean enabled = !state.isMusicEnabled();
        soundManager.setMusicEnabled(enabled);
        if (enabled) soundManager.playMusic();
        else soundManager.pauseMusic();
        GameState newState = new GameState(state);
        newState.setMusicEnabled(enabled);
        updateState(newState);
    }

    public void setMusicVolume(float volume) {
        soundManager.setMusicVolume(volume);
        GameState state = _gameState.getValue();
        if (state == null) return;
        GameState newState = new GameState(state);
        newState.setMusicVolume(volume);
        updateState(newState);
    }

    public boolean isOverlayEnabled() {
        GameState state = _gameState.getValue();
        return state != null && state.isOverlayEnabled();
    }

    public void toggleOverlay() {
        GameState state = _gameState.getValue();
        if (state == null) return;
        GameState newState = new GameState(state);
        newState.setOverlayEnabled(!state.isOverlayEnabled());
        updateState(newState);
    }

    public void setOverlayEnabled(boolean enabled) {
        GameState state = _gameState.getValue();
        if (state == null) return;
        if (state.isOverlayEnabled() != enabled) {
            GameState newState = new GameState(state);
            newState.setOverlayEnabled(enabled);
            updateState(newState);
        }
    }

    public int getCacheSizeMb() {
        return problemCache.getCacheSizeMb();
    }

    public void setMaxCacheSizeMb(int size) {
        GameState state = _gameState.getValue();
        if (state == null) return;
        GameState newState = new GameState(state);
        newState.setMaxCacheSizeMb(size);
        updateState(newState);
        problemCache.enforceMaxSize(size);
    }

    public boolean isShopAvailable() {
        GameState state = _gameState.getValue();
        return state != null && state.getLevel() >= SHOP_MIN_LEVEL;
    }

    private void updateState(GameState newState) {
        GameState oldState = _gameState.getValue();
        if (oldState != null && newState.isSleeping() != oldState.isSleeping()) {
            if (newState.isSleeping()) soundManager.playSleepSound();
            else soundManager.stopSleepSound();
        }
        newState.setLastUpdateTime(System.currentTimeMillis());
        _gameState.setValue(newState);
        saveState(newState);
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                getApplication().getSystemService(Application.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            android.net.Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            if (caps == null) return false;
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (SecurityException e) {
            return true;
        }
    }

    private void addRecentProblemId(String id) {
        GameState state = _gameState.getValue();
        if (state == null) return;
        GameState newState = new GameState(state);
        List<String> recent = new ArrayList<>();
        recent.add(id);
        recent.addAll(state.getRecentProblemIds());
        if (recent.size() > 20) recent = recent.subList(0, 20);
        newState.setRecentProblemIds(recent);
        updateState(newState);
    }

    public void startSolving() {
        try {
            GameState state = _gameState.getValue();
            if (state == null) return;
            Log.w("SDAMGIA_DEBUG", "startSolving() | isDead=" + state.isDead() + " | isSleeping=" + state.isSleeping());
            if (state.isDead() || state.isSleeping()) return;

            ProblemSession loadingSession = new ProblemSession();
            loadingSession.setLoading(true);
            _problemSession.setValue(loadingSession);

            if (!isOnline()) {
                Log.w("SDAMGIA_DEBUG", "Нет интернета — пытаемся из кеша");
                CachedProblem cached = problemCache.getRandom(new HashSet<>(state.getRecentProblemIds()));
                if (cached != null) {
                    Log.w("SDAMGIA_DEBUG", "Оффлайн: загружено #" + cached.getId() + " из кеша");
                    addRecentProblemId(cached.getId());
                    ProblemSession session = new ProblemSession();
                    session.setProblem(new ProblemData(cached.getId(), cached.getText(),
                        cached.getAnswer(), cached.getSolution(), cached.getHtml()));
                    session.setLoading(false);
                    _problemSession.setValue(session);
                } else {
                    ProblemSession session = new ProblemSession();
                    session.setLoading(false);
                    session.setError("Нет интернета и нет сохранённых задач");
                    _problemSession.setValue(session);
                }
                return;
            }

            Thread executorThread = new Thread(() -> {
                try {
                    GameState currentState = _gameState.getValue();
                    Set<String> recentExclude = new HashSet<>(currentState.getRecentProblemIds());

                    for (int attempt = 1; attempt <= 5; attempt++) {
                        String problemId = api.getRandomProblemId();
                        Log.w("SDAMGIA_DEBUG", "Попытка " + attempt + ", ID задачи: " + problemId);

                        if (recentExclude.contains(problemId)) {
                            Log.w("SDAMGIA_DEBUG", "ID " + problemId + " уже недавно был");
                            CachedProblem cached = problemCache.getRandom(recentExclude);
                            if (cached != null) {
                                final String fId = cached.getId();
                                final String fText = cached.getText();
                                final String fAnswer = cached.getAnswer();
                                final String fSolution = cached.getSolution();
                                final String fHtml = cached.getHtml();
                                mainHandler.post(() -> {
                                    addRecentProblemId(fId);
                                    ProblemSession session = new ProblemSession();
                                    session.setProblem(new ProblemData(fId, fText, fAnswer, fSolution, fHtml));
                                    session.setLoading(false);
                                    _problemSession.setValue(session);
                                });
                                return;
                            }
                            recentExclude.add(problemId);
                            continue;
                        }

                        CachedProblem cached = problemCache.get(problemId);
                        if (cached != null) {
                            if (cached.getAnswer() == null || cached.getAnswer().isBlank()) {
                                Log.w("SDAMGIA_DEBUG", "Задача #" + problemId + " в кеше без ответа — удаляем");
                                problemCache.delete(problemId);
                                recentExclude.add(problemId);
                                continue;
                            }
                            Log.w("SDAMGIA_DEBUG", "Загружено из кеша #" + problemId + " | Ответ: " + cached.getAnswer());
                            final String fId = problemId;
                            final String fText = cached.getText();
                            final String fAnswer = cached.getAnswer();
                            final String fSolution = cached.getSolution();
                            final String fHtml = cached.getHtml();
                            mainHandler.post(() -> {
                                addRecentProblemId(fId);
                                ProblemSession session = new ProblemSession();
                                session.setProblem(new ProblemData(fId, fText, fAnswer, fSolution, fHtml));
                                session.setLoading(false);
                                _problemSession.setValue(session);
                            });
                            return;
                        }

                        SdamgiaApi.Result<SdamgiaApi.ParsedHtmlResult> htmlResult = api.fetchProblemHtml(problemId);
                        if (htmlResult.isFailure()) {
                            Log.w("SDAMGIA_DEBUG", "Ошибка HTML: " +
                                (htmlResult.exceptionOrNull() != null ? htmlResult.exceptionOrNull().getMessage() : "unknown"));
                            CachedProblem c2 = problemCache.getRandom(recentExclude);
                            if (c2 != null) {
                                final String fId = c2.getId();
                                final String fText = c2.getText();
                                final String fAnswer = c2.getAnswer();
                                final String fSolution = c2.getSolution();
                                final String fHtml = c2.getHtml();
                                mainHandler.post(() -> {
                                    addRecentProblemId(fId);
                                    ProblemSession session = new ProblemSession();
                                    session.setProblem(new ProblemData(fId, fText, fAnswer, fSolution, fHtml));
                                    session.setLoading(false);
                                    _problemSession.setValue(session);
                                });
                            } else {
                                mainHandler.post(() -> {
                                    ProblemSession session = new ProblemSession();
                                    session.setLoading(false);
                                    session.setError("Ошибка загрузки задачи: " +
                                        (htmlResult.exceptionOrNull() != null ? htmlResult.exceptionOrNull().getMessage() : "unknown"));
                                    _problemSession.setValue(session);
                                });
                            }
                            return;
                        }

                        SdamgiaApi.ParsedHtmlResult result = htmlResult.getOrThrow();
                        ParsedProblem parsed = result.getProblem();
                        Log.w("SDAMGIA_DEBUG", "Задача #" + problemId + " | Ответ: \"" + parsed.getAnswer() + "\"");

                        if (parsed.getAnswer().isBlank()) {
                            Log.w("SDAMGIA_DEBUG", "Задача #" + problemId + " без ответа — пробуем другую");
                            recentExclude.add(problemId);
                            continue;
                        }

                        final String fId = problemId;
                        final String fText = parsed.getText();
                        final String fAnswer = parsed.getAnswer();
                        final String fSolution = parsed.getSolution();
                        final String fHtml = result.getHtml();

                        problemCache.put(new CachedProblem(fId, fAnswer, fText, fSolution, fHtml));
                        problemCache.enforceMaxSize(_gameState.getValue().getMaxCacheSizeMb());

                        mainHandler.post(() -> {
                            addRecentProblemId(fId);
                            ProblemSession session = new ProblemSession();
                            session.setProblem(new ProblemData(fId, fText, fAnswer, fSolution, fHtml));
                            session.setLoading(false);
                            _problemSession.setValue(session);
                        });
                        return;
                    }

                    mainHandler.post(() -> {
                        ProblemSession session = new ProblemSession();
                        session.setLoading(false);
                        session.setError("Не удалось найти задачу с ответом после 5 попыток");
                        _problemSession.setValue(session);
                    });
                } catch (Exception e) {
                    Log.wtf("SDAMGIA_DEBUG", "КРАШ в startSolving: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        ProblemSession session = new ProblemSession();
                        session.setLoading(false);
                        session.setError("Ошибка: " + e.getMessage());
                        _problemSession.setValue(session);
                    });
                }
            });
            executorThread.start();
        } catch (Exception e) {
            Log.wtf("SDAMGIA_DEBUG", "КРАШ вне корутины: " + e.getMessage(), e);
        }
    }

    public void submitAnswer(String answer) {
        ProblemSession session = _problemSession.getValue();
        if (session == null || session.getProblem() == null || session.isSolved()) return;

        ProblemData problem = session.getProblem();
        int newAttempts = session.getAttempts() + 1;
        boolean isCorrect = checkAnswer(answer, problem.getAnswer());

        if (isCorrect) {
            GameState state = _gameState.getValue();
            if (state == null) return;
            int pts;
            switch (newAttempts) {
                case 1: pts = 5; break;
                case 2: pts = 4; break;
                default: pts = 0;
            }
            int ptsB = state.getPurchasedUpgrades().contains(9) ? (int) (pts * 1.2) : pts;
            int exp = pts * 2;
            int expB = state.getPurchasedUpgrades().contains(8) ? (int) (exp * 1.25) : exp;

            int newCoins = state.getCoins() + ptsB;
            int newExp = state.getExperience() + expB;
            int lv = state.getLevel();
            int next = state.getExpToNext();
            int rem = newExp;
            while (rem >= next) {
                rem -= next;
                lv++;
                next = (int) (next * 1.5) + 5;
            }

            float newHunger = state.getHunger();
            if (!session.isSolutionShown()) {
                float ha = 30f;
                if (state.getPurchasedUpgrades().contains(0)) ha *= 1.1f;
                if (state.getPurchasedUpgrades().contains(5)) ha *= 1.1f;
                newHunger = Math.min(state.getHunger() + ha, 100f);
            }

            soundManager.playSound("correct_answer");
            GameState newState = new GameState(state);
            newState.setHunger(newHunger);
            newState.setHappiness(Math.min(state.getHappiness() + 5f, 100f));
            newState.setCoins(newCoins);
            newState.setExperience(rem);
            newState.setLevel(lv);
            newState.setExpToNext(next);
            newState.setSolveAttempts(state.getSolveAttempts() + 1);
            updateState(newState);

            ProblemSession updatedSession = new ProblemSession(session);
            updatedSession.setAttempts(newAttempts);
            updatedSession.setSolved(true);
            _problemSession.setValue(updatedSession);
        } else {
            soundManager.playSound("wrong_answer");
            soundManager.vibrate(200);
            ProblemSession updatedSession = new ProblemSession(session);
            updatedSession.setAttempts(newAttempts);
            updatedSession.setSolutionShown(newAttempts >= session.getMaxAttempts());
            _problemSession.setValue(updatedSession);
        }
    }

    private boolean checkAnswer(String userAnswer, String correctAnswer) {
        String[] parts = correctAnswer.split("\\|");
        for (String part : parts) {
            String v = part.trim();
            try {
                if (Math.abs(Double.parseDouble(normalizeAnswer(userAnswer)) -
                    Double.parseDouble(normalizeAnswer(v))) < 1e-9) {
                    return true;
                }
            } catch (NumberFormatException e) {
                if (normalizeAnswer(userAnswer).equals(normalizeAnswer(v))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeAnswer(String a) {
        String s = a.replace(",", ".").trim().toLowerCase().replaceAll("\\s+", "");
        Matcher m = Pattern.compile("-?\\d+\\.?\\d*").matcher(s);
        if (m.find()) return m.group();
        return s;
    }

    public void dismissProblem() {
        ProblemSession session = _problemSession.getValue();
        if (session != null && session.isSolved()) {
            soundManager.playSound("feed");
            GameState state = _gameState.getValue();
            if (state != null) {
                GameState newState = new GameState(state);
                newState.setAnimationState(AnimationState.EATING);
                newState.setAnimStartTime(System.currentTimeMillis());
                updateState(newState);
            }
        }
        _problemSession.setValue(new ProblemSession());
    }

    public void buyUpgrade(int upgradeId) {
        GameState state = _gameState.getValue();
        if (state == null || state.getPurchasedUpgrades().contains(upgradeId)) return;

        ShopItem item = ShopItem.findById(upgradeId);
        if (item == null) return;
        if (state.getCoins() < item.getPrice()) return;

        GameState newState = new GameState(state);
        newState.setCoins(state.getCoins() - item.getPrice());
        Set<Integer> upgrades = new HashSet<>(state.getPurchasedUpgrades());
        upgrades.add(upgradeId);
        newState.setPurchasedUpgrades(upgrades);
        updateState(newState);
    }

    public List<ShopItem> shopItems() {
        GameState state = _gameState.getValue();
        if (state == null) return ShopItem.getItems();
        List<ShopItem> result = new ArrayList<>();
        for (ShopItem item : ShopItem.getItems()) {
            int reqLevel = (item.getId() + 5) / 2 + 4;
            if (state.getLevel() >= reqLevel || item.getId() == 0) {
                result.add(item);
            }
        }
        return result;
    }

    public boolean isItemAvailable(int itemLevel) {
        GameState state = _gameState.getValue();
        return state != null && state.getLevel() >= itemLevel;
    }

    private void saveState(GameState state) {
        preferencesManager.saveGameState(state);
    }

    public int expForNextLevel() {
        GameState state = _gameState.getValue();
        return state != null ? state.getExpToNext() : 10;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public String getAnimationPath(GameState state) {
        if (state.isDead()) return "images/dead/dead.png";
        switch (state.getAnimationState()) {
            case SLEEPING: return "images/sleep/sleep";
            case EATING: return "images/eat/eat";
            case PLAYING: return "images/play/play";
            case BATHING: return "images/bath/bath";
            default:
                if (!state.isSleeping() && !state.getLowStats().isEmpty()) {
                    if (state.getLowStats().contains("hunger")) return "images/eat_want/";
                    if (state.getLowStats().contains("happiness")) return "images/play_want/";
                    if (state.getLowStats().contains("hygiene")) return "images/bath_want/";
                    if (state.getLowStats().contains("energy")) return "images/sleep_want/";
                }
                return "images/default.png";
        }
    }

    public int getAnimationFrameCount(GameState state) {
        if (state.isDead()) return 1;
        if (state.getAnimationState() == AnimationState.SLEEPING ||
            state.getAnimationState() == AnimationState.EATING ||
            state.getAnimationState() == AnimationState.PLAYING ||
            state.getAnimationState() == AnimationState.BATHING) return 4;
        if (!state.isSleeping() && !state.getLowStats().isEmpty()) return 4;
        return 1;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }
}
