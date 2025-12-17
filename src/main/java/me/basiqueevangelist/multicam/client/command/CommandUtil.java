package me.basiqueevangelist.multicam.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.multicam.client.CameraWindow;
import me.basiqueevangelist.multicam.client.command.argument.MsTimeArgumentType;
import me.basiqueevangelist.multicam.client.owocode.Animatable;
import me.basiqueevangelist.multicam.client.owocode.AnimatableProperty;
import me.basiqueevangelist.multicam.client.owocode.Easing;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;

import java.util.function.Function;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandUtil {
    // TODO: translate.
    public static final SimpleCommandExceptionType NO_SUCH_CAMERA = new SimpleCommandExceptionType(Component.literal("No such camera"));

    public static ArgumentBuilder<CommandSourceStack, ?> cameraNode() {
        return argument("camera", IntegerArgumentType.integer(1))
            .suggests((ctx, suggestionsBuilder) -> {
                for (int i = 0; i < CameraWindow.CAMERAS.size(); i++) {
                    if (CameraWindow.CAMERAS.get(i) != null) {
                        suggestionsBuilder.suggest(i + 1);
                    }
                }

                return suggestionsBuilder.buildFuture();
            });
    }

    public static CameraWindow getCamera(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int cameraId = IntegerArgumentType.getInteger(ctx, "camera");

        cameraId -= 1;

        if (cameraId >= CameraWindow.CAMERAS.size()) throw NO_SUCH_CAMERA.create();

        CameraWindow camera = CameraWindow.CAMERAS.get(cameraId);

        if (camera == null) throw NO_SUCH_CAMERA.create();

        return camera;
    }

    public static void addInAt(ArgumentBuilder<CommandSourceStack, ?> builder, Function<AnimationConfigurer, ArgumentBuilder<CommandSourceStack, ?>> next) {
        // TODO: custom easings.
        builder
            .then(next.apply(new AnimationConfigurer() {
                @Override
                public <A extends Animatable<A>> void configureAnimation(CommandContext<CommandSourceStack> ctx, AnimatableProperty<A> property, A target, float distance) {
                    property.set(target);
                }
            }))
            .then(literal("in")
                .then(argument("duration", MsTimeArgumentType.time())
                    .then(next.apply(new AnimationConfigurer() {
                        @Override
                        public <A extends Animatable<A>> void configureAnimation(CommandContext<CommandSourceStack> ctx, AnimatableProperty<A> property, A target, float distance) {
                            property.animate(
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                Easing.LINEAR,
                                target
                            )
                                .forwards();
                        }
                    }))))
            .then(literal("at")
                .then(argument("speed", FloatArgumentType.floatArg(0.1f))
                    .then(next.apply(new AnimationConfigurer() {
                        @Override
                        public <A extends Animatable<A>> void configureAnimation(CommandContext<CommandSourceStack> ctx, AnimatableProperty<A> property, A target, float distance) {
                            int duration = (int) ((distance / FloatArgumentType.getFloat(ctx, "speed")) * 1000);

                            property.animate(
                                duration,
                                Easing.LINEAR,
                                target
                            )
                                .forwards();
                        }
                    }))));
    }

    static CommandSourceStack getSourceForCamera(CommandSourceStack delegate, CameraWindow camera) {
        return new CommandSourceStack(
            delegate.source,
            camera.worldView.position(),
            new Vec2(camera.worldView.pitch(), camera.worldView.yaw()),
            delegate.getLevel(),
            delegate.hasPermission(2) ? 2 : 0, // Changed from getPermissionLevel() to hasPermission()
            delegate.getTextName(),
            delegate.getDisplayName(),
            delegate.getServer(),
            delegate.getEntity()
        );
    }

    public interface AnimationConfigurer {
        <A extends Animatable<A>> void configureAnimation(CommandContext<CommandSourceStack> ctx, AnimatableProperty<A> property, A target, float distance);
    }
}
