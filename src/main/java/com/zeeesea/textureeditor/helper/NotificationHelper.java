package com.zeeesea.textureeditor.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
   private static long TOAST_COOLDOWN_MS = 5000L;
   private static final Map<String, Long> lastToast = new HashMap();

   public static boolean addToast(SystemToast.SystemToastId type, String title, String description) {
      long now = System.currentTimeMillis();
      Long last = (Long)lastToast.get(title);
      if (last != null && now - last <= TOAST_COOLDOWN_MS) {
         return false;
      }

      lastToast.put(title, now);
      showToast(type, title, description);
      return true;
   }

   public static boolean addToast(SystemToast.SystemToastId type, String title) {
      return addToast(type, title, "");
   }

   private static void showToast(SystemToast.SystemToastId type, String title, String description) {
      Minecraft client = Minecraft.getInstance();
      client.getToasts().addToast(SystemToast.multiline(client, type, Component.literal(title), Component.literal(description)));
   }

   public static long getToastCooldown() {
      return TOAST_COOLDOWN_MS;
   }

   public static void setToastCooldown(long cooldown) {
      TOAST_COOLDOWN_MS = cooldown;
   }
}
