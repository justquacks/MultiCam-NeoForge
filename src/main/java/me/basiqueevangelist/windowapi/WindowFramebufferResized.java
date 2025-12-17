package me.basiqueevangelist.windowapi;

import net.neoforged.bus.api.Event;

public class WindowFramebufferResized extends Event {
    public final int width;
    public final int height;

    public WindowFramebufferResized(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
