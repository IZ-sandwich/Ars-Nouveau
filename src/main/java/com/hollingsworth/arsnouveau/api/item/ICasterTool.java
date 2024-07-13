package com.hollingsworth.arsnouveau.api.item;

import com.hollingsworth.arsnouveau.api.client.IDisplayMana;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.ItemCasterProvider;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.items.SpellParchment;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An interface for caster items that provides default behavior for scribing, displaying mana, and tooltips
 */
// TODO 1.20: Split ISpellHotkeyListener out
public interface ICasterTool extends IScribeable, IDisplayMana, ISpellHotkeyListener, ItemCasterProvider {
    @Override
    default boolean onScribe(Level world, BlockPos pos, Player player, InteractionHand handIn, ItemStack tableStack) {
        ItemStack heldStack = player.getItemInHand(handIn);
        AbstractCaster<?> tableCaster = SpellCasterRegistry.from(tableStack);
        if (!((heldStack.getItem() instanceof SpellBook) || (heldStack.getItem() instanceof SpellParchment) || (heldStack.getItem() == ItemsRegistry.MANIPULATION_ESSENCE.asItem())))
            return false;
        if(tableCaster == null){
            return false;
        }
        boolean success;

        AbstractCaster<?> heldCaster = SpellCasterRegistry.from(heldStack);
        Spell spell = new Spell();
        if (heldCaster != null) {
            spell = heldCaster.getSpell();
            tableCaster.setColor(heldCaster.getColor())
                    .setFlavorText(heldCaster.getFlavorText())
                    .setSpellName(heldCaster.getSpellName())
                    .setSound(heldCaster.getCurrentSound())
                    .saveToStack(tableStack);

        } else if (heldStack.getItem() == ItemsRegistry.MANIPULATION_ESSENCE.asItem()) {
            // Thanks mojang
            String[] words = new String[]{"the", "elder", "scrolls", "klaatu", "berata", "niktu", "xyzzy", "bless", "curse", "light", "darkness", "fire", "air", "earth", "water", "hot", "dry", "cold", "wet", "ignite", "snuff", "embiggen", "twist", "shorten", "stretch", "fiddle", "destroy", "imbue", "galvanize", "enchant", "free", "limited", "range", "of", "towards", "inside", "sphere", "cube", "self", "other", "ball", "mental", "physical", "grow", "shrink", "demon", "elemental", "spirit", "animal", "creature", "beast", "humanoid", "undead", "fresh", "stale", "phnglui", "mglwnafh", "cthulhu", "rlyeh", "wgahnagl", "fhtagn", "baguette"};
            // Pick between 3 and 5 words
            int numWords = world.random.nextInt(3) + 3;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numWords; i++) {
                sb.append(words[world.random.nextInt(words.length)]).append(" ");
            }
            tableCaster.setHidden(true)
                    .setHiddenRecipe(sb.toString())
                    .saveToStack(tableStack);
            PortUtil.sendMessageNoSpam(player, Component.translatable("ars_nouveau.spell_hidden"));
            return true;
        }
        if (isScribedSpellValid(tableCaster, player, handIn, tableStack, spell)) {
            var mutableSpell = spell.mutable();
            scribeModifiedSpell(tableCaster, player, handIn, tableStack, mutableSpell);
            tableCaster.setSpell(mutableSpell.immutable()).saveToStack(tableStack);
            sendSetMessage(player);
        } else {
            sendInvalidMessage(player);
        }
        return false;
    }

    default void sendSetMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("ars_nouveau.set_spell"));
    }

    default void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("ars_nouveau.invalid_spell"));
    }

    @Override
    default @NotNull AbstractCaster<?> getSpellCaster(ItemStack stack) {
        return SpellCasterRegistry.from(stack);
    }

    default void scribeModifiedSpell(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell.Mutable spell){}

    default boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return spell.isValid();
    }

    @Override
    default boolean shouldDisplay(ItemStack stack) {
        return true;
    }

    default void getInformation(ItemStack stack, Item.TooltipContext context, List<Component> tooltip2, TooltipFlag flagIn) {
        AbstractCaster<?> caster = getSpellCaster(stack);
        stack.addToTooltip(caster.getComponentType(), context, tooltip2::add, flagIn);
    }
}
