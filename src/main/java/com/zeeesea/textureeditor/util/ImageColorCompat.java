package com.zeeesea.textureeditor.util;

import com.mojang.blaze3d.platform.NativeImage;


public final class ImageColorCompat {
   private ImageColorCompat() {
   }

   public static int readArgb(NativeImage image, int x, int y) {
      return swapRedBlue(image.getPixelRGBA(x, y));
   }

   public static void writeArgb(NativeImage image, int x, int y, int argb) {
      image.setPixelRGBA(x, y, swapRedBlue(argb));
   }

   private static int swapRedBlue(int color) {
      return color & -16711936 | (color & 0xFF0000) >>> 16 | (color & 0xFF) << 16;
   }
}
