package me.basiqueevangelist.multicam.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.basiqueevangelist.multicam.client.command.ConfigCameraCommand;
import me.basiqueevangelist.multicam.client.command.MoveCameraCommand;
import me.basiqueevangelist.multicam.client.command.OrbitCameraCommand;
import me.basiqueevangelist.multicam.client.command.ZoomCameraCommand;
import me.basiqueevangelist.windowapi.OpenWindows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = "multicam", value = Dist.CLIENT)
public class MultiCam {
    public static ShaderInstance WORLD_VIEW_PROGRAM = null;

    public static int FPS_TARGET = 60;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Empty again idk 
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("multicam", "world_view"),
                    DefaultVertexFormat.POSITION_TEX
                ),
                shader -> WORLD_VIEW_PROGRAM = shader
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to register shader", e);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            literal("multicam")
                .requires(x -> ServerData.canUse(x.hasPermission(2)))
                .executes(context -> {
                    new CameraWindow().open();
                    return 1;
                })
                .then(literal("fps_target")
                    .then(argument("target", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            FPS_TARGET = IntegerArgumentType.getInteger(ctx, "target");
                            return 0;
                        })))
                .then(MoveCameraCommand.build())
                .then(ZoomCameraCommand.build())
                .then(OrbitCameraCommand.build())
                .then(ConfigCameraCommand.build())
        );
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        closeAllWindows();
    }

    public static void closeAllWindows() {
        OpenWindows.windows().forEach(x -> {
            if (x instanceof CameraWindow) {
                Minecraft.getInstance().execute(x::close);
            }
        });
    }
}
