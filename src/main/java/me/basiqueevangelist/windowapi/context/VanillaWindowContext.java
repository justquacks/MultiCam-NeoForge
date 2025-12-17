package me.basiqueevangelist.windowapi.context;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import me.basiqueevangelist.windowapi.WindowFramebufferResized;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;

import java.util.WeakHashMap;
import java.util.function.Consumer;

public class VanillaWindowContext implements WindowContext {
    private static final WeakHashMap<Window, VanillaWindowContext> MAP = new WeakHashMap<>();

    public static final VanillaWindowContext MAIN =
            new VanillaWindowContext(
                    Minecraft.getInstance().getWindow(),
                    Minecraft.getInstance().getMainRenderTarget()
            );

    private final Window window;
    private final RenderTarget framebuffer;

    private VanillaWindowContext(Window window, RenderTarget framebuffer) {
        this.window = window;
        this.framebuffer = framebuffer;
        MAP.put(window, this);
    }

    public static void onWindowResized(Window window) {
        var ctx = MAP.get(window);
        if (ctx == null) return;
        NeoForge.EVENT_BUS.post(
                new WindowFramebufferResized(
                        window.getWidth(),
                        window.getHeight()
                )
        );
    }

    @Override
    public int framebufferWidth() {
        return window.getWidth();
    }

    @Override
    public int framebufferHeight() {
        return window.getHeight();
    }

    @Override
    public RenderTarget framebuffer() {
        return framebuffer;
    }

    @Override
    public int scaledWidth() {
        return window.getGuiScaledWidth();
    }

    @Override
    public int scaledHeight() {
        return window.getGuiScaledHeight();
    }

    @Override
    public double scaleFactor() {
        return window.getGuiScale();
    }

    @Override
    public long handle() {
        return window.getWindow();
    }

    @Override
    public void onFramebufferResized(Consumer<WindowContext> consumer) {
        consumer.accept(this);
    }

    @Override
    public <T> T get(Key<WindowContext, T> key) {
        return null;
    }
}
