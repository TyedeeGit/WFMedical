package com.warfactory.medical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record TargetSheetInfoPacket(int targetEntityId, MedicalSyncPacket snapshot, int tourniquetMask) {

    public static TargetSheetInfoPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        MedicalSyncPacket snap = MedicalSyncPacket.decode(buf);
        int mask = buf.readVarInt();
        return new TargetSheetInfoPacket(id, snap, mask);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
        snapshot.encode(buf);
        buf.writeVarInt(tourniquetMask);
    }

    public void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.warfactory.medical.client.screen.MedInteractionScreen.onTargetSheetInfo(this));
    }
}
