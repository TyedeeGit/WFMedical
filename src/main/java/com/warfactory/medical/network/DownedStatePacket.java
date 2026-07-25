package com.warfactory.medical.network;

import com.warfactory.medical.client.ClientDownedTracker;
import com.warfactory.medical.core.MedicalProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

public record DownedStatePacket(int entityId, boolean downed) {

    public static DownedStatePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        boolean downed = buf.readBoolean();
        return new DownedStatePacket(entityId, downed);
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public boolean downed() {
        return downed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(downed);
    }

    public void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientDownedTracker.set(entityId, downed));
    }
}
