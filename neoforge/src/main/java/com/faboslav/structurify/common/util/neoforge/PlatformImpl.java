package com.faboslav.structurify.common.util.neoforge;

import com.faboslav.structurify.common.Structurify;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class PlatformImpl
{
	public static boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Nullable
	public static String getModVersion() {
		return ModList.get().getModContainerById(Structurify.MOD_ID).map(modContainer -> modContainer.getModInfo().getVersion().toString()).orElse(null);
	}

	private PlatformImpl() {
	}
}
