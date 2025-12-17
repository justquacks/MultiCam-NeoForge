package me.basiqueevangelist.multicam.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import me.basiqueevangelist.multicam.client.CameraWindow;
import me.basiqueevangelist.multicam.client.command.argument.ClientVec3ArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OrbitCameraCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> build() {
        return literal("orbit")
            .then(CommandUtil.cameraNode()
                .then(argument("position", ClientVec3ArgumentType.vec3(true))
                    .then(argument("period", FloatArgumentType.floatArg(0))
                        .executes(ctx -> {
                            CameraWindow camera = CommandUtil.getCamera(ctx);

                            LocalPlayer player = Minecraft.getInstance().player;
                            ClientLevel level = Minecraft.getInstance().level;
                            
                            Vec3 pos = ClientVec3ArgumentType.getPosArgument(ctx, "position").toAbsolutePos(player, level);

                            float period = FloatArgumentType.getFloat(ctx, "period");

                            float rad2d = (float) rad2d(camera.worldView.position(), pos);

                            camera.beginOrbit(pos, period, (float) camera.worldView.position().y, rad2d);

                            return 0;
                        }))));
    }

    private static double rad2d(Vec3 a, Vec3 b) {
        double x = b.x - a.x;
        double z = b.z - a.z;

        return Math.sqrt(x * x + z * z);
    }
}
