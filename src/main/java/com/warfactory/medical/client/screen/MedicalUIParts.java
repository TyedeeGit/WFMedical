package com.warfactory.medical.client.screen;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.warfactory.medical.core.DerivedStats;
import com.warfactory.medical.core.HealthState;
import com.warfactory.medical.core.limb.LimbType;
import com.warfactory.medical.item.MedicalItem;
import com.warfactory.medical.network.*;
import com.warfactory.medical.network.MedicalSyncPacket.LimbSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Function;

public final class MedicalUIParts {

    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int HIGHLIGHT_BORDER = 2;

    private MedicalUIParts() {
    }


    public static int limbColor(float healthPercent01) {
        float p = healthPercent01;
        if (p < 0.0F) {
            p = 0.0F;
        } else if (p > 1.0F) {
            p = 1.0F;
        }
        int r;
        int g;
        if (p < 0.5F) {
            r = 255;
            g = Math.round(255.0F * (p * 2.0F));
        } else {
            r = Math.round(255.0F * ((1.0F - p) * 2.0F));
            g = 255;
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    public static int stateColor(HealthState state) {
        if (state == null) {
            return 0xFFFFFFFF;
        }
        return switch (state) {
            case HEALTHY -> 0xFF33CC33;
            case CRITICAL -> 0xFFE0A020;
            case UNCONSCIOUS -> 0xFFCC3030;
            case DEAD -> 0xFF404040;
        };
    }


    public static LimbType selectedLimb() {
        return ClientMedicalCache.selectedLimb();
    }

    public static void selectLimb(LimbType limb) {
        ClientMedicalCache.setSelectedLimb(limb);
    }


    public static void requestAction(ItemStack medicalItemStack, LimbType limb) {
        requestAction(medicalItemStack, limb, -1);
    }

    public static void requestAction(ItemStack medicalItemStack, LimbType limb, int targetEntityId) {
        if (medicalItemStack == null || medicalItemStack.isEmpty()
                || !(medicalItemStack.getItem() instanceof MedicalItem)) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(medicalItemStack.getItem());
        if (id == null) {
            return;
        }
        MedicalNetworking.sendToServer(new MedicalActionPacket(id, limb, targetEntityId));
    }

    public static void requestRemoveTourniquet(LimbType limb) {
        requestRemoveTourniquet(limb, -1);
    }

    public static void requestRemoveTourniquet(LimbType limb, int targetEntityId) {
        if (limb == null) {
            return;
        }
        MedicalNetworking.sendToServer(new RemoveTourniquetPacket(limb, targetEntityId));
    }

    public static void requestCancelTreatment() {
        MedicalNetworking.sendToServer(new CancelTreatmentPacket());
    }

    public static List<ItemStack> availableMedicalItems() {
        List<ItemStack> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return out;
        }
        Inventory inv = mc.player.getInventory();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof MedicalItem && seen.add(stack.getItem())) {
                out.add(stack);
            }
        }
        return out;
    }


    public static DerivedStats stats() {
        return ClientMedicalCache.stats();
    }

    public static LimbSummary[] limbSummaries() {
        LimbType[] all = LimbType.VALUES;
        LimbSummary[] out = new LimbSummary[all.length];
        for (int i = 0; i < all.length; i++) {
            out[i] = limbSummary(all[i]);
        }
        return out;
    }

    public static LimbSummary limbSummary(LimbType limb) {
        MedicalSyncPacket snap = ClientMedicalCache.get();
        if (snap != null) {
            LimbSummary[] limbs = snap.limbs();
            if (limbs != null) {
                for (LimbSummary s : limbs) {
                    if (s != null && s.limb() == limb) {
                        return s;
                    }
                }
            }
        }
        return new LimbSummary(limb, 1.0F, 0.0F, 0.0F, false, java.util.List.of());
    }


    public static Component limbName(LimbType limb) {
        return Component.translatable("gui.wfmedical.limb." + limb.name().toLowerCase(Locale.ROOT));
    }

    public static Component stateName(HealthState state) {
        HealthState s = state == null ? HealthState.HEALTHY : state;
        return Component.translatable("gui.wfmedical.state." + s.name().toLowerCase(Locale.ROOT));
    }


    public static WidgetGroup bodyDiagram(int x, int y, int width, int height) {
        WidgetGroup group = new WidgetGroup(x, y, width, height);

        int colW = Math.max(1, width / 3);
        int rowH = Math.max(1, height / 4);
        int halfCol = Math.max(1, colW / 2);
        int gap = 1;

        addLimbTile(group, LimbType.HEAD, colW + gap, gap, colW - 2 * gap, rowH - 2 * gap);
        addLimbTile(group, LimbType.LEFT_ARM, gap, rowH + gap, colW - 2 * gap, rowH - 2 * gap);
        addLimbTile(group, LimbType.TORSO, colW + gap, rowH + gap, colW - 2 * gap, 2 * rowH - 2 * gap);
        addLimbTile(group, LimbType.RIGHT_ARM, 2 * colW + gap, rowH + gap, colW - 2 * gap, rowH - 2 * gap);
        addLimbTile(group, LimbType.LEFT_LEG, colW + gap, 3 * rowH + gap, halfCol - 2 * gap, rowH - 2 * gap);
        addLimbTile(group, LimbType.RIGHT_LEG, colW + halfCol + gap, 3 * rowH + gap, halfCol - 2 * gap, rowH - 2 * gap);

        group.setClientSideWidget();
        return group;
    }

    public static void addLimbTile(WidgetGroup group, LimbType limb, int tx, int ty, int tw, int th) {
        addLimbTile(group, limb, tx, ty, tw, th, MedicalUIParts::limbSummary);
    }

    public static void addLimbTile(WidgetGroup group, LimbType limb, int tx, int ty, int tw, int th,
                                   Function<LimbType, LimbSummary> source) {
        if (tw <= 0 || th <= 0) {
            return;
        }

        ImageWidget fill = new ImageWidget(tx, ty, tw, th,
                () ->
                        new ColorRectTexture(limbColor(source.apply(limb).healthPercent())));
        fill.setClientSideWidget();
        group.addWidget(fill);

        ImageWidget border = new ImageWidget(tx, ty, tw, th,
                () ->
                        selectedLimb() == limb
                                ? new ColorBorderTexture(HIGHLIGHT_BORDER, HIGHLIGHT_COLOR)
                                : IGuiTexture.EMPTY);
        border.setClientSideWidget();
        group.addWidget(border);

        ButtonWidget click = new ButtonWidget(tx, ty, tw, th,
                (ClickData cd) -> selectLimb(limb)) {
            @Override
            public void updateScreen() {
                super.updateScreen();
                setHoverTooltips(limbTooltip(limb, source));
            }
        };
        click.setHoverTooltips(limbTooltip(limb, source));
        click.setClientSideWidget();
        group.addWidget(click);
    }

    public static List<Component> limbTooltip(LimbType limb) {
        return limbTooltip(limb, MedicalUIParts::limbSummary);
    }

    public static List<Component> limbTooltip(LimbType limb, Function<LimbType, LimbSummary> source) {
        LimbSummary s = source.apply(limb);
        List<Component> lines = new ArrayList<>(4);
        lines.add(limbName(limb));
        lines.add(Component.translatable("gui.wfmedical.health")
                .append(Component.literal(": " + Math.round(s.healthPercent() * 100.0F) + "%")));
        lines.add(Component.translatable("gui.wfmedical.bleeding")
                .append(Component.literal(": " + String.format(Locale.ROOT, "%.1f", s.bleeding()))));
        lines.add(Component.translatable("gui.wfmedical.pain")
                .append(Component.literal(": " + String.format(Locale.ROOT, "%.1f", s.pain()))));
        if (s.fracture()) {
            lines.add(Component.translatable("gui.wfmedical.fracture"));
        }
        return lines;
    }
}
