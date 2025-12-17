package me.basiqueevangelist.windowapi.mixin;

import me.basiqueevangelist.windowapi.OpenWindows;
import me.basiqueevangelist.windowapi.context.VanillaWindowContext;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Shadow @Final private Window window;

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"))
    private void renderAllWindows(boolean tick, CallbackInfo ci) {
        if (!tick) {
            OpenWindows.renderAll();
        }
    }

    @Inject(method = "resizeDisplay", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {
        VanillaWindowContext.onWindowResized(this.window);
    }
}
