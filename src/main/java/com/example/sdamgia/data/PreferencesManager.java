package com.example.sdamgia.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sdamgia.model.GameState;
import com.google.gson.Gson;

public class PreferencesManager {

    private static final String PREFS_NAME = "game_state_prefs";
    private static final String KEY_GAME_STATE = "game_state";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public GameState loadGameState() {
        String json = prefs.getString(KEY_GAME_STATE, null);
        if (json == null) return new GameState();
        try {
            return gson.fromJson(json, GameState.class);
        } catch (Exception e) {
            return new GameState();
        }
    }

    public void saveGameState(GameState state) {
        try {
            String json = gson.toJson(state);
            prefs.edit().putString(KEY_GAME_STATE, json).apply();
        } catch (Exception ignored) {}
    }
}
