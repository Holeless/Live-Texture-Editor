package com.zeeesea.textureeditor.settings;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.lwjgl.glfw.GLFW;

public class ModSettings {
   private static ModSettings instance;
   public int defaultZoom = 12;
   public boolean gridOnByDefault = true;
   public String defaultTool = "PENCIL";
   public Map<String, Integer> keybinds = new HashMap();
   public boolean showToolHints = true;
   public boolean autoApplyLive = true;
   public boolean confirmResetAll = true;
   public int maxUndoSteps = 50;
   public int colorHistorySize = 20;
   public float brushVariation = 0.15F;
   public boolean useExternalEditor = false;
   public String selectedEditorName = "";
   public String externalEditorCustomPath = "";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private ModSettings() {
      this.keybinds.put("pencil", 66);
      this.keybinds.put("eraser", 69);
      this.keybinds.put("fill", 70);
      this.keybinds.put("eyedropper", 73);
      this.keybinds.put("line", 76);
      this.keybinds.put("brush", 86);
      this.keybinds.put("undo", 89);
      this.keybinds.put("redo", 85);
      this.keybinds.put("grid", 71);
      this.keybinds.put("browse", 341);
   }

   public static ModSettings getInstance() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   public int getKeybind(String action) {
      return (Integer)this.keybinds.getOrDefault(action, -1);
   }

   public void setKeybind(String action, int keyCode) {
      this.keybinds.put(action, keyCode);
      this.save();
   }

   public String getKeyName(String action) {
      int code = this.getKeybind(action);
      if (code <= 0) {
         return "?";
      }

      String name = GLFW.glfwGetKeyName(code, 0);
      if (name != null) {
         return name.toUpperCase();
      }

      return switch (code) {
         case 32 -> "SPACE";
         case 258 -> "TAB";
         case 340 -> "L-SHIFT";
         case 341 -> "L-CTRL";
         case 342 -> "L-ALT";
         case 344 -> "R-SHIFT";
         case 345 -> "R-CTRL";
         default -> "KEY" + code;
      };
   }

   private static File getConfigFile() {
      File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
      if (!configDir.exists()) {
         configDir.mkdirs();
      }

      return new File(configDir, "textureeditor.json");
   }

   public void save() {
      try {
         Writer writer = new OutputStreamWriter(new FileOutputStream(getConfigFile()), StandardCharsets.UTF_8);

         try {
            GSON.toJson(this, writer);
         } catch (Throwable var5) {
            try {
               writer.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         writer.close();
      } catch (IOException e) {
         System.out.println("[TextureEditor] Failed to save settings: " + e.getMessage());
      }
   }

   private static ModSettings load() {
      File file = getConfigFile();
      if (file.exists()) {
         try {
            label50: {
               Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);

               ModSettings var10;
               label44: {
                  try {
                     ModSettings settings = (ModSettings)GSON.fromJson(reader, ModSettings.class);
                     if (settings != null) {
                        ModSettings defaults = new ModSettings();

                        for (Entry<String, Integer> entry : defaults.keybinds.entrySet()) {
                           settings.keybinds.putIfAbsent((String)entry.getKey(), (Integer)entry.getValue());
                        }

                        var10 = settings;
                        break label44;
                     }
                  } catch (Throwable var7) {
                     try {
                        reader.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }

                     throw var7;
                  }

                  reader.close();
                  break label50;
               }

               reader.close();
               return var10;
            }
         } catch (Exception e) {
            System.out.println("[TextureEditor] Failed to load settings, using defaults: " + e.getMessage());
         }
      }

      ModSettings settings = new ModSettings();
      settings.save();
      return settings;
   }
}
