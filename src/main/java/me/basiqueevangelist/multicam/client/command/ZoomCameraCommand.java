package me.basiqueevangelist.multicam.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import me.basiqueevangelist.multicam.client.AnimatableFloat;
import me.basiqueevangelist.multicam.client.CameraWindow;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ZoomCameraCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> build() {
        var cameraNode = CommandUtil.cameraNode();

        CommandUtil.addInAt(cameraNode, configurer ->
            literal("to")
                .then(argument("fov", FloatArgumentType.floatArg(0))
                    .executes(ctx -> {
                        CameraWindow camera = CommandUtil.getCamera(ctx);
                        float fov = FloatArgumentType.getFloat(ctx, "fov");

                        configurer.configureAnimation(
                            ctx,
                            camera.worldView.fov,
                            new AnimatableFloat(fov),
                            Math.abs(fov - camera.worldView.fov())
                        );

                        return 0;
                    })));

        return literal("zoom").then(cameraNode);
    }
}
