package com.zeeesea.textureeditor.editor;


import java.util.ArrayList;
import java.util.List;

public class ColorHistory {
   private static final ColorHistory INSTANCE = new ColorHistory();
   private static int MAX_HISTORY = 20;
   private final List<Integer> history = new ArrayList();

   private ColorHistory() {
   }

   public static ColorHistory getInstance() {
      return INSTANCE;
   }

   public void addColor(int color) {
      // Box it: List.remove(int) removes by INDEX, which threw IndexOutOfBounds for every ARGB color
      // (the crash when clicking a pixel with pencil/brush/fill/line).
      this.history.remove(Integer.valueOf(color));
      this.history.add(0, color);

      while (this.history.size() > MAX_HISTORY) {
         this.history.remove(this.history.size() - 1);
      }
   }

   public List<Integer> getColors() {
      return this.history;
   }

   public int size() {
      return this.history.size();
   }

   public static void setMaxHistory(int maxHistory) {
      MAX_HISTORY = maxHistory;
   }
}
