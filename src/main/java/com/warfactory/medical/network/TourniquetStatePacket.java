package com.warfactory.medical.network;

import com.warfactory.medical.client.ClientTourniquetTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record TourniquetStatePacket(int entityId, int mask) {

    public static TourniquetStatePacket decode(FriendlyByteBuf buf) {
        return new TourniquetStatePacket(buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(mask);
    }

    public void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientTourniquetTracker.set(entityId, mask));
    }
}
