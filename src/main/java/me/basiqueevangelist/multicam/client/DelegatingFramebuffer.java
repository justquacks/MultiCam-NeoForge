package me.basiqueevangelist.multicam.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public class DelegatingFramebuffer extends RenderTarget {
    private final RenderTarget original;
    private final LoadingCache<WorldViewComponent, RenderTarget> subFramebuffers;
    private RenderTarget tracking;

    public DelegatingFramebuffer(RenderTarget tracking) {
        super(tracking.useDepth);

        switchTo(tracking);
        this.original = tracking;

        this.subFramebuffers = CacheBuilder.newBuilder()
                .<WorldViewComponent, RenderTarget>removalListener(r -> {
                    if (r.getValue() != null) {
                        r.getValue().destroyBuffers();
                    }
                })
                .weakKeys()
                .build(CacheLoader.from(ctx -> {
                    TextureTarget sub = new TextureTarget(
                            ctx.framebuffer.viewWidth,
                            ctx.framebuffer.viewHeight,
                            true,
                            Minecraft.ON_OSX
                    );

                    sub.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

                    ctx.whenResized((w, h) ->
                            sub.resize(w, h, Minecraft.ON_OSX)
                    );

                    return sub;
                }));
    }

    public void switchTo(@Nullable WorldViewComponent component) {
        if (component == null) {
            switchTo(original);
        } else {
            switchTo(subFramebuffers.getUnchecked(component));
        }
    }

    public void switchTo(RenderTarget tracking) {
        this.tracking = tracking;
        this.frameBufferId = tracking.frameBufferId;
        this.width = tracking.width;
        this.height = tracking.height;
        this.viewWidth = tracking.viewWidth;
        this.viewHeight = tracking.viewHeight;
        this.filterMode = tracking.filterMode;
    }

    @Override
    public void resize(int width, int height, boolean getError) {
        tracking.resize(width, height, getError);
    }

    @Override
    public void destroyBuffers() {
        tracking.destroyBuffers();
    }

    @Override
    public void setFilterMode(int filterMode) {
        tracking.setFilterMode(filterMode);
    }

    @Override
    public void checkStatus() {
        tracking.checkStatus();
    }

    @Override
    public void bindRead() {
        tracking.bindRead();
    }

    @Override
    public void unbindRead() {
        tracking.unbindRead();
    }

    @Override
    public void bindWrite(boolean setViewport) {
        tracking.bindWrite(setViewport);
    }

    @Override
    public void unbindWrite() {
        tracking.unbindWrite();
    }

    @Override
    public void setClearColor(float r, float g, float b, float a) {
        tracking.setClearColor(r, g, b, a);
    }

    @Override
    public void blitToScreen(int width, int height) {
        tracking.blitToScreen(width, height);
    }

    @Override
    public void clear(boolean getError) {
        tracking.clear(getError);
    }

    @Override
    public int getColorTextureId() {
        return tracking.getColorTextureId();
    }
}
