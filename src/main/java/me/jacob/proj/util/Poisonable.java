package me.jacob.proj.util;

public class Poisonable<T> {

    private final T item;
    private final boolean isPoisoned;

    private Poisonable(T item, boolean isPoisoned) {
        this.item = item;
        this.isPoisoned = isPoisoned;
    }

    public T getItem() {
        return item;
    }

    public boolean isPoisoned() {
        return isPoisoned;
    }

    public static <T> Poisonable<T> poison() {
        return new Poisonable<>(null,true);
    }

    public static <T> Poisonable<T> item(T item) {
        return new Poisonable<>(item,false);
    }
}
