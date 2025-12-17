package me.basiqueevangelist.multicam.client;

import me.basiqueevangelist.multicam.client.owocode.Animatable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record AnimatableVec3d(Vec3 inner) implements Animatable<AnimatableVec3d> {
    @Override
    public AnimatableVec3d interpolate(AnimatableVec3d next, float delta) {
        return new AnimatableVec3d(
            new Vec3(
                Mth.lerp(delta, this.inner.x, next.inner.x),
                Mth.lerp(delta, this.inner.y, next.inner.y),
                Mth.lerp(delta, this.inner.z, next.inner.z)
            )
        );
    }
}
