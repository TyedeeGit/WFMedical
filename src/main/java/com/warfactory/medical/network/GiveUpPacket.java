package com.warfactory.medical.network;

import com.warfactory.medical.server.MedicalEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record GiveUpPacket() {

    public static GiveUpPacket decode(FriendlyByteBuf buf) {
        return new GiveUpPacket();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handleServer(ServerPlayer sender) {
        if (sender != null) {
            MedicalEngine.giveUp(sender);
        }
    }
}
