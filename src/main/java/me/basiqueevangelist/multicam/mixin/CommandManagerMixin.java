package me.basiqueevangelist.multicam.mixin;

import me.basiqueevangelist.multicam.common.MultiCamCommon;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandManagerMixin {
    @Inject(method = "sendCommands", at = @At("HEAD"))
    private void onReloadPermissions(ServerPlayer player, CallbackInfo ci) {
        MultiCamCommon.sendUsagePacket(player);
    }
}
