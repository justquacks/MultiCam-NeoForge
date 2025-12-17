package me.basiqueevangelist.multicam.mixin.client;

import me.basiqueevangelist.multicam.client.DelegatingFramebuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PostChain.class)
public class PostEffectProcessorMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static RenderTarget trackifyFramebuffer(RenderTarget framebuffer) {
        if (framebuffer instanceof DelegatingFramebuffer)
            return framebuffer;
        else
            return new DelegatingFramebuffer(framebuffer);
    }

    @ModifyVariable(method = "addTempTarget", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;setClearColor(FFFF)V"))
    private RenderTarget trackify(RenderTarget old) {
        return new DelegatingFramebuffer(old);
    }
}
