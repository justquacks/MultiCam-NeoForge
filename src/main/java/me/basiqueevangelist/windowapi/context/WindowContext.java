package me.basiqueevangelist.windowapi.context;

import com.mojang.blaze3d.pipeline.RenderTarget;
import me.basiqueevangelist.windowapi.SupportsFeatures;

import java.util.function.Consumer;

public interface WindowContext extends SupportsFeatures<WindowContext> {
    int framebufferWidth();
    int framebufferHeight();
    
    void onFramebufferResized(Consumer<WindowContext> callback);
    
    RenderTarget framebuffer();

    int scaledWidth();
    int scaledHeight();
    double scaleFactor();

    long handle();
}
