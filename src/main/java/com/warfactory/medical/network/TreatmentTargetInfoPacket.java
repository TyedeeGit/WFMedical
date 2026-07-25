package com.warfactory.medical.network;

import com.warfactory.medical.network.MedicalSyncPacket.LimbSummary;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record TreatmentTargetInfoPacket(int targetEntityId, ResourceLocation itemId, LimbSummary[] limbs,
                                        int treatableMask) {

    public static TreatmentTargetInfoPacket decode(FriendlyByteBuf buf) {
        int targetEntityId = buf.readVarInt();
        ResourceLocation itemId = buf.readResourceLocation();
        int count = buf.readVarInt();
        LimbSummary[] limbs = new LimbSummary[count];
        for (int i = 0; i < count; i++) {
            limbs[i] = MedicalSyncPacket.readLimb(buf);
        }
        int treatableMask = buf.readVarInt();
        return new TreatmentTargetInfoPacket(targetEntityId, itemId, limbs, treatableMask);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(limbs.length);
        for (LimbSummary s : limbs) {
            MedicalSyncPacket.writeLimb(buf, s);
        }
        buf.writeVarInt(treatableMask);
    }

    public void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.warfactory.medical.client.TreatmentInteractions.onTargetInfo(this));
    }
}
