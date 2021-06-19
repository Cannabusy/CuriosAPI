/*
 * Copyright (c) 2018-2020 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.api.type.capability;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.ISlotType;

public interface ICurio {

  /**
   * Gets the ItemStack associated with the instance
   *
   * @return The ItemStack associated with the instance
   */
  ItemStack getStack();

  /**
   * Called every tick on both client and server while the ItemStack is equipped.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   */
  default void curioTick(SlotContext slotContext) {
    curioTick(slotContext.getIdentifier(), slotContext.getIndex(), slotContext.getWearer());
  }

  /**
   * Called when the ItemStack is equipped into a slot or its data changes.
   *
   * @param slotContext Context about the slot that the ItemStack was just unequipped from
   * @param prevStack   The previous ItemStack in the slot
   */
  default void onEquip(SlotContext slotContext, ItemStack prevStack) {
    onEquip(slotContext.getIdentifier(), slotContext.getIndex(), slotContext.getWearer());
  }

  /**
   * Called when the ItemStack is unequipped from a slot or its data changes.
   *
   * @param slotContext Context about the slot that the ItemStack was just unequipped from
   * @param newStack    The new ItemStack in the slot
   */
  default void onUnequip(SlotContext slotContext, ItemStack newStack) {
    onUnequip(slotContext.getIdentifier(), slotContext.getIndex(), slotContext.getWearer());
  }

  /**
   * Determines if the ItemStack can be equipped into a slot.
   *
   * @param slotContext Context about the slot that the ItemStack is attempting to equip into
   * @return True if the ItemStack can be equipped/put in, false if not
   */
  default boolean canEquip(SlotContext slotContext) {
    return true;
  }

  /**
   * Determines if the ItemStack can be unequipped from a slot.
   *
   * @param slotContext Context about the slot that the ItemStack is attempting to unequip from
   * @return True if the ItemStack can be unequipped/taken out, false if not
   */
  default boolean canUnequip(SlotContext slotContext) {
    return true;
  }

  /**
   * Retrieves a list of tooltips when displaying curio slot information. By default, this will be a
   * list of each slot identifier, translated and in gold text, associated with the curio.
   * <br>
   * If overriding, make sure the user has some indication which slots are associated with the
   * curio.
   *
   * @param tooltips A list of {@link ITextComponent} with every slot valid for this curio
   * @return A list of ITextComponent to display as curio slot information
   */
  default List<ITextComponent> getSlotsTooltip(List<ITextComponent> tooltips) {
    return getTagsTooltip(tooltips);
  }

  /**
   * Retrieves a map of attribute modifiers for the curio.
   * <br>
   * Note that only the identifier is guaranteed to be present in the slot context. For instances
   * where the ItemStack may not be in a curio slot, such as when retrieving item tooltips, the
   * index is -1 and the wearer may be null.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @param uuid        Slot-unique UUID
   * @return A map of attribute modifiers to apply
   */
  default Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                       UUID uuid) {
    return getAttributeModifiers(slotContext.getIdentifier());
  }

  /**
   * Called server-side when the ItemStack is equipped by using it (i.e. from the hotbar), after
   * calling {@link ICurio#canEquipFromUse(SlotContext)}.
   * <br>
   * Default implementation plays the equip sound from {@link ICurio#getEquipSound(SlotContext)}.
   * This can be overridden to avoid that, but it is advised to always play something as an auditory
   * feedback for players.
   *
   * @param slotContext Context about the slot that the ItemStack was just equipped into
   */
  default void onEquipFromUse(SlotContext slotContext) {
    playRightClickEquipSound(slotContext.getWearer());
  }

  /**
   * Retrieves the equip sound information for the given slot context.
   *
   * @param slotContext Context about the slot that the ItemStack was just equipped into
   * @return {@link SoundInfo} containing information about the sound event, volume, and pitch
   */
  @Nonnull
  default SoundInfo getEquipSound(SlotContext slotContext) {
    return new SoundInfo(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
  }

  /**
   * Determines if the ItemStack can be automatically equipped into the first available slot when
   * used.
   *
   * @param slotContext Context about the slot that the ItemStack is attempting to equip into
   * @return True to enable auto-equipping when the item is used, false to disable
   */
  default boolean canEquipFromUse(SlotContext slotContext) {
    return canRightClickEquip();
  }

  /**
   * Called when rendering break animations and sounds client-side when a worn curio item is
   * broken.
   *
   * @param slotContext Context about the slot that the ItemStack broke in
   */
  default void curioBreak(SlotContext slotContext) {
    curioBreak(getStack(), slotContext.getWearer());
  }

  /**
   * Compares the current ItemStack and the previous ItemStack in the slot to detect any changes and
   * returns true if the change should be synced to all tracking clients. Note that this check
   * occurs every tick so implementations need to code their own timers for other intervals.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @return True to sync the ItemStack change to all tracking clients, false to do nothing
   */
  default boolean canSync(SlotContext slotContext) {
    return canSync(slotContext.getIdentifier(), slotContext.getIndex(), slotContext.getWearer());
  }

  /**
   * Gets a tag that is used to sync extra curio data from the server to the client. Only used when
   * {@link ICurio#canSync(String, int, LivingEntity)} returns true.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @return Data to be sent to the client
   */
  @Nonnull
  default CompoundNBT writeSyncData(SlotContext slotContext) {
    return writeSyncData();
  }

  /**
   * Used client-side to read data tags created by {@link ICurio#writeSyncData()} received from the
   * server.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @param compound    Data received from the server
   */
  default void readSyncData(SlotContext slotContext, CompoundNBT compound) {
    readSyncData(compound);
  }

  /**
   * Determines if the ItemStack should drop on death and persist through respawn. This will persist
   * the ItemStack in the curio slot to the respawned player if applicable.
   *
   * @param slotContext  Context about the slot that the ItemStack is attempting to equip into
   * @param source       The damage source that killed the wearer and triggered the drop
   * @param lootingLevel The level of looting that triggered the drop
   * @param recentlyHit  Whether or not the wearer was recently hit
   * @return The {@link DropRule} that applies to this curio
   */
  @Nonnull
  default DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel,
                               boolean recentlyHit) {
    return getDropRule(slotContext.getWearer());
  }

  /**
   * Retrieves a list of tooltips when displaying curio attribute modifier information returned by
   * {@link ICurio#getAttributeModifiers(SlotContext, UUID)}. By default, this will display a list
   * similar to the vanilla attribute modifier tooltips.
   *
   * @param tooltips A list of {@link ITextComponent} with the attribute modifier information
   * @return A list of ITextComponent to display as curio attribute modifier information
   */
  default List<ITextComponent> getAttributesTooltip(List<ITextComponent> tooltips) {
    return showAttributesTooltip("") ? tooltips : new ArrayList<>();
  }

  /**
   * Get the amount of bonus Fortune levels that are provided by curio.
   * Default implementation returns level of Fortune enchantment on ItemStack.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @return Amount of additional Fortune levels that will be applied when mining
   */
  default int getFortuneLevel(SlotContext slotContext) {
    return getFortuneBonus(slotContext.getIdentifier(), slotContext.getWearer(), getStack(),
        slotContext.getIndex());
  }

  /**
   * Get the amount of bonus Looting levels that are provided by curio.
   * Default implementation returns level of Looting enchantment on ItemStack.
   *
   * @param slotContext Context about the slot that the ItemStack is in
   * @return Amount of additional Looting levels that will be applied in LootingLevelEvent
   */
  default int getLootingLevel(SlotContext slotContext) {
    return getLootingBonus(slotContext.getIdentifier(), slotContext.getWearer(), getStack(),
        slotContext.getIndex());
  }

  /**
   * Used by {@link ICurio#getDropRule(LivingEntity)} to determine drop on death behavior.
   * <br>
   * DEFAULT - normal vanilla behavior with drops dictated by the Keep Inventory game rule
   * <br>
   * ALWAYS_DROP - always drop regardless of game rules
   * <br>
   * ALWAYS_KEEP - always keep regardless of game rules
   * <br>
   * DESTROY - destroy the item upon death
   */
  enum DropRule {
    DEFAULT, ALWAYS_DROP, ALWAYS_KEEP, DESTROY
  }

  final class SoundInfo {
    final SoundEvent soundEvent;
    final float volume;
    final float pitch;

    public SoundInfo(SoundEvent soundEvent, float volume, float pitch) {
      this.soundEvent = soundEvent;
      this.volume = volume;
      this.pitch = pitch;
    }

    public SoundEvent getSoundEvent() {
      return soundEvent;
    }

    public float getVolume() {
      return volume;
    }

    public float getPitch() {
      return pitch;
    }
  }

  /*
   * Copy of vanilla implementation for breaking items client-side
   */
  static void playBreakAnimation(ItemStack stack, LivingEntity livingEntity) {

    if (!stack.isEmpty()) {

      if (!livingEntity.isSilent()) {
        livingEntity.world
            .playSound(livingEntity.getPosX(), livingEntity.getPosY(), livingEntity.getPosZ(),
                SoundEvents.ENTITY_ITEM_BREAK, livingEntity.getSoundCategory(), 0.8F,
                0.8F + livingEntity.world.rand.nextFloat() * 0.4F, false);
      }

      for (int i = 0; i < 5; ++i) {
        Vector3d vec3d = new Vector3d((livingEntity.getRNG().nextFloat() - 0.5D) * 0.1D,
            Math.random() * 0.1D + 0.1D, 0.0D);
        vec3d = vec3d.rotatePitch(-livingEntity.rotationPitch * ((float) Math.PI / 180F));
        vec3d = vec3d.rotateYaw(-livingEntity.rotationYaw * ((float) Math.PI / 180F));
        double d0 = (-livingEntity.getRNG().nextFloat()) * 0.6D - 0.3D;

        Vector3d vec3d1 = new Vector3d((livingEntity.getRNG().nextFloat() - 0.5D) * 0.3D,
            d0, 0.6D);
        vec3d1 = vec3d1.rotatePitch(-livingEntity.rotationPitch * ((float) Math.PI / 180F));
        vec3d1 = vec3d1.rotateYaw(-livingEntity.rotationYaw * ((float) Math.PI / 180F));
        vec3d1 = vec3d1.add(livingEntity.getPosX(),
            livingEntity.getPosY() + livingEntity.getEyeHeight(), livingEntity.getPosZ());

        livingEntity.world
            .addParticle(new ItemParticleData(ParticleTypes.ITEM, stack), vec3d1.x, vec3d1.y,
                vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z);
      }
    }
  }

  // ============ DEPRECATED ================

  /**
   * @deprecated See {@link ICurio#curioTick(SlotContext)}
   */
  @Deprecated
  default void curioTick(String identifier, int index, LivingEntity livingEntity) {

  }

  /**
   * @deprecated See {@link ICurio#curioTick(SlotContext)}
   */
  @Deprecated
  default void curioAnimate(String identifier, int index, LivingEntity livingEntity) {

  }

  /**
   * @deprecated See {@link ICurio#curioBreak(SlotContext)}
   */
  @Deprecated
  default void curioBreak(ItemStack stack, LivingEntity livingEntity) {
    playBreakAnimation(stack, livingEntity);
  }

  /**
   * @deprecated See {@link ICurio#onEquip(SlotContext, ItemStack)}
   */
  @Deprecated
  default void onEquip(String identifier, int index, LivingEntity livingEntity) {

  }

  /**
   * @deprecated See {@link ICurio#onUnequip(SlotContext, ItemStack)}
   */
  @Deprecated
  default void onUnequip(String identifier, int index, LivingEntity livingEntity) {

  }

  /**
   * @deprecated See {@link ICurio#canEquip(SlotContext)}
   */
  @Deprecated
  default boolean canEquip(String identifier, LivingEntity livingEntity) {
    return true;
  }

  /**
   * @deprecated See {@link ICurio#canUnequip(SlotContext)}
   */
  @Deprecated
  default boolean canUnequip(String identifier, LivingEntity livingEntity) {
    return true;
  }

  /**
   * @deprecated See {@link ICurio#getSlotsTooltip(List)}
   */
  @Deprecated
  default List<ITextComponent> getTagsTooltip(List<ITextComponent> tagTooltips) {
    return tagTooltips;
  }

  /**
   * @deprecated See {@link ICurio#getFortuneLevel(SlotContext)}
   */
  @Deprecated
  default int getFortuneBonus(String identifier, LivingEntity livingEntity, ItemStack curio,
                              int index) {
    return EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, curio);
  }

  /**
   * @deprecated See {@link ICurio#getLootingLevel(SlotContext)}
   */
  @Deprecated
  default int getLootingBonus(String identifier, LivingEntity livingEntity, ItemStack curio,
                              int index) {
    return EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, curio);
  }

  /**
   * @deprecated See {@link ICurio#getAttributesTooltip(List)}
   */
  @Deprecated
  default boolean showAttributesTooltip(String identifier) {
    return true;
  }

  /**
   * @deprecated See {@link ICurio#getDropRule(SlotContext, DamageSource, int, boolean)}
   */
  @Nonnull
  @Deprecated
  default DropRule getDropRule(LivingEntity livingEntity) {
    return DropRule.DEFAULT;
  }

  /**
   * @deprecated See {@link ICurio#canSync(SlotContext)}
   */
  @Deprecated
  default boolean canSync(String identifier, int index, LivingEntity livingEntity) {
    return false;
  }

  /**
   * @deprecated See {@link ICurio#writeSyncData(SlotContext)}
   */
  @Nonnull
  @Deprecated
  default CompoundNBT writeSyncData() {
    return new CompoundNBT();
  }

  /**
   * @deprecated See {@link ICurio#readSyncData(SlotContext, CompoundNBT)}
   */
  @Deprecated
  default void readSyncData(CompoundNBT compound) {

  }

  /**
   * @deprecated See {@link ICurio#canEquipFromUse(SlotContext)} for a more appropriately named
   * alternative with additional context.
   */
  @Deprecated
  default boolean canRightClickEquip() {
    return false;
  }

  /**
   * @deprecated See {@link ICurio#onEquipFromUse(SlotContext)} for a more appropriately
   * named alternative with additional context.
   * <br>
   * Also see {@link ICurio#getEquipSound(SlotContext)}.
   */
  @Deprecated
  default void playRightClickEquipSound(LivingEntity livingEntity) {
    // Not enough context for id and index so we just pass in artificial values with the entity
    SoundInfo soundInfo = getEquipSound(new SlotContext("", livingEntity));
    livingEntity.world.playSound(null, livingEntity.getPosition(), soundInfo.getSoundEvent(),
        livingEntity.getSoundCategory(), soundInfo.getVolume(), soundInfo.getPitch());
  }

  /**
   * @deprecated See {@link ICurio#getAttributeModifiers(SlotContext, UUID)} for an updated
   * alternative with additional context and a slot-unique UUID parameter.
   */
  @Deprecated
  default Multimap<Attribute, AttributeModifier> getAttributeModifiers(String identifier) {
    return HashMultimap.create();
  }
}
