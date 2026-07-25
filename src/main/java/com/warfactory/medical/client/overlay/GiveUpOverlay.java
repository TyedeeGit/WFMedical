package com.warfactory.medical.client.overlay;

import com.warfactory.medical.client.GiveUpHandler;
import com.warfactory.medical.client.MedicalKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public final class GiveUpOverlay implements IGuiOverlay {

    public static final IGuiOverlay INSTANCE = new GiveUpOverlay();
    public static final String OVERLAY_ID = "wfmedical_give_up";

    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_BG = 0xC0202020;
    private static final int BAR_BORDER = 0xFF000000;
    private static final int BAR_FILL = 0xFFCC3030;
    private static final int TEXT_COLOR = 0xFFE6E6E6;

    private GiveUpOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenW, int screenH) {
        if (!GiveUpHandler.available()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String key = MedicalKeyMappings.GIVE_UP.getTranslatedKeyMessage().getString();
        Component prompt = Component.translatable("gui.wfmedical.give_up.prompt", key);

        int cx = screenW / 2;
        int textY = screenH - 64;
        graphics.drawCenteredString(mc.font, prompt, cx, textY, TEXT_COLOR);

        float progress = GiveUpHandler.progress01();
        if (progress > 0.0F) {
            int barX = cx - BAR_WIDTH / 2;
            int barY = textY + 12;
            graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BAR_BORDER);
            graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_BG);
            int fillW = (int) (BAR_WIDTH * progress);
            graphics.fill(barX, barY, barX + fillW, barY + BAR_HEIGHT, BAR_FILL);
        }
    }
}
