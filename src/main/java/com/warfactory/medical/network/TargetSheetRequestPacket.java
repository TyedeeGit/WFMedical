package com.warfactory.medical.network;

import com.warfactory.medical.server.MedicalActionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record TargetSheetRequestPacket(int targetEntityId) {

    public static TargetSheetRequestPacket decode(FriendlyByteBuf buf) {
        return new TargetSheetRequestPacket(buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
    }

    public void handleServer(ServerPlayer sender) {
        if (sender != null) {
            MedicalActionService.requestTargetSheet(sender, targetEntityId);
        }
    }
}
