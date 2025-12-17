package me.basiqueevangelist.multicam.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void invokeSetPosition(Vec3 pos);

    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);
}
