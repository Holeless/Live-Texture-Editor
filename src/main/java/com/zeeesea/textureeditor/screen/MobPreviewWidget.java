package com.zeeesea.textureeditor.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Quaternionf;

public class MobPreviewWidget {
   private boolean visible = false;
   private int x;
   private int y;
   private int width;
   private int height;
   private float rotationYaw = 30.0F;
   private float rotationPitch = -15.0F;
   private boolean isDragging = false;
   private double dragStartX;
   private double dragStartY;
   private float dragStartYaw;
   private float dragStartPitch;
   private final Entity entity;
   private float scale = 1.0F;
   private static final float MIN_SCALE = 0.5F;
   private static final float MAX_SCALE = 2.5F;

   public MobPreviewWidget(Entity entity) {
      this.entity = entity;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public void toggleVisible() {
      this.visible = !this.visible;
   }

   public void setPosition(int x, int y, int width, int height) {
      this.x = x;
      this.y = y + 30;
      this.width = width;
      this.height = height;
   }

   public void render(GuiGraphics context, int mouseX, int mouseY) {
      if (this.visible && this.entity != null) {
         context.fill(this.x - 2, this.y - 16, this.x + this.width + 2, this.y + this.height + 2, -299752892);
         context.fill(this.x - 2, this.y - 16, this.x + this.width + 2, this.y - 15, -1);
         context.fill(this.x - 2, this.y + this.height + 1, this.x + this.width + 2, this.y + this.height + 2, -1);
         context.fill(this.x - 2, this.y - 16, this.x - 1, this.y + this.height + 2, -1);
         context.fill(this.x + this.width + 1, this.y - 16, this.x + this.width + 2, this.y + this.height + 2, -1);
         Minecraft client = Minecraft.getInstance();
         context.drawString(client.font, "§b3D Preview", this.x + 2, this.y - 13, 16777215, false);
         if (this.entity instanceof LivingEntity livingEntity) {
            int centerX = this.x + this.width / 2;
            int centerY = this.y + this.height - 10;
            float entityHeight = livingEntity.getBbHeight();
            float entityWidth = livingEntity.getBbWidth();
            float maxDim = Math.max(entityHeight, entityWidth);
            int entitySize = (int)(Math.min(this.width, this.height) * 0.35F * this.scale / maxDim);
            entitySize = Math.max(10, Math.min(entitySize, (int)(80.0F * this.scale)));
            Quaternionf rotQuat = new Quaternionf().rotateY((float)Math.toRadians(-this.rotationYaw)).rotateX((float)Math.toRadians(this.rotationPitch));
            Quaternionf modelQuat = new Quaternionf().rotateX((float)Math.toRadians(this.rotationPitch));

            try {
               InventoryScreen.renderEntityInInventoryFollowsMouse(context, this.x, this.y, this.x + this.width, this.y + this.height, entitySize, 0.0625F, mouseX, mouseY, livingEntity);
            } catch (Exception e) {
               context.drawString(client.font, "Preview N/A", this.x + 10, this.y + this.height / 2, 16733525, false);
            }
         } else {
            Minecraft client2 = Minecraft.getInstance();
            context.drawString(client2.font, "Non-living entity", this.x + 5, this.y + this.height / 2, 11184810, false);
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.visible) {
         return false;
      }

      if (mouseX >= this.x - 2 && mouseX <= this.x + this.width + 2 && mouseY >= this.y - 16 && mouseY <= this.y + this.height + 2) {
         if (button == 0) {
            this.isDragging = true;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
            this.dragStartYaw = this.rotationYaw;
            this.dragStartPitch = this.rotationPitch;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isDragging) {
         this.isDragging = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.visible && this.isDragging) {
         this.rotationYaw = this.dragStartYaw + (float)(mouseX - this.dragStartX) * 0.8F;
         this.rotationPitch = this.dragStartPitch + (float)(mouseY - this.dragStartY) * 0.5F;
         this.rotationPitch = Math.max(-90.0F, Math.min(90.0F, this.rotationPitch));
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double ha, double va) {
      if (!this.visible) {
         return false;
      }

      if (mouseX >= this.x - 2 && mouseX <= this.x + this.width + 2 && mouseY >= this.y - 16 && mouseY <= this.y + this.height + 2) {
         if (va > 0.0) {
            this.scale = Math.min(2.5F, this.scale + 0.1F);
         } else if (va < 0.0) {
            this.scale = Math.max(0.5F, this.scale - 0.1F);
         }

         return true;
      } else {
         return false;
      }
   }
}
