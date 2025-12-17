package me.basiqueevangelist.multicam.client.command.argument;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface ClientPosArgument {
    Vec3 toAbsolutePos(LocalPlayer player, ClientLevel level);

    Vec2 toAbsoluteRotation(LocalPlayer player);

    default BlockPos toAbsoluteBlockPos(LocalPlayer player, ClientLevel level) {
        return BlockPos.containing(this.toAbsolutePos(player, level));
    }

    boolean isXRelative();

    boolean isYRelative();

    boolean isZRelative();
}
