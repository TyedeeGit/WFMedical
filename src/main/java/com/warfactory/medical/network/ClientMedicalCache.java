package com.warfactory.medical.network;

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
    }

    public static void applyDelta(MedicalDeltaPacket delta) {
        MedicalSyncPacket base = snapshot;
        if (base != null) {
            snapshot = delta.applyTo(base);
        }
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
