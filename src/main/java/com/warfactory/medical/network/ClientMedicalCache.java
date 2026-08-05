package com.warfactory.medical.network;

import com.warfactory.medical.WFMedical;
import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.core.DerivedStats;
import com.warfactory.medical.core.HealthState;
import com.warfactory.medical.core.limb.LimbType;

public final class ClientMedicalCache {

    private static volatile MedicalSyncPacket snapshot;
    private static volatile ActiveTreatmentPacket activeTreatment;
    private static volatile boolean debug;
    private static volatile LimbType selectedLimb;

    private ClientMedicalCache() {
    }

    public static MedicalSyncPacket get() {
        return snapshot;
    }

    public static void set(MedicalSyncPacket packet) {
        snapshot = packet;
        if (MedicalConfig.logMedicalSync()) {
            WFMedical.LOGGER.info("[wfmed-sync] FULL  apply {}", summarize(packet));
        }
    }

    public static void applyDelta(MedicalDeltaPacket delta) {
        MedicalSyncPacket base = snapshot;
        boolean log = MedicalConfig.logMedicalSync();
        if (base == null) {
            if (log) {
                WFMedical.LOGGER.warn("[wfmed-sync] DELTA dropped: no client baseline (mask={})",
                        Integer.toBinaryString(delta.mask()));
            }
            return;
        }
        snapshot = delta.applyTo(base);
        if (log) {
            WFMedical.LOGGER.info("[wfmed-sync] DELTA apply mask={} {}",
                    Integer.toBinaryString(delta.mask()), summarize(snapshot));
        }
    }

    /** Compact per-limb health%/bleed/pain/wound-count line for sync tracing (see logMedicalSync). */
    private static String summarize(MedicalSyncPacket packet) {
        if (packet == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder(96);
        sb.append("state=").append(packet.state()).append(" blood=").append(Math.round(packet.bloodMl()))
                .append(" limbs[");
        MedicalSyncPacket.LimbSummary[] limbs = packet.limbs();
        if (limbs != null) {
            for (int i = 0; i < limbs.length; i++) {
                MedicalSyncPacket.LimbSummary s = limbs[i];
                if (s == null) {
                    continue;
                }
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(s.limb()).append('=').append(Math.round(s.healthPercent() * 100.0F)).append('%');
                if (s.bleeding() > 0.0F) {
                    sb.append(",bleed").append(String.format(java.util.Locale.ROOT, "%.2f", s.bleeding()));
                }
                if (s.pain() > 0.0F) {
                    sb.append(",pain").append(String.format(java.util.Locale.ROOT, "%.2f", s.pain()));
                }
                if (s.fracture()) {
                    sb.append(",fx");
                }
                int w = s.wounds() == null ? 0 : s.wounds().size();
                if (w > 0) {
                    sb.append(",w").append(w);
                }
            }
        }
        return sb.append(']').toString();
    }

    public static DerivedStats stats() {
        MedicalSyncPacket s = snapshot;
        return s == null ? DerivedStats.healthy() : s.stats();
    }

    public static HealthState state() {
        MedicalSyncPacket s = snapshot;
        return s == null ? HealthState.HEALTHY : s.state();
    }

    public static float painSuppression() {
        MedicalSyncPacket s = snapshot;
        return s == null ? 0.0F : s.painSuppression();
    }

    public static float drugLoad() {
        MedicalSyncPacket s = snapshot;
        return s == null ? 0.0F : s.drugLoad();
    }

    public static float deathProgress() {
        MedicalSyncPacket s = snapshot;
        return s == null ? 0.0F : s.deathProgress();
    }


    public static void setActiveTreatment(ActiveTreatmentPacket packet) {
        activeTreatment = packet;
    }

    public static ActiveTreatmentPacket activeTreatment() {
        return activeTreatment;
    }

    public static boolean hasActiveTreatment() {
        ActiveTreatmentPacket a = activeTreatment;
        return a != null && a.active();
    }


    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static boolean toggleDebug() {
        debug = !debug;
        return debug;
    }

    public static LimbType selectedLimb() {
        return selectedLimb;
    }

    public static void setSelectedLimb(LimbType limb) {
        selectedLimb = limb;
    }

    public static void clear() {
        snapshot = null;
        activeTreatment = null;
        selectedLimb = null;
        debug = false;
    }
}
