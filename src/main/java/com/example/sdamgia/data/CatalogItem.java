package com.example.sdamgia.data;

import com.google.gson.annotations.SerializedName;

public class CatalogItem {
    @SerializedName("id")
    private final int id;

    @SerializedName("name")
    private final String name;

    @SerializedName("count")
    private final int count;

    public CatalogItem(int id, String name, int count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCount() { return count; }
}
