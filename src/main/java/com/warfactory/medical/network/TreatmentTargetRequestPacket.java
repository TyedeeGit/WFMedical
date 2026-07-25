package com.warfactory.medical.network;

import com.warfactory.medical.server.MedicalActionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record TreatmentTargetRequestPacket(int targetEntityId, ResourceLocation itemId) {

    public static TreatmentTargetRequestPacket decode(FriendlyByteBuf buf) {
        int targetEntityId = buf.readVarInt();
        ResourceLocation itemId = buf.readResourceLocation();
        return new TreatmentTargetRequestPacket(targetEntityId, itemId);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
        buf.writeResourceLocation(itemId);
    }

    public void handleServer(ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        MedicalActionService.requestTargetInfo(sender, targetEntityId, itemId);
    }
}
