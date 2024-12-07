package com.faboslav.structurify.common.config.client.gui;

import com.faboslav.structurify.common.Structurify;
import com.faboslav.structurify.common.config.StructurifyConfig;
import com.faboslav.structurify.common.config.client.api.controller.builder.StructureButtonControllerBuilder;
import com.faboslav.structurify.common.config.data.StructureData;
import com.faboslav.structurify.common.config.data.WorldgenDataProvider;
import com.faboslav.structurify.common.events.common.LoadConfigEvent;
import com.faboslav.structurify.common.util.LanguageUtil;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class StructuresConfigScreen
{
	public static Screen createConfigGui(StructurifyConfig config, Screen parent) {
		LoadConfigEvent.EVENT.invoke(new LoadConfigEvent());

		var yacl = YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("gui.structurify.title"))
			.save(config::save);

		createStructuresTab(yacl, config);

		return yacl.build().generateScreen(parent);
	}

	public static void createStructuresTab(YetAnotherConfigLib.Builder yacl, StructurifyConfig config) {
		List<Option<Boolean>> structureOptions = new ArrayList<>();

		var structureCategoryBuilder = ConfigCategory.createBuilder()
			.name(Component.translatable("gui.structurify.structures_category.title"))
			.tooltip(Component.translatable("gui.structurify.structures_category.description"));

		var generalStructuresGroupBuilder = OptionGroup.createBuilder()
			.name(Component.translatable("gui.structurify.structures.global.title"))
			.description(OptionDescription.of(Component.translatable("gui.structurify.structures.global.description")));

		var disableAllStructuresOption = Option.<Boolean>createBuilder()
			.name(Component.translatable("gui.structurify.structures.disable_all_structures.title"))
			.description(OptionDescription.of(Component.translatable("gui.structurify.structures.disable_all_structures.description")))
			.binding(
				false,
				() -> config.disableAllStructures,
				disableAllStructures -> config.disableAllStructures = disableAllStructures
			)
			.controller(opt -> BooleanControllerBuilder.create(opt).valueFormatter(val -> val ? Component.translatable("gui.structurify.label.yes").withStyle(style -> style.withColor(ChatFormatting.RED)):Component.translatable("gui.structurify.label.no").withStyle(style -> style.withColor(ChatFormatting.GREEN)))).build();

		disableAllStructuresOption.addListener((opt, disableAllStructures) -> {
			for(var structureOption : structureOptions) {
				structureOption.setAvailable(!disableAllStructures);
			}
		});

		generalStructuresGroupBuilder.option(disableAllStructuresOption);

		var minStructureDistanceFromWorldOptionBuilder = Option.<Integer>createBuilder()
			.name(Component.translatable("gui.structurify.structures.min_structure_distance_from_world_center.title"))
			.description(OptionDescription.of(Component.translatable("gui.structurify.structures.min_structure_distance_from_world_center.description")))
			.binding(
				0,
				() -> config.minStructureDistanceFromWorldCenter,
				minStructureDistanceFromWorldCenter -> config.minStructureDistanceFromWorldCenter = minStructureDistanceFromWorldCenter
			)
			.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 1024).step(1));

		generalStructuresGroupBuilder.option(minStructureDistanceFromWorldOptionBuilder.build());

		structureCategoryBuilder.group(generalStructuresGroupBuilder.build());

		var structures = WorldgenDataProvider.getStructures();
		List<OptionGroup> optionGroups = new ArrayList<>();
		OptionGroup.Builder currentGroupBuilder = null;
		String currentNamespace = null;

		for (Map.Entry<String, StructureData> entry : structures.entrySet()) {
			String structureStringId = entry.getKey();
			StructureData structureData = entry.getValue();

			ResourceLocation structureId = Structurify.makeNamespacedId(structureStringId);
			String namespace = structureId.getNamespace();

			// Create new group for each namespace
			if (!namespace.equals(currentNamespace)) {
				if (currentGroupBuilder != null) {
					optionGroups.add(currentGroupBuilder.build());
				}

				// Create new group
				currentGroupBuilder = OptionGroup.createBuilder()
					.name(Component.translatable("gui.structurify.structures.structures_group.title", LanguageUtil.translateId(null, namespace).getString()))
					.description(OptionDescription.of(Component.translatable("gui.structurify.structures.structures_group.description", namespace)));

				currentNamespace = namespace;
			}

			var structureOptionBuilder = Option.<Boolean>createBuilder()
				.name(LanguageUtil.translateId("structure", structureStringId))
				.binding(
					true,
					() -> !config.getStructureData().get(structureStringId).isDisabled(),
					isEnabled -> config.getStructureData().get(structureStringId).setDisabled(!isEnabled)
				).available(!config.disableAllStructures)
				.controller(opt -> StructureButtonControllerBuilder.create(opt, structureStringId)
					.valueFormatter(val -> val ? Component.translatable("gui.structurify.label.enabled"):Component.translatable("gui.structurify.label.disabled"))
					.coloured(true));

			var descriptionBuilder = OptionDescription.createBuilder();

			descriptionBuilder.text(Component.translatable("gui.structurify.structures.biomes_description").append(Component.literal("\n")));

			for (String biome : structureData.getBiomes()) {
				descriptionBuilder.text(Component.literal(" - ").append(LanguageUtil.translateId("biome", biome)));
			}

			descriptionBuilder.text(Component.literal("\n\n").append(Component.translatable("gui.structurify.structures.warning")).withStyle(style -> style.withColor(ChatFormatting.YELLOW)));

			structureOptionBuilder.description(descriptionBuilder.build());
			var structureOption = structureOptionBuilder.build();
			structureOptions.add(structureOption);
			currentGroupBuilder.option(structureOption);
		}

		if (currentGroupBuilder != null) {
			optionGroups.add(currentGroupBuilder.build());
		}

		for (OptionGroup structureOptionGroup : optionGroups) {
			structureCategoryBuilder.group(structureOptionGroup);
		}

		yacl.category(structureCategoryBuilder.build());
	}
}