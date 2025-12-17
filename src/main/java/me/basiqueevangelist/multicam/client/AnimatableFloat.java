package me.basiqueevangelist.multicam.client;

import me.basiqueevangelist.multicam.client.owocode.Animatable;
import net.minecraft.util.Mth;

public record AnimatableFloat(float inner) implements Animatable<AnimatableFloat> {
    @Override
    public AnimatableFloat interpolate(AnimatableFloat next, float delta) {
        return new AnimatableFloat(Mth.lerp(delta, this.inner, next.inner));
    }
}
