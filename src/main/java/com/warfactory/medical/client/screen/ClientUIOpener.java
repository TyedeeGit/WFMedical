package com.warfactory.medical.client.screen;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import net.minecraft.client.Minecraft;

public final class ClientUIOpener {

    private ClientUIOpener() {
    }

    public static void openClientUI(ModularUI ui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ui == null) {
            return;
        }
        ui.initWidgets();
        int windowId = mc.player.containerMenu.containerId;
        ModularUIGuiContainer gui = new ModularUIGuiContainer(ui, windowId);
        mc.setScreen(gui);
        mc.player.containerMenu = gui.getMenu();
    }
}
