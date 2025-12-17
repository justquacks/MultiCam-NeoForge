package me.basiqueevangelist.multicam.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ClientDefaultPosArgument implements ClientPosArgument {
    private final WorldCoordinates worldCoordinates;

    public ClientDefaultPosArgument(WorldCoordinates worldCoordinates) {
        this.worldCoordinates = worldCoordinates;
    }

    @Override
    public Vec3 toAbsolutePos(LocalPlayer player, ClientLevel level) {
        // Use the player's command source stack to resolve coordinates
        return worldCoordinates.getPosition(player.createCommandSourceStack());
    }

    @Override
    public Vec2 toAbsoluteRotation(LocalPlayer player) {
        // Use the player's command source stack to resolve rotation
        return worldCoordinates.getRotation(player.createCommandSourceStack());
    }

    @Override
    public boolean isXRelative() {
        return worldCoordinates.isXRelative();
    }

    @Override
    public boolean isYRelative() {
        return worldCoordinates.isYRelative();
    }

    @Override
    public boolean isZRelative() {
        return worldCoordinates.isZRelative();
    }

    public static ClientDefaultPosArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        
        // WorldCoordinates.parseInt() no longer takes a boolean parameter in 1.21.1
        WorldCoordinates coords = WorldCoordinates.parseInt(reader);
        
        return new ClientDefaultPosArgument(coords);
    }
}
