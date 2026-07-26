package com.warfactory.medical.mixin.tacz;

import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.HitboxHelper;
import com.warfactory.medical.core.damage.MedicalHitReg;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TACZ decides whether a bullet hits an entity AT ALL by clipping the shot's per-tick ray against
 * {@link HitboxHelper#getFixedBoundingBox}, its own (vanilla-derived) lag-compensated box — entirely
 * unaware of WFMedical's rigged per-limb OBBs. A shot landing in a limb box that extends past that
 * narrow box (e.g. an outstretched arm) never even reaches {@code onHitEntity}, so no hurt event fires
 * and the accurate limb classification wired up elsewhere never gets a chance to run.
 * <p>
 * This mirrors {@code ProjectileUtilMixin}, which does the same widening for vanilla projectiles/melee
 * via {@link net.minecraft.world.entity.projectile.ProjectileUtil}: keep TACZ's own box (preserving its
 * lag compensation) but inflate it by the same pose-aware envelope margins used everywhere else in
 * WFMedical's hit registration.
 */
@Mixin(value = EntityUtil.class, remap = false)
public abstract class EntityUtilMixin {

    @Redirect(method = "getHitResult",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/util/HitboxHelper;getFixedBoundingBox("
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/minecraft/world/entity/Entity;"
                            + ")Lnet/minecraft/world/phys/AABB;"))
    private static AABB wfmedical$envelopeBoxTacz(Entity entity, Entity shooterOwner) {
        AABB fixed = HitboxHelper.getFixedBoundingBox(entity, shooterOwner);
        return MedicalHitReg.registrationBox(entity, fixed);
    }
}
