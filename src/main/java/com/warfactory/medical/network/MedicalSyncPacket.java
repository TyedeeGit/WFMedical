package com.warfactory.medical.network;

import com.warfactory.medical.core.DerivedStats;
import com.warfactory.medical.core.HealthState;
import com.warfactory.medical.core.MedicalProfile;
import com.warfactory.medical.core.limb.Limb;
import com.warfactory.medical.core.limb.LimbType;
import net.minecraft.network.FriendlyByteBuf;

public record MedicalSyncPacket(DerivedStats stats, LimbSummary[] limbs, double bloodMl, double maxBloodMl,
                                float painSuppression, float drugLoad, HealthState state, float deathProgress) {

    public static MedicalSyncPacket fromProfile(MedicalProfile profile) {
        LimbType[] all = LimbType.VALUES;
        LimbSummary[] summaries = new LimbSummary[all.length];
        for (int i = 0; i < all.length; i++) {
            Limb limb = profile.limb(all[i]);
            float max = limb.getMaxHealth();
            float remaining = max - limb.getCachedHealthReduction() - limb.getMinorDamage();
            float pct = max <= 0.0F ? 0.0F : remaining / max;
            if (pct < 0.0F) {
                pct = 0.0F;
            } else if (pct > 1.0F) {
                pct = 1.0F;
            }
            summaries[i] = new LimbSummary(
                    all[i],
                    pct,
                    (float) limb.getCachedBleeding(),
                    limb.getCachedPain(),
                    limb.hasCachedFracture());
        }
        return new MedicalSyncPacket(
                profile.cached(),
                summaries,
                profile.getBloodMl(),
                profile.getMaxBloodMl(),
                profile.getPainSuppression(),
                profile.getDrugLoad(),
                profile.getState(),
                profile.getDeathProgress());
    }

    public static MedicalSyncPacket decode(FriendlyByteBuf buf) {
        DerivedStats stats = readStats(buf);
        double bloodMl = buf.readDouble();
        double maxBloodMl = buf.readDouble();
        float painSuppression = buf.readFloat();
        float drugLoad = buf.readFloat();
        HealthState state = buf.readEnum(HealthState.class);
        int count = buf.readVarInt();
        LimbSummary[] limbs = new LimbSummary[count];
        for (int i = 0; i < count; i++) {
            limbs[i] = readLimb(buf);
        }
        float deathProgress = buf.readFloat();
        return new MedicalSyncPacket(stats, limbs, bloodMl, maxBloodMl, painSuppression, drugLoad, state,
                deathProgress);
    }

    static void writeStats(FriendlyByteBuf buf, DerivedStats s) {
        buf.writeFloat(s.effectiveMaxHealth());
        buf.writeFloat(s.healthModifier());
        buf.writeFloat(s.effectiveCurrentHealth());
        buf.writeDouble(s.totalBleeding());
        buf.writeFloat(s.totalPain());
        buf.writeFloat(s.systemicPain());
        buf.writeFloat(s.movementMultiplier());
        buf.writeBoolean(s.sprintBlocked());
        buf.writeFloat(s.jumpMultiplier());
        buf.writeEnum(s.state());
        buf.writeBoolean(s.anyLegFracture());
        buf.writeBoolean(s.anyArmFracture());
        buf.writeBoolean(s.asphyxiating());
        buf.writeBoolean(s.painKoPending());
        buf.writeBoolean(s.bothArmsDisabled());
        buf.writeBoolean(s.bothLegsDisabled());
        buf.writeBoolean(s.anyArmTourniquet());
    }

    static DerivedStats readStats(FriendlyByteBuf buf) {
        return new DerivedStats(
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat(),
                buf.readEnum(HealthState.class),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    static void writeLimb(FriendlyByteBuf buf, LimbSummary s) {
        buf.writeEnum(s.limb());
        buf.writeFloat(s.healthPercent());
        buf.writeFloat(s.bleeding());
        buf.writeFloat(s.pain());
        buf.writeBoolean(s.fracture());
    }


    static LimbSummary readLimb(FriendlyByteBuf buf) {
        return new LimbSummary(buf.readEnum(LimbType.class), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readBoolean());
    }

    @Override
    public float painSuppression() {
        return painSuppression;
    }

    @Override
    public float drugLoad() {
        return drugLoad;
    }

    public void encode(FriendlyByteBuf buf) {
        writeStats(buf, stats);
        buf.writeDouble(bloodMl);
        buf.writeDouble(maxBloodMl);
        buf.writeFloat(painSuppression);
        buf.writeFloat(drugLoad);
        buf.writeEnum(state);
        buf.writeVarInt(limbs.length);
        for (LimbSummary s : limbs) {
            writeLimb(buf, s);
        }
        buf.writeFloat(deathProgress);
    }

    public void handleClient() {
        ClientMedicalCache.set(this);
    }

    public record LimbSummary(LimbType limb, float healthPercent, float bleeding, float pain, boolean fracture) {
    }
}
