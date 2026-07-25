package com.warfactory.medical.item;

import com.warfactory.medical.core.substance.Substance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class InjectableItem extends MedicalItem {

    private final Substance substance;

    public InjectableItem(Properties properties, Substance substance) {
        super(properties, null);
        this.substance = substance;
    }

    public Substance getSubstance() {
        return substance;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        int ticks = substance != null ? substance.useDurationTicks() : 0;
        return ticks > 0 ? ticks : 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
