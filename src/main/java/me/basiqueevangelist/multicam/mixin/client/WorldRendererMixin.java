package me.basiqueevangelist.multicam.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.basiqueevangelist.multicam.client.WorldViewComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    // Taken from https://github.com/maruohon/tweakeroo/blob/pre-rewrite/fabric/1.19.x/src/main/java/fi/dy/masa/tweakeroo/mixin/MixinWorldRenderer.java#L67-L78.
    // I have no idea if this comment is real anymore, claude might've eaten you. Sorry. Shoutout that person tho for doin that one thing. No clue what it was but W you.
    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;", ordinal = 3))
    private Entity makePlayerRender(Entity old) {
        if (WorldViewComponent.CURRENT_BUFFER != null)
            return minecraft.player;

        return old;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean makePlayerRender(boolean old) {
        if (WorldViewComponent.CURRENT_BUFFER != null)
            return true;

        return old;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;entitiesForRendering()Ljava/lang/Iterable;"))
    private Iterable<Entity> disableEntitiesIfNeeded(Iterable<Entity> old) {
        if (WorldViewComponent.CURRENT != null && WorldViewComponent.CURRENT.disableEntities())
            return Collections.emptyList();

        return old;
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
    private boolean disableParticlesIfNeeded(ParticleEngine instance, LightTexture lightTexture, Camera camera, float tickDelta) {
        return WorldViewComponent.CURRENT == null || !WorldViewComponent.CURRENT.disableParticles();
    }
}
