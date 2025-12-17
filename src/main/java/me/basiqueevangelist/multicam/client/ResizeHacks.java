package me.basiqueevangelist.multicam.client;

import java.lang.reflect.Field;

import me.basiqueevangelist.multicam.mixin.client.PostEffectProcessorAccessor;
import me.basiqueevangelist.multicam.mixin.client.WorldRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class ResizeHacks {
    private ResizeHacks() {

    }

    public static void resize(GameRenderer renderer, @Nullable WorldViewComponent ctx) {
        if (renderer.currentEffect() != null) {
            resize(renderer.currentEffect(), ctx);
        }

        resize(renderer.getMinecraft().levelRenderer, ctx);
    }

    public static void resize(LevelRenderer renderer, @Nullable WorldViewComponent ctx) {
        try {
            Field entityEffectField = LevelRenderer.class.getDeclaredField("entityEffect");
            entityEffectField.setAccessible(true);
            PostChain entityOutlineProcessor = (PostChain) entityEffectField.get(renderer);
            
            if (entityOutlineProcessor != null) {
                resize(entityOutlineProcessor, ctx);
            }

            Field transparencyField = LevelRenderer.class.getDeclaredField("transparencyChain");
            transparencyField.setAccessible(true);
            PostChain transparencyProcessor = (PostChain) transparencyField.get(renderer);
            
            if (transparencyProcessor != null) {
                resize(transparencyProcessor, ctx);
            }
        } catch (Exception e) {
            System.out.println("Failed to access LevelRenderer fields: " + e.getMessage());
        }
    }

    public static void resize(PostChain processor, @Nullable WorldViewComponent ctx) {
        PostEffectProcessorAccessor duck = (PostEffectProcessorAccessor) processor;

        duck.setWidth(width(ctx));
        duck.setHeight(height(ctx));

        var projectionMatrix = new Matrix4f()
            .setOrtho(
                0.0F,
                width(ctx),
                0.0F,
                height(ctx),
                0.1F,
                1000.0F
            );
        duck.setProjectionMatrix(projectionMatrix);

        if (duck.getMainTarget() instanceof DelegatingFramebuffer deleg)
            deleg.switchTo(ctx);

        for (PostPass pass : duck.getPasses()) {
            pass.setOrthoMatrix(projectionMatrix);
        }

        for (var target : duck.getDefaultSizedTargets()) {
            if (target instanceof DelegatingFramebuffer deleg)
                deleg.switchTo(ctx);
        }
    }

    private static int width(@Nullable WorldViewComponent ctx) {
        return ctx == null ? Minecraft.getInstance().getWindow().getWidth() : ctx.framebuffer.width;
    }

    private static int height(@Nullable WorldViewComponent ctx) {
        return ctx == null ? Minecraft.getInstance().getWindow().getHeight() : ctx.framebuffer.height;
    }
}
