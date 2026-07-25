package com.warfactory.medical.item;

import com.warfactory.medical.client.render.MedicalItemRenderer;
import com.warfactory.medical.core.treatment.Treatment;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class MedicalItem extends Item {

    private final Treatment treatment;
    private final UseAnim useAnim;

    public MedicalItem(Properties properties, Treatment treatment) {
        this(properties, treatment, UseAnim.BOW);
    }

    public MedicalItem(Properties properties, Treatment treatment, boolean eatAnim) {
        this(properties, treatment, eatAnim ? UseAnim.EAT : UseAnim.BOW);
    }

    public MedicalItem(Properties properties, Treatment treatment, UseAnim useAnim) {
        super(properties);
        this.treatment = treatment;
        this.useAnim = useAnim;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        int ticks = treatment != null ? treatment.useDurationTicks() : 0;
        return ticks > 0 ? ticks : 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnim;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MedicalItemRenderer.get();
            }
        });
    }
}
