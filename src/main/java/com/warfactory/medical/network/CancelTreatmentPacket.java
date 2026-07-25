package com.warfactory.medical.network;

import com.warfactory.medical.server.MedicalActionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record CancelTreatmentPacket() {

    public static CancelTreatmentPacket decode(FriendlyByteBuf buf) {
        return new CancelTreatmentPacket();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handleServer(ServerPlayer sender) {
        if (sender != null) {
            MedicalActionService.cancel(sender, "interrupted");
        }
    }
}
