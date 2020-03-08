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

import com.google.common.collect.Maps;
import net.minecraftforge.common.ToolType;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.item.Item;

@Mixin(Item.class)
public class MixinItem {
	@Mixin(Item.Settings.class)
	static class MixinItemSettings {
		private java.util.Map<ToolType, Integer> toolClasses = Maps.newHashMap();

		public Item.Settings addToolType(ToolType type, int level) {
			toolClasses.put(type, level);
			return (Item.Settings) (Object) this;
		}
	}
}
