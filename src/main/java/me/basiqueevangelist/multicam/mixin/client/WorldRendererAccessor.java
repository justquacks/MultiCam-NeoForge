package me.basiqueevangelist.multicam.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("entityEffect")
    @Nullable
    PostChain getEntityOutlinePostProcessor();

    @Accessor("transparencyChain")
    @Nullable
    PostChain getTransparencyPostProcessor();
}
