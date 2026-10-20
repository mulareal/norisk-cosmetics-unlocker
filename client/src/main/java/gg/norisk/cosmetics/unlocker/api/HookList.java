package gg.norisk.cosmetics.unlocker.api;

import gg.norisk.cosmetics.unlocker.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Listeners for one hook. A failing listener is logged and never breaks the caller (usually a mixin). */
public final class HookList<T> {

    private final String name;
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    HookList(String name) {
        this.name = name;
    }

    public void register(T listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    public void fire(Consumer<T> call) {
        for (T listener : listeners) {
            try {
                call.accept(listener);
            } catch (Throwable t) {
                Log.warn("Hook " + name + " failed: " + t);
            }
        }
    }
}
