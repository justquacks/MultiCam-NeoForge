package me.basiqueevangelist.multicam.client.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import me.basiqueevangelist.multicam.client.AnimatableFloat;
import me.basiqueevangelist.multicam.client.AnimatableVec3d;
import me.basiqueevangelist.multicam.client.CameraWindow;
import me.basiqueevangelist.multicam.client.command.argument.ClientPosArgument;
import me.basiqueevangelist.multicam.client.command.argument.ClientRotationArgumentType;
import me.basiqueevangelist.multicam.client.command.argument.ClientVec3ArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class MoveCameraCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> build() {
        var cameraNode = CommandUtil.cameraNode();

        CommandUtil.addInAt(cameraNode, configurer -> literal("to")
            .then(argument("position", ClientVec3ArgumentType.vec3(true))
                .executes(ctx -> {
                    CameraWindow camera = CommandUtil.getCamera(ctx);

                    LocalPlayer player = Minecraft.getInstance().player;
                    ClientLevel level = Minecraft.getInstance().level;
                    
                    Vec3 pos = ClientVec3ArgumentType.getPosArgument(ctx, "position").toAbsolutePos(player, level);

                    configurer.configureAnimation(
                        ctx,
                        camera.worldView.position,
                        new AnimatableVec3d(pos),
                        (float) pos.distanceTo(camera.worldView.position())
                    );

                    return 0;
                })
                .then(argument("rotation", ClientRotationArgumentType.rotation())
                    .executes(ctx -> {
                        CameraWindow camera = CommandUtil.getCamera(ctx);

                        LocalPlayer player = Minecraft.getInstance().player;
                        ClientLevel level = Minecraft.getInstance().level;

                        Vec3 pos = ClientVec3ArgumentType.getPosArgument(ctx, "position").toAbsolutePos(player, level);
                        ClientPosArgument rotArg = ClientRotationArgumentType.getRotation(ctx, "rotation");

                        Vec2 rot = rotArg.toAbsoluteRotation(player);

                        configurer.configureAnimation(
                            ctx,
                            camera.worldView.position,
                            new AnimatableVec3d(pos),
                            (float) pos.distanceTo(camera.worldView.position())
                        );

                        configurer.configureAnimation(
                            ctx,
                            camera.worldView.pitch,
                            new AnimatableFloat(rot.x),
                            Math.abs(rot.x - camera.worldView.pitch())
                        );

                        configurer.configureAnimation(
                            ctx,
                            camera.worldView.yaw,
                            new AnimatableFloat(rot.y),
                            Math.abs(rot.y - camera.worldView.yaw())
                        );

                        return 0;
                    }))));

        return literal("move").then(cameraNode);
    }
}
