package com.zeeesea.textureeditor.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;


public class EntityMapper {
   public static Entity getEntityFromItem(ItemStack stack, Level world) {
      Item item = stack.getItem();
      if (item instanceof SpawnEggItem) {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
         String path = id.getPath();
         if (path.endsWith("_spawn_egg")) {
            String entityName = path.substring(0, path.length() - "_spawn_egg".length());
            ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), entityName);
            EntityType<?> type = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(entityId);
            if (type != EntityType.PIG || entityName.equals("pig")) {
               return type.create(world);
            }
         }
      }

      if (item instanceof BoatItem) {
         ResourceLocation boatId = BuiltInRegistries.ITEM.getKey(item).getPath().contains("chest_boat")
            ? ResourceLocation.withDefaultNamespace("chest_boat")
            : ResourceLocation.withDefaultNamespace("boat");
         return ((EntityType)BuiltInRegistries.ENTITY_TYPE.get(boatId)).create(world);
      } else if (item instanceof MinecartItem) {
         return ((EntityType)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace("minecart"))).create(world);
      } else {
         return item instanceof ArmorStandItem
            ? ((EntityType)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace("armor_stand"))).create(world)
            : null;
      }
   }

   public static boolean hasEntityMode(ItemStack stack) {
      return getEntityFromItem(stack, Minecraft.getInstance().level) != null;
   }

   public static ItemStack getItemFromEntity(Entity entity) {
      if (entity instanceof Boat) {
         return new ItemStack(Items.OAK_BOAT);
      }

      if (entity instanceof ChestBoat) {
         return new ItemStack(Items.OAK_CHEST_BOAT);
      }

      if (entity instanceof AbstractMinecart) {
         return new ItemStack(Items.MINECART);
      }

      if (entity instanceof ArmorStand) {
         return new ItemStack(Items.ARMOR_STAND);
      }

      EntityType<?> type = entity.getType();
      ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
      ResourceLocation eggId = ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), entityId.getPath() + "_spawn_egg");
      Item eggItem = (Item)BuiltInRegistries.ITEM.get(eggId);
      return eggItem != Items.AIR ? new ItemStack(eggItem) : null;
   }

   public static boolean hasItemMode(Entity entity) {
      return getItemFromEntity(entity) != null;
   }
}
