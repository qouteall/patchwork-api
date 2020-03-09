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

package com.patchworkmc.mixin.tooltypes;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import net.minecraftforge.common.ToolType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.patchworkmc.impl.tooltypes.ToolTypeRetriver;

@Mixin(Item.class)
public class MixinItem {
	private Map<ToolType, Integer> toolClasses = Maps.newHashMap();

	@Inject(method = "<init>", at = @At("RETURN"))
	public void hookConstructor(Item.Settings settings, CallbackInfo ci) {
		toolClasses = ((ToolTypeRetriver) settings).getToolTypeMap();
	}

	public Set<ToolType> getToolTypes(ItemStack stack) {
		return toolClasses.keySet();
	}

	public int getHarvestLevel(ItemStack stack, ToolType tool, PlayerEntity player, BlockState blockState) {
		return toolClasses.getOrDefault(tool, -1);
	}

	@Mixin(Item.Settings.class)
	static class MixinItemSettings implements ToolTypeRetriver {
		private Map<ToolType, Integer> toolClasses = Maps.newHashMap();

		public Item.Settings addToolType(ToolType type, int level) {
			toolClasses.put(type, level);
			return (Item.Settings) (Object) this;
		}

		@Override
		public Map<ToolType, Integer> getToolTypeMap() {
			return toolClasses;
		}
	}
}
