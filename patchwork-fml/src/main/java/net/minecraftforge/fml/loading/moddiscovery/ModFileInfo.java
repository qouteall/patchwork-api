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

package net.minecraftforge.fml.loading.moddiscovery;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

public class ModFileInfo {
	private ModFile modFile;

	public ModFileInfo(String modid) {
		modFile = new ModFile(modid);
	}

	//create empty mod file info for Fabric mods
	public ModFileInfo() {
		modFile = new ModFile();
	}

	public ModFile getFile() {
		return modFile;
	}

	public static boolean isForgeMod(ModContainer modContainer) {
		CustomValue source = modContainer.getMetadata().getCustomValue("patchwork:source");

		if (source == null) {
			return false;
		}

		CustomValue.CvObject object = source.getAsObject();
		String loader = object.get("loader").getAsString();
		return loader.equals("forge");
	}
}
