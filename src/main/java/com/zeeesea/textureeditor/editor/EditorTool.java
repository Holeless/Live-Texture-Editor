package com.zeeesea.textureeditor.editor;



public enum EditorTool {
   PENCIL("Pencil", "✏"),
   BRUSH("Brush", "\ud83d\udd8c"),
   ERASER("Eraser", "⌫"),
   FILL("Fill", "\ud83e\udea3"),
   EYEDROPPER("Eyedropper", "\ud83d\udc89"),
   LINE("Line", "╱");

   private final String name;
   private final String icon;

   EditorTool(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.name;
   }

   public String getIcon() {
      return this.icon;
   }

   public static EditorTool getToolByName(String name) {
      for (EditorTool tool : values()) {
         if (tool.name.equals(name)) {
            return tool;
         }
      }

      return PENCIL;
   }
}
