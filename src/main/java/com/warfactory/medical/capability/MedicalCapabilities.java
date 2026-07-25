package com.warfactory.medical.capability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class MedicalCapabilities {

    public static final Capability<IMedicalData> MEDICAL =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private MedicalCapabilities() {
    }

    public static IMedicalData get(Player player) {
        if (player == null) {
            return null;
        }
        return player.getCapability(MEDICAL).resolve().orElse(null);
    }

    public static void copy(Player original, Player clone) {
        IMedicalData oldData = get(original);
        IMedicalData newData = get(clone);
        if (oldData == null || newData == null) {
            return;
        }
        newData.load(oldData.save());
        newData.bumpRevision();
    }
}
