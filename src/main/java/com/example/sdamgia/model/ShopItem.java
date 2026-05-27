package com.example.sdamgia.model;

import java.util.Arrays;
import java.util.List;

public class ShopItem {
    private final int id;
    private final String name;
    private final String description;
    private final String effect;
    private final int price;

    public ShopItem(int id, String name, String description, String effect, int price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effect = effect;
        this.price = price;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEffect() { return effect; }
    public int getPrice() { return price; }

    private static final List<ShopItem> ITEMS = Arrays.asList(
        new ShopItem(0, "Усиленное питание", "Сытость восстанавливается на 10% больше", "Сытость ×1.1", 100),
        new ShopItem(1, "Замедление грусти", "Счастье убывает в 2 раза медленнее", "Счастье ×2 медленнее", 150),
        new ShopItem(2, "Энергосбережение", "Энергия убывает в 1.5 раза медленнее", "Энергия ×1.5 медленнее", 120),
        new ShopItem(3, "Сытость", "Сытость убывает в 1.5 раза медленнее", "Сытость ×1.5 медленнее", 120),
        new ShopItem(4, "Гигиена", "Чистота убывает в 1.5 раза медленнее", "Чистота ×1.5 медленнее", 100),
        new ShopItem(5, "Двойная порция", "Сытость +10% за решённую задачу", "Сытость +10%", 200),
        new ShopItem(6, "Игрушки", "Счастье +15% за игру", "Счастье +15%", 250),
        new ShopItem(7, "Уютная постель", "Энергия восстанавливается в 1.5 раза быстрее", "Энергия ×1.5 быстрее", 180),
        new ShopItem(8, "Учебник", "Опыт +25%", "Опыт +25%", 300),
        new ShopItem(9, "Копилка", "Очки +20%", "Очки +20%", 300)
    );

    public static List<ShopItem> getItems() {
        return ITEMS;
    }

    public static ShopItem findById(int id) {
        for (ShopItem item : ITEMS) {
            if (item.id == id) return item;
        }
        return null;
    }
}
