/*
 * Minecraft Forge, Patchwork Project
 * Copyright (c) 2016-2020, 2019-2020
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.patchworkmc.mixin.gui.gameoverlay;

import static com.patchworkmc.impl.gui.PatchworkGui.post;
import static com.patchworkmc.impl.gui.PatchworkGui.pre;
import static net.minecraftforge.client.ForgeIngameGui.left_height;
import static net.minecraftforge.client.ForgeIngameGui.renderAir;
import static net.minecraftforge.client.ForgeIngameGui.renderArmor;
import static net.minecraftforge.client.ForgeIngameGui.renderFood;
import static net.minecraftforge.client.ForgeIngameGui.renderHealth;
import static net.minecraftforge.client.ForgeIngameGui.renderHealthMount;
import static net.minecraftforge.client.ForgeIngameGui.right_height;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.AIR;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ARMOR;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.FOOD;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HEALTH;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HEALTHMOUNT;

import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.math.MathHelper;

@Mixin(InGameHud.class)
public abstract class HookStatusBarEvents extends DrawableHelper {
	@Shadow
	protected abstract PlayerEntity getCameraPlayer();

	@Shadow
	private int ticks;

	@Shadow
	@Final
	private Random random;

	@Shadow
	private int scaledWidth;

	@Shadow
	private int scaledHeight;

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	protected abstract LivingEntity getRiddenEntity();

	@Shadow
	protected abstract int method_1744(LivingEntity livingEntity);

	@Shadow
	protected abstract int method_1733(int i);

	@Shadow
	private long field_2032;

	@Shadow
	private int field_2014;

	@Shadow
	private long field_2012;

	@Shadow
	private int field_2033;

	/**
	 * Forge changes the order of rendering armor and health, so an overwrite is needed.
	 * This
	 * @author TheGlitch76
	 */
	@Overwrite
	private void renderStatusBars() {
		if (renderHealth) {
			renderHealth(this.scaledWidth, this.scaledHeight);
		}

		if (renderArmor) {
			renderArmor(this.scaledWidth, this.scaledHeight);
		}

		if (renderFood) {
			renderFood(this.scaledWidth, this.scaledHeight);
		}

		if (renderHealthMount) {
			renderHealthMount(this.scaledWidth, this.scaledHeight);
		}

		if (renderAir) {
			renderAir(this.scaledWidth, this.scaledHeight);
		}
	}

	public void renderHealth(int width, int height) {
		bind(GUI_ICONS_LOCATION);

		if (pre(HEALTH)) {
			return;
		}

		client.getProfiler().push("health");
		GlStateManager.enableBlend();

		PlayerEntity player = (PlayerEntity) this.client.getCameraEntity();
		int health = MathHelper.ceil(player.getHealth());
		boolean highlight = field_2032 > (long) ticks && (field_2032 - (long) ticks) / 3L % 2L == 1L;

		if (health < this.field_2014 && player.timeUntilRegen > 0) {
			this.field_2012 = SystemUtil.getMeasuringTimeMs();
			this.field_2032 = this.ticks + 20;
		} else if (health > this.field_2014 && player.timeUntilRegen > 0) {
			this.field_2012 = SystemUtil.getMeasuringTimeMs();
			this.field_2032 = this.ticks + 10;
		}

		if (SystemUtil.getMeasuringTimeMs() - this.field_2012 > 1000L) {
			this.field_2014 = health;
			this.field_2033 = health;
			this.field_2012 = SystemUtil.getMeasuringTimeMs();
		}

		this.field_2014 = health;
		int healthLast = this.field_2033;

		EntityAttributeInstance attrMaxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		float healthMax = (float) attrMaxHealth.getValue();
		float absorb = MathHelper.ceil(player.getAbsorptionAmount());

		int healthRows = MathHelper.ceil((healthMax + absorb) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (healthRows - 2), 3);

		this.random.setSeed(ticks * 312871);

		int left = width / 2 - 91;
		int top = height - left_height;
		left_height += (healthRows * rowHeight);

		if (rowHeight != 10) {
			left_height += 10 - rowHeight;
		}

		int regen = -1;

		if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
			regen = ticks % 25;
		}

		final int TOP = 9 * (client.world.getLevelProperties().isHardcore() ? 5 : 0);
		final int BACKGROUND = (highlight ? 25 : 16);
		int MARGIN = 16;

		if (player.hasStatusEffect(StatusEffects.POISON)) {
			MARGIN += 36;
		} else if (player.hasStatusEffect(StatusEffects.WITHER)) {
			MARGIN += 72;
		}

		float absorbRemaining = absorb;

		for (int i = MathHelper.ceil((healthMax + absorb) / 2.0F) - 1; i >= 0; --i) {
			//int b0 = (highlight ? 1 : 0);
			int row = MathHelper.ceil((float) (i + 1) / 10.0F) - 1;
			int x = left + i % 10 * 8;
			int y = top - row * rowHeight;

			if (health <= 4) {
				y += random.nextInt(2);
			}

			if (i == regen) {
				y -= 2;
			}

			blit(x, y, BACKGROUND, TOP, 9, 9);

			if (highlight) {
				if (i * 2 + 1 < healthLast) {
					blit(x, y, MARGIN + 54, TOP, 9, 9); //6
				} else if (i * 2 + 1 == healthLast) {
					blit(x, y, MARGIN + 63, TOP, 9, 9); //7
				}
			}

			if (absorbRemaining > 0.0F) {
				if (absorbRemaining == absorb && absorb % 2.0F == 1.0F) {
					blit(x, y, MARGIN + 153, TOP, 9, 9); //17
					absorbRemaining -= 1.0F;
				} else {
					blit(x, y, MARGIN + 144, TOP, 9, 9); //16
					absorbRemaining -= 2.0F;
				}
			} else {
				if (i * 2 + 1 < health) {
					blit(x, y, MARGIN + 36, TOP, 9, 9); //4
				} else if (i * 2 + 1 == health) {
					blit(x, y, MARGIN + 45, TOP, 9, 9); //5
				}
			}
		}

		GlStateManager.disableBlend();
		client.getProfiler().pop();
		post(HEALTH);
	}

	protected void renderArmor(int width, int height) {
		if (pre(ARMOR)) {
			return;
		}

		client.getProfiler().push("armor");

		GlStateManager.enableBlend();
		int left = width / 2 - 91;
		int top = height - left_height;

		int level = client.player.getArmor();

		for (int i = 1; level > 0 && i < 20; i += 2) {
			if (i < level) {
				blit(left, top, 34, 9, 9, 9);
			} else if (i == level) {
				blit(left, top, 25, 9, 9, 9);
			} else if (i > level) {
				blit(left, top, 16, 9, 9, 9);
			}

			left += 8;
		}

		left_height += 10;

		GlStateManager.disableBlend();
		client.getProfiler().pop();
		post(ARMOR);
	}

	public void renderFood(int width, int height) {
		if (pre(FOOD)) {
			return;
		}

		client.getProfiler().push("food");

		PlayerEntity player = (PlayerEntity) this.client.getCameraEntity();
		GlStateManager.enableBlend();
		int left = width / 2 + 91;
		int top = height - right_height;
		right_height += 10;
		boolean unused = false; // Unused flag in vanilla, seems to be part of a 'fade out' mechanic

		HungerManager manager = client.player.getHungerManager();
		int level = manager.getFoodLevel();

		for (int i = 0; i < 10; ++i) {
			int idx = i * 2 + 1;
			int x = left - i * 8 - 9;
			int y = top;
			int icon = 16;
			byte background = 0;

			if (client.player.hasStatusEffect(StatusEffects.HUNGER)) {
				icon += 36;
				background = 13;
			}

			if (unused) {
				background = 1; //Probably should be a += 1 but vanilla never uses this
			}

			if (manager.getSaturationLevel() <= 0.0F && ticks % (level * 3 + 1) == 0) {
				y = top + (random.nextInt(3) - 1);
			}

			blit(x, y, 16 + background * 9, 27, 9, 9);

			if (idx < level) {
				blit(x, y, icon + 36, 27, 9, 9);
			} else if (idx == level) {
				blit(x, y, icon + 45, 27, 9, 9);
			}
		}

		GlStateManager.disableBlend();
		client.getProfiler().pop();
		post(FOOD);
	}

	protected void renderHealthMount(int width, int height) {
		PlayerEntity player = (PlayerEntity) client.getCameraEntity();
		Entity tmp = player.getVehicle();

		if (!(tmp instanceof LivingEntity)) {
			return;
		}

		bind(GUI_ICONS_LOCATION);

		if (pre(HEALTHMOUNT)) {
			return;
		}

		boolean unused = false;
		int left_align = width / 2 + 91;

		client.getProfiler().swap("mountHealth");
		GlStateManager.enableBlend();
		LivingEntity mount = (LivingEntity) tmp;
		int health = (int) Math.ceil(mount.getHealth());
		float healthMax = mount.getHealthMaximum();
		int hearts = (int) (healthMax + 0.5F) / 2;

		if (hearts > 30) {
			hearts = 30;
		}

		final int MARGIN = 52;
		final int BACKGROUND = MARGIN + (unused ? 1 : 0);
		final int HALF = MARGIN + 45;
		final int FULL = MARGIN + 36;

		for (int heart = 0; hearts > 0; heart += 20) {
			int top = height - right_height;

			int rowCount = Math.min(hearts, 10);
			hearts -= rowCount;

			for (int i = 0; i < rowCount; ++i) {
				int x = left_align - i * 8 - 9;
				blit(x, top, BACKGROUND, 9, 9, 9);

				if (i * 2 + 1 + heart < health) {
					blit(x, top, FULL, 9, 9, 9);
				} else if (i * 2 + 1 + heart == health) {
					blit(x, top, HALF, 9, 9, 9);
				}
			}

			right_height += 10;
		}

		GlStateManager.disableBlend();
		post(HEALTHMOUNT);
	}

	protected void renderAir(int width, int height) {
		if (pre(AIR)) {
			return;
		}

		client.getProfiler().push("air");
		PlayerEntity player = (PlayerEntity) this.client.getCameraEntity();
		GlStateManager.enableBlend();
		int left = width / 2 + 91;
		int top = height - right_height;

		int air = player.getBreath();

		if (player.isInFluid(FluidTags.WATER) || air < 300) {
			int full = MathHelper.ceil((double) (air - 2) * 10.0D / 300.0D);
			int partial = MathHelper.ceil((double) air * 10.0D / 300.0D) - full;

			for (int i = 0; i < full + partial; ++i) {
				blit(left - i * 8 - 9, top, (i < full ? 16 : 25), 18, 9, 9);
			}

			right_height += 10;
		}

		GlStateManager.disableBlend();
		client.getProfiler().pop();
		post(AIR);
	}

	private void bind(Identifier identifier) {
		client.getTextureManager().bindTexture(identifier);
	}
}

