package me.basiqueevangelist.multicam.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MultiCamUsageS2CPacket(boolean canUse) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MultiCamUsageS2CPacket> TYPE = 
        new CustomPacketPayload.Type<>(MultiCamCommon.id("usage"));
    
    public static final StreamCodec<ByteBuf, MultiCamUsageS2CPacket> STREAM_CODEC = 
        ByteBufCodecs.BOOL.map(MultiCamUsageS2CPacket::new, MultiCamUsageS2CPacket::canUse);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
