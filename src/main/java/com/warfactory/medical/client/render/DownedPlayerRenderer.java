package com.warfactory.medical.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warfactory.medical.WFMedical;
import com.warfactory.medical.client.ClientDownedTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WFMedical.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DownedPlayerRenderer {

    private static final float GROUND_LIFT = 0.1F;
    private static final float LAY_DEGREES = -90.0F;
    private static final float STABLE_YAW = 8.0F;

    private static boolean applied;

    private DownedPlayerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || !ClientDownedTracker.isDowned(player.getId())) {
            return;
        }
        try {
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            applied = true;
            pose.translate(0.0F, GROUND_LIFT, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(LAY_DEGREES));
            pose.mulPose(Axis.YP.rotationDegrees(STABLE_YAW));
        } catch (Throwable t) {
            WFMedical.LOGGER.warn("[{}] Downed body pose failed; skipping this frame", WFMedical.MOD_ID, t);
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        if (!applied) {
            return;
        }
        applied = false;
        try {
            event.getPoseStack().popPose();
        } catch (Throwable t) {
            WFMedical.LOGGER.warn("[{}] Downed body pose cleanup failed", WFMedical.MOD_ID, t);
        }
    }
}
