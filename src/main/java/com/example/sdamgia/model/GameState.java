package com.example.sdamgia.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameState {
    private float hunger;
    private float happiness;
    private float hygiene;
    private float energy;
    private boolean isSleeping;
    private boolean isDead;
    private int coins;
    private int experience;
    private int level;
    private int expToNext;
    private Set<Integer> purchasedUpgrades;
    private Set<String> activeUpgrades;
    private long lastUpdateTime;
    private String petName;
    private Set<String> lowStats;
    private AnimationState animationState;
    private long animStartTime;
    private boolean solutionShown;
    private int solveAttempts;
    private boolean soundEnabled;
    private boolean musicEnabled;
    private float musicVolume;
    private List<String> recentProblemIds;
    private int maxCacheSizeMb;
    private boolean overlayEnabled;

    public GameState() {
        this.hunger = 70f;
        this.happiness = 70f;
        this.hygiene = 70f;
        this.energy = 70f;
        this.isSleeping = false;
        this.isDead = false;
        this.coins = 0;
        this.experience = 0;
        this.level = 1;
        this.expToNext = 10;
        this.purchasedUpgrades = new HashSet<>();
        this.activeUpgrades = new HashSet<>();
        this.lastUpdateTime = System.currentTimeMillis();
        this.petName = "Шпоргалка";
        this.lowStats = new HashSet<>();
        this.animationState = AnimationState.IDLE;
        this.animStartTime = 0L;
        this.solutionShown = false;
        this.solveAttempts = 0;
        this.soundEnabled = true;
        this.musicEnabled = true;
        this.musicVolume = 0.5f;
        this.recentProblemIds = new ArrayList<>();
        this.maxCacheSizeMb = 10;
        this.overlayEnabled = false;
    }

    public GameState(GameState other) {
        this.hunger = other.hunger;
        this.happiness = other.happiness;
        this.hygiene = other.hygiene;
        this.energy = other.energy;
        this.isSleeping = other.isSleeping;
        this.isDead = other.isDead;
        this.coins = other.coins;
        this.experience = other.experience;
        this.level = other.level;
        this.expToNext = other.expToNext;
        this.purchasedUpgrades = new HashSet<>(other.purchasedUpgrades);
        this.activeUpgrades = new HashSet<>(other.activeUpgrades);
        this.lastUpdateTime = other.lastUpdateTime;
        this.petName = other.petName;
        this.lowStats = new HashSet<>(other.lowStats);
        this.animationState = other.animationState;
        this.animStartTime = other.animStartTime;
        this.solutionShown = other.solutionShown;
        this.solveAttempts = other.solveAttempts;
        this.soundEnabled = other.soundEnabled;
        this.musicEnabled = other.musicEnabled;
        this.musicVolume = other.musicVolume;
        this.recentProblemIds = new ArrayList<>(other.recentProblemIds);
        this.maxCacheSizeMb = other.maxCacheSizeMb;
        this.overlayEnabled = other.overlayEnabled;
    }

    public float getHunger() { return hunger; }
    public void setHunger(float hunger) { this.hunger = hunger; }

    public float getHappiness() { return happiness; }
    public void setHappiness(float happiness) { this.happiness = happiness; }

    public float getHygiene() { return hygiene; }
    public void setHygiene(float hygiene) { this.hygiene = hygiene; }

    public float getEnergy() { return energy; }
    public void setEnergy(float energy) { this.energy = energy; }

    public boolean isSleeping() { return isSleeping; }
    public void setSleeping(boolean sleeping) { isSleeping = sleeping; }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExpToNext() { return expToNext; }
    public void setExpToNext(int expToNext) { this.expToNext = expToNext; }

    public Set<Integer> getPurchasedUpgrades() { return purchasedUpgrades; }
    public void setPurchasedUpgrades(Set<Integer> purchasedUpgrades) { this.purchasedUpgrades = purchasedUpgrades; }

    public Set<String> getActiveUpgrades() { return activeUpgrades; }
    public void setActiveUpgrades(Set<String> activeUpgrades) { this.activeUpgrades = activeUpgrades; }

    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public Set<String> getLowStats() { return lowStats; }
    public void setLowStats(Set<String> lowStats) { this.lowStats = lowStats; }

    public AnimationState getAnimationState() { return animationState; }
    public void setAnimationState(AnimationState animationState) { this.animationState = animationState; }

    public long getAnimStartTime() { return animStartTime; }
    public void setAnimStartTime(long animStartTime) { this.animStartTime = animStartTime; }

    public boolean isSolutionShown() { return solutionShown; }
    public void setSolutionShown(boolean solutionShown) { this.solutionShown = solutionShown; }

    public int getSolveAttempts() { return solveAttempts; }
    public void setSolveAttempts(int solveAttempts) { this.solveAttempts = solveAttempts; }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float musicVolume) { this.musicVolume = musicVolume; }

    public List<String> getRecentProblemIds() { return recentProblemIds; }
    public void setRecentProblemIds(List<String> recentProblemIds) { this.recentProblemIds = recentProblemIds; }

    public int getMaxCacheSizeMb() { return maxCacheSizeMb; }
    public void setMaxCacheSizeMb(int maxCacheSizeMb) { this.maxCacheSizeMb = maxCacheSizeMb; }

    public boolean isOverlayEnabled() { return overlayEnabled; }
    public void setOverlayEnabled(boolean overlayEnabled) { this.overlayEnabled = overlayEnabled; }
}
