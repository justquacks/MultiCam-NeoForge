package me.basiqueevangelist.multicam.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

@Mod("multicam")
public class MultiCamCommon {
    public static final String MOD_ID = "multicam";
    
    public static final PermissionNode<Boolean> USE_PERMISSION = new PermissionNode<>(
        MOD_ID,
        "use",
        PermissionTypes.BOOLEAN,
        (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public MultiCamCommon(IEventBus modEventBus) {
        //I have no idea why this is empty. Claude did this. Blame AL
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void sendUsagePacket(ServerPlayer player) {
        boolean canUse = PermissionAPI.getPermission(player, USE_PERMISSION);
        PacketDistributor.sendToPlayer(player, new MultiCamUsageS2CPacket(canUse));
    }
}
