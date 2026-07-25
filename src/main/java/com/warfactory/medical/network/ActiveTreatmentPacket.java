package com.warfactory.medical.network;

import com.warfactory.medical.core.limb.LimbType;
import com.warfactory.medical.core.treatment.TreatmentAction;
import net.minecraft.network.FriendlyByteBuf;

public record ActiveTreatmentPacket(boolean active, TreatmentAction action, LimbType limb, int totalTicks,
                                    long startGameTime, int targetEntityId) {

    public static ActiveTreatmentPacket inactive() {
        return new ActiveTreatmentPacket(false, null, null, 0, 0L, -1);
    }

    public static ActiveTreatmentPacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        if (!active) {
            return inactive();
        }
        TreatmentAction action = buf.readEnum(TreatmentAction.class);
        LimbType limb = buf.readBoolean() ? buf.readEnum(LimbType.class) : null;
        int totalTicks = buf.readVarInt();
        long startGameTime = buf.readLong();
        int targetEntityId = buf.readVarInt();
        return new ActiveTreatmentPacket(true, action, limb, totalTicks, startGameTime, targetEntityId);
    }

    @Override
    public TreatmentAction action() {
        return action;
    }

    @Override
    public LimbType limb() {
        return limb;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        if (!active) {
            return;
        }
        buf.writeEnum(action);
        boolean hasLimb = limb != null;
        buf.writeBoolean(hasLimb);
        if (hasLimb) {
            buf.writeEnum(limb);
        }
        buf.writeVarInt(totalTicks);
        buf.writeLong(startGameTime);
        buf.writeVarInt(targetEntityId);
    }

    @Override
    public int targetEntityId() {
        return targetEntityId;
    }

    public void handleClient() {
        ClientMedicalCache.setActiveTreatment(this);
    }
}
