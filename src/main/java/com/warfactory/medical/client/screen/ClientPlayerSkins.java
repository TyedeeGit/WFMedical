package com.warfactory.medical.client.screen;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TransformTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.warfactory.medical.WFMedical;
import com.warfactory.medical.core.limb.LimbType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.DoubleSupplier;

/**
 * Resolves a player's already-loaded client skin and paints the front-facing region of a single body part
 * into a GUI rect, so the medical body diagram shows WHO is being treated instead of anonymous coloured boxes.
 * No textures are uploaded here -- it reuses {@link AbstractClientPlayer#getSkinTextureLocation()} (the skin the
 * client already streamed/cached for rendering the player), so there is nothing extra to manage or free.
 *
 * <p>The damage look is done per-pixel by the {@code wfmedical:limb_damage} LDLib shader: a healthy limb is the
 * untouched skin; as its health drops the skin desaturates and bleeds toward dark red. Where GUI shaders are
 * unavailable it falls back to a plain skin blit (no damage stain).
 */
public final class ClientPlayerSkins {

    private static final ResourceLocation DAMAGE_SHADER = new ResourceLocation(WFMedical.MOD_ID, "limb_damage");
    private static final float SKIN = 64.0F;

    private ClientPlayerSkins() {
    }

    /** A resolved client skin: the loaded texture plus whether it uses the 3px-wide (slim/Alex) arm model. */
    public record Skin(ResourceLocation texture, boolean slim) {
    }

    /**
     * The skin to draw on a medical sheet. {@code entityId < 0} means the local player (their own sheet);
     * otherwise the tracked entity with that id (a treated teammate). Falls back to the default skin when the
     * entity is not a resolvable client player (e.g. out of tracking range).
     */
    public static Skin forEntity(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = null;
        if (entityId < 0) {
            entity = mc.player;
        } else if (mc.level != null) {
            entity = mc.level.getEntity(entityId);
        }
        if (entity instanceof AbstractClientPlayer player) {
            return new Skin(player.getSkinTextureLocation(), "slim".equals(player.getModelName()));
        }
        return new Skin(DefaultPlayerSkin.getDefaultSkin(), false);
    }

    /**
     * A live texture for one limb tile: the skin's front face for {@code limb}, stained per-pixel by
     * {@code health} (0..1) via the damage shader (or a plain skin blit if GUI shaders are unavailable).
     */
    public static IGuiTexture limbTile(LimbType limb, Skin skin, DoubleSupplier health) {
        Region base = baseRegion(limb, skin.slim());
        Region overlay = overlayRegion(limb, skin.slim());
        if (LDLib.isRemote() && ShaderManager.allowedShader()) {
            try {
                ShaderTexture shared = ShaderTexture.createShader(DAMAGE_SHADER);
                if (shared != null) {
                    return new ShaderLimbTexture(shared, skin.texture(), base, overlay, health);
                }
            } catch (RuntimeException | LinkageError e) {
                WFMedical.LOGGER.warn("[wfmedical] limb_damage shader unavailable; using plain skin blit", e);
            }
        }
        return new SkinPartTexture(skin.texture(), base, overlay);
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    /** A skin sub-region in normalized (0..1) texture space. */
    private record Region(float u, float v, float w, float h) {
    }

    private static Region region(int u, int v, int w, int h) {
        return new Region(u / SKIN, v / SKIN, w / SKIN, h / SKIN);
    }

    private static Region baseRegion(LimbType limb, boolean slim) {
        int armW = slim ? 3 : 4;
        return switch (limb) {
            case HEAD -> region(8, 8, 8, 8);
            case TORSO -> region(20, 20, 8, 12);
            case RIGHT_ARM -> region(44, 20, armW, 12);
            case LEFT_ARM -> region(36, 52, armW, 12);
            case RIGHT_LEG -> region(4, 20, 4, 12);
            case LEFT_LEG -> region(20, 52, 4, 12);
        };
    }

    private static Region overlayRegion(LimbType limb, boolean slim) {
        int armW = slim ? 3 : 4;
        return switch (limb) {
            case HEAD -> region(40, 8, 8, 8);
            case TORSO -> region(20, 36, 8, 12);
            case RIGHT_ARM -> region(44, 36, armW, 12);
            case LEFT_ARM -> region(52, 52, armW, 12);
            case RIGHT_LEG -> region(4, 36, 4, 12);
            case LEFT_LEG -> region(4, 52, 4, 12);
        };
    }

    /**
     * Draws one limb via the shared damage shader. {@link ShaderTexture#createShader} caches one instance per
     * shader id, so all limb tiles share it: we (re)bind the skin + uniforms right before delegating the draw.
     * That is safe because GUI rendering is sequential -- each tile configures then draws before the next runs.
     */
    private static final class ShaderLimbTexture extends TransformTexture {

        private final ShaderTexture shader;
        private final ResourceLocation skin;
        private final Region base;
        private final Region overlay;
        private final DoubleSupplier health;

        private ShaderLimbTexture(ShaderTexture shader, ResourceLocation skin, Region base, Region overlay,
                                  DoubleSupplier health) {
            this.shader = shader;
            this.skin = skin;
            this.base = base;
            this.overlay = overlay;
            this.health = health;
        }

        @Override
        protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y,
                                    int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            shader.bindTexture("Skin", skin);
            shader.setUniformCache(cache -> {
                cache.glUniform4F("uBase", base.u(), base.v(), base.w(), base.h());
                cache.glUniform4F("uOverlay", overlay.u(), overlay.v(), overlay.w(), overlay.h());
                cache.glUniform1F("uHealth", clamp01((float) health.getAsDouble()));
            });
            shader.draw(graphics, mouseX, mouseY, x, y, width, height);
        }
    }

    /** Plain blit fallback (no shader): draws the base body layer then the overlay layer. */
    private static final class SkinPartTexture extends TransformTexture {

        private final ResourceLocation skin;
        private final Region base;
        private final Region overlay;

        private SkinPartTexture(ResourceLocation skin, Region base, Region overlay) {
            this.skin = skin;
            this.base = base;
            this.overlay = overlay;
        }

        @Override
        protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y,
                                    int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            int ix = (int) x;
            int iy = (int) y;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            blit(graphics, ix, iy, width, height, base);
            blit(graphics, ix, iy, width, height, overlay);
            RenderSystem.disableBlend();
        }

        private void blit(GuiGraphics graphics, int ix, int iy, int width, int height, Region r) {
            graphics.blit(skin, ix, iy, width, height,
                    r.u() * SKIN, r.v() * SKIN, Math.round(r.w() * SKIN), Math.round(r.h() * SKIN),
                    (int) SKIN, (int) SKIN);
        }
    }
}
