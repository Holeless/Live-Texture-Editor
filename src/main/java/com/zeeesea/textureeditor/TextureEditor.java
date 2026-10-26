package com.zeeesea.textureeditor;

import com.zeeesea.textureeditor.screen.SettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge entry point. The mod is purely client side, so it only loads on {@link Dist#CLIENT}.
 */
@Mod(value = TextureEditor.MOD_ID, dist = Dist.CLIENT)
public class TextureEditor {
   public static final String MOD_ID = "textureeditor";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public TextureEditor(IEventBus modBus, ModContainer container) {
      modBus.addListener(TextureEditorClient::registerKeyMappings);
      modBus.addListener(TextureEditorClient::registerReloadListeners);
      NeoForge.EVENT_BUS.addListener(TextureEditorClient::onClientTick);
      // Replaces the Fabric ModMenu integration: shows up as the "Config" button in the mod list.
      container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new SettingsScreen(parent));
      LOGGER.info("Texture Editor loaded");
   }
}
