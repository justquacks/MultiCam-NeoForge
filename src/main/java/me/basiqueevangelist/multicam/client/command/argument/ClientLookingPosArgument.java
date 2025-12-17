package me.basiqueevangelist.multicam.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ClientLookingPosArgument implements ClientPosArgument {
    private final WorldCoordinate left;
    private final WorldCoordinate up;
    private final WorldCoordinate forwards;

    public ClientLookingPosArgument(WorldCoordinate left, WorldCoordinate up, WorldCoordinate forwards) {
        this.left = left;
        this.up = up;
        this.forwards = forwards;
    }

    @Override
    public Vec3 toAbsolutePos(LocalPlayer player, ClientLevel level) {
        Vec3 playerPos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        
        float yawRad = yaw * (float) (Math.PI / 180.0);
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        
        // Extract coordinate values - for local coordinates, we use 0 as the base
        double leftVal = left.get(0);
        double upVal = up.get(0);
        double forwardsVal = forwards.get(0);
        
        Vec3 leftVec = new Vec3(-sinYaw, 0, cosYaw);
        Vec3 upVec = new Vec3(0, 1, 0);
        Vec3 forwardVec = new Vec3(cosYaw * cosPitch, -sinPitch, sinYaw * cosPitch);
        
        Vec3 offset = leftVec.scale(leftVal)
            .add(upVec.scale(upVal))
            .add(forwardVec.scale(-forwardsVal));
        
        return playerPos.add(offset);
    }

    @Override
    public Vec2 toAbsoluteRotation(LocalPlayer player) {
        return player.getRotationVector();
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static ClientLookingPosArgument parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        WorldCoordinate left = WorldCoordinate.parseDouble(reader, false);
        
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, "Expected 3 coordinates");
        }
        
        reader.skip();
        WorldCoordinate up = WorldCoordinate.parseDouble(reader, false);
        
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, "Expected 3 coordinates");
        }
        
        reader.skip();
        WorldCoordinate forwards = WorldCoordinate.parseDouble(reader, false);
        
        return new ClientLookingPosArgument(left, up, forwards);
    }
}
