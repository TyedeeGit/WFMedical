package com.warfactory.medical.client;

import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.network.ClientMedicalCache;
import com.warfactory.medical.network.GiveUpPacket;
import com.warfactory.medical.network.MedicalNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class GiveUpHandler {

    private static int held;
    private static boolean sent;

    private GiveUpHandler() {
    }

    public static void tick(Minecraft mc) {
        if (available(mc) && MedicalKeyMappings.GIVE_UP.isDown()) {
            held++;
            if (!sent && held >= threshold()) {
                MedicalNetworking.sendToServer(new GiveUpPacket());
                sent = true;
            }
        } else {
            held = 0;
            sent = false;
        }
    }

    public static boolean available() {
        return available(Minecraft.getInstance());
    }

    private static boolean available(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null
                && mc.screen == null
                && MedicalConfig.enableGiveUp()
                && ClientMedicalCache.stats().unconscious();
    }

    public static float progress01() {
        int t = threshold();
        float p = (float) held / (float) t;
        return p < 0.0F ? 0.0F : (Math.min(p, 1.0F));
    }

    private static int threshold() {
        return Math.max(1, MedicalConfig.giveUpHoldTicks());
    }
}
