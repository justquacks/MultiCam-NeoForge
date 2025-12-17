package me.basiqueevangelist.multicam.mixin.client;

import com.mojang.blaze3d.platform.Window;
import me.basiqueevangelist.multicam.client.WorldViewComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void malding(CallbackInfoReturnable<Integer> cir) {
        if (WorldViewComponent.CURRENT_BUFFER != null) {
            cir.setReturnValue(WorldViewComponent.CURRENT_BUFFER.width);
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void maldnite(CallbackInfoReturnable<Integer> cir) {
        if (WorldViewComponent.CURRENT_BUFFER != null) {
            cir.setReturnValue(WorldViewComponent.CURRENT_BUFFER.height);
        }
    }
}
