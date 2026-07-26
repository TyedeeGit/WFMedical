package com.warfactory.medical.core.damage;

import com.warfactory.medical.WFMedical;
import com.warfactory.medical.api.MedicalState;
import com.warfactory.medical.core.HealthState;
import com.warfactory.medical.core.damage.rig.HumanoidRig;
import com.warfactory.medical.core.damage.rig.Obb;
import com.warfactory.medical.core.damage.rig.RigTuning;
import com.warfactory.medical.core.limb.LimbType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Diagnostic-only tracing for the whole hit-location pipeline, gated entirely behind
 * {@link com.warfactory.medical.config.MedicalConfig#logHitDetection()} so it costs nothing when off.
 * Covers three distinct failure modes reported in practice:
 * <ol>
 *   <li>TACZ's own broad-phase collision test missing a shot the rig OBBs would have classified
 *       ({@link #logBulletPath}) -- "shooting the arm does nothing".</li>
 *   <li>{@link HitGeometry#rigPointPick} picking the wrong limb at close range because the captured hit
 *       point sits near two overlapping OBB surfaces ({@link #logRigPointPick}) -- "arm shot damaged the
 *       head".</li>
 *   <li>The player's pose flipping to the DOWNED rig (or the classifier giving up and falling back to a
 *       random limb) between the shot being fired and it being classified ({@link #logClassifyHit},
 *       {@link #logRandomFallback}, {@link #logStateTransition}) -- "damage redirected to a different limb
 *       at zero health".</li>
 * </ol>
 */
public final class HitDetectionDebug {

    private static final double SCAN_MARGIN = 3.0;
    private static final double AMBIGUOUS_MARGIN = 0.15;

    private HitDetectionDebug() {
    }

    public static void logBulletPath(Projectile bullet, Vec3 start, Vec3 end, Entity taczHit) {
        if (!(bullet.level() instanceof ServerLevel level)) {
            return;
        }
        Entity owner = bullet.getOwner();
        AABB scan = new AABB(start, end).inflate(SCAN_MARGIN);
        List<Player> nearby = level.getEntitiesOfClass(Player.class, scan, p -> p != owner);

        for (Player candidate : nearby) {
            LimbType rigLimb = HitGeometry.classifyRay(candidate, start, end);
            boolean vanillaHit = candidate.getBoundingBox().clip(start, end).isPresent();
            boolean taczRegisteredThis = candidate == taczHit;

            if (rigLimb == null && !vanillaHit && !taczRegisteredThis) {
                continue;
            }

            if (rigLimb != null && !taczRegisteredThis) {
                WFMedical.LOGGER.warn(
                        "[{}] HITBOX GAP: shot from {} would classify as {}'s {} via the rig OBB, but TACZ's own "
                                + "collision test did not register a hit on them at all (vanillaAABBHit={}) -- no "
                                + "damage/hurt event will fire for this shot",
                        WFMedical.MOD_ID, ownerName(owner), candidate.getName().getString(), rigLimb, vanillaHit);
            } else {
                WFMedical.LOGGER.info(
                        "[{}] hit-scan: shooter={} target={} vanillaAABBHit={} rigLimb={} taczRegisteredHit={}",
                        WFMedical.MOD_ID, ownerName(owner), candidate.getName().getString(), vanillaHit, rigLimb,
                        taczRegisteredThis);
            }
        }
    }

    /**
     * Logs the full ranked distance-to-surface for every limb OBB a point-pick decision considered, not
     * just the winner, so a near-tie between (say) HEAD and RIGHT_ARM at close range is directly visible
     * instead of hidden behind a single silently-chosen result.
     */
    public static void logRigPointPick(LivingEntity victim, DamageSource src, String branch,
                                        HumanoidRig.LocalRig rig, Vec3 local, LimbType winner) {
        Obb[] all = rig.all();
        int n = all.length;
        int[] order = new int[n];
        double[] dist = new double[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            dist[i] = Math.sqrt(all[i].distanceSq(local));
        }
        for (int i = 1; i < n; i++) {
            int oi = order[i];
            double di = dist[oi];
            int j = i - 1;
            while (j >= 0 && dist[order[j]] > di) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = oi;
        }

        StringBuilder ranked = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                ranked.append(", ");
            }
            ranked.append(all[order[i]].limb()).append('=').append(String.format("%.3f", dist[order[i]]));
        }

        double margin = n > 1 ? dist[order[1]] - dist[order[0]] : Double.POSITIVE_INFINITY;
        boolean ambiguous = margin < AMBIGUOUS_MARGIN;
        Entity attacker = src.getEntity();

        if (ambiguous) {
            WFMedical.LOGGER.warn(
                    "[{}] AMBIGUOUS POINT-PICK ({}): shot from {} on {} chose {} but the runner-up was only "
                            + "{}m farther (margin < {}m) -- ranked distances: [{}]",
                    WFMedical.MOD_ID, branch, ownerName(attacker), victim.getName().getString(), winner,
                    String.format("%.3f", margin), AMBIGUOUS_MARGIN, ranked);
        } else {
            WFMedical.LOGGER.info(
                    "[{}] point-pick ({}): shot from {} on {} -> {} -- ranked distances: [{}]",
                    WFMedical.MOD_ID, branch, ownerName(attacker), victim.getName().getString(), winner, ranked);
        }
    }

    /**
     * Top-level trace for every real damage event that reaches {@link HitGeometry#classifyHit}: which
     * branch produced the result, plus the victim's pose/downed/health context at that exact moment, so a
     * mid-fight pose flip (standing -> DOWNED rig) landing on a shot fired before the flip is visible
     * directly in the log instead of just looking like a wrong limb.
     */
    public static void logClassifyHit(LivingEntity victim, DamageSource src, DamageCategory cat, String branch,
                                       LimbType result) {
        Entity attacker = src.getEntity();
        double dist = attacker != null ? attacker.position().distanceTo(victim.position()) : -1.0;
        boolean downed = victim instanceof Player player && MedicalState.isDowned(player);
        RigTuning.RigPose pose = HumanoidRig.resolvePose(victim);

        WFMedical.LOGGER.info(
                "[{}] classify: attacker={} victim={} cat={} branch={} result={} downed={} pose={} "
                        + "victimHealth={} dist={}",
                WFMedical.MOD_ID, ownerName(attacker), victim.getName().getString(), cat, branch, result, downed,
                pose, String.format("%.1f", victim.getHealth()),
                dist < 0.0 ? "?" : String.format("%.2f", dist));
    }

    /**
     * Logs whenever geometric classification gave up entirely and {@link HitLocation} fell back to a
     * random weighted limb -- the most literal form of "damage redirected to a different limb".
     */
    public static void logRandomFallback(LivingEntity victim, DamageSource src, DamageCategory cat,
                                          LimbType chosen) {
        Entity attacker = src == null ? null : src.getEntity();
        boolean downed = victim instanceof Player player && MedicalState.isDowned(player);
        WFMedical.LOGGER.warn(
                "[{}] RANDOM FALLBACK: no geometric hit location for attacker={} victim={} cat={} "
                        + "(downed={}, health={}) -- randomly assigned {}",
                WFMedical.MOD_ID, ownerName(attacker), victim.getName().getString(), cat, downed,
                String.format("%.1f", victim.getHealth()), chosen);
    }

    /**
     * Logs edge-triggered {@code HealthState} transitions (e.g. HEALTHY -> UNCONSCIOUS), so a shot's
     * classify-log timestamp can be lined up against exactly when the victim's rig pose flipped to DOWNED.
     */
    public static void logStateTransition(Player player, HealthState before, HealthState after, long gameTime) {
        WFMedical.LOGGER.info(
                "[{}] state transition: {} {} -> {} at tick {} (health={})",
                WFMedical.MOD_ID, player.getName().getString(), before, after, gameTime,
                String.format("%.1f", player.getHealth()));
    }

    private static String ownerName(Entity owner) {
        return owner == null ? "?" : owner.getName().getString();
    }
}
