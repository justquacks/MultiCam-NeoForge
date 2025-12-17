package me.basiqueevangelist.multicam.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import me.basiqueevangelist.multicam.client.WorldViewComponent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void malding(CallbackInfoReturnable<RenderTarget> cir) {
        if (WorldViewComponent.CURRENT_BUFFER != null) {
            cir.setReturnValue(WorldViewComponent.CURRENT_BUFFER);
        }
    }
}
