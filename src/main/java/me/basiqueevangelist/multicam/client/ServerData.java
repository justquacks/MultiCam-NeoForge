package me.basiqueevangelist.multicam.client;

import me.basiqueevangelist.multicam.common.MultiCamUsageS2CPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public class ServerData {
    private static @Nullable MultiCamUsageS2CPacket PACKET;

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(
            MultiCamUsageS2CPacket.TYPE,
            MultiCamUsageS2CPacket.STREAM_CODEC,
            ServerData::handleUsagePacket
        );
    }

    private static void handleUsagePacket(MultiCamUsageS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerData.PACKET = packet;

            if (!packet.canUse()) {
                MultiCam.closeAllWindows();
            }
        });
    }

    public static void onDisconnect() {
        PACKET = null;
    }

    public static boolean canUse(boolean defaultValue) {
        return PACKET != null ? PACKET.canUse() : defaultValue;
    }
}
