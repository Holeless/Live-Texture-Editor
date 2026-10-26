package com.zeeesea.textureeditor.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decides whether a block can be edited with the simple cube editor. Blocks with special models or
 * renderers (non-cube shapes, block entities, fluids, plants, ...) are routed to the generic browser instead.
 *
 * <p>NOTE: the 1.21 Fabric build only shipped as a compiled jar with obfuscated class names, so this
 * blacklist was reconstructed from the equivalent vanilla block classes rather than copied verbatim.
 */
public class BlockFilter {
   private static final Class<?>[] NON_EDITABLE = {
      AirBlock.class,
      BedBlock.class,
      ChestBlock.class,
      EnderChestBlock.class,
      DoorBlock.class,
      TrapDoorBlock.class,
      StairBlock.class,
      SlabBlock.class,
      FenceBlock.class,
      FenceGateBlock.class,
      WallBlock.class,
      IronBarsBlock.class,
      TorchBlock.class,
      WallTorchBlock.class,
      RedstoneTorchBlock.class,
      RedstoneWallTorchBlock.class,
      SignBlock.class,
      WallSignBlock.class,
      CeilingHangingSignBlock.class,
      WallHangingSignBlock.class,
      AbstractBannerBlock.class,
      AbstractSkullBlock.class,
      ButtonBlock.class,
      BasePressurePlateBlock.class,
      WeightedPressurePlateBlock.class,
      LeverBlock.class,
      CarpetBlock.class,
      VineBlock.class,
      BushBlock.class,
      BaseRailBlock.class,
      RedStoneWireBlock.class,
      DiodeBlock.class,
      HopperBlock.class,
      AnvilBlock.class,
      AbstractCauldronBlock.class,
      BrewingStandBlock.class,
      EnchantingTableBlock.class,
      LanternBlock.class,
      CandleBlock.class,
      ChainBlock.class,
      LadderBlock.class,
      PistonBaseBlock.class,
      PistonHeadBlock.class,
      MovingPistonBlock.class,
      CakeBlock.class,
      CandleCakeBlock.class,
      LiquidBlock.class,
      WaterlilyBlock.class,
      SnowLayerBlock.class,
      CampfireBlock.class,
      BellBlock.class,
      ConduitBlock.class,
      ScaffoldingBlock.class,
      CactusBlock.class,
      CocoaBlock.class,
      FlowerPotBlock.class,
      EndRodBlock.class,
      DragonEggBlock.class,
      LightningRodBlock.class,
      PointedDripstoneBlock.class,
      AmethystClusterBlock.class,
      SculkSensorBlock.class,
      SculkShriekerBlock.class,
      SculkVeinBlock.class,
      GlowLichenBlock.class,
      CaveVinesBlock.class,
      BambooStalkBlock.class,
      BigDripleafBlock.class,
      SmallDripleafBlock.class,
      SeaPickleBlock.class,
      TurtleEggBlock.class,
      FrogspawnBlock.class,
      EndPortalFrameBlock.class,
      GrindstoneBlock.class,
      LecternBlock.class,
      StonecutterBlock.class,
      ComposterBlock.class,
      DaylightDetectorBlock.class,
      TripWireBlock.class,
      TripWireHookBlock.class,
      CommandBlock.class,
      StructureVoidBlock.class,
      LightBlock.class,
      BarrierBlock.class,
      EndGatewayBlock.class,
      EndPortalBlock.class,
      NetherPortalBlock.class,
      ShulkerBoxBlock.class,
   };

   public static boolean isEditableBlock(BlockState state) {
      Block block = state.getBlock();
      for (Class<?> type : NON_EDITABLE) {
         if (type.isInstance(block)) {
            return false;
         }
      }

      return true;
   }

   public static boolean isEditableBlockId(ResourceLocation blockId) {
      Block block = BuiltInRegistries.BLOCK.get(blockId);
      return isEditableBlock(block.defaultBlockState());
   }
}
