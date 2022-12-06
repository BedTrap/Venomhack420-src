package venomhack.modules.combat;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.math.Direction.class_2353;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import venomhack.Venomhack420;
import venomhack.enums.Origin;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.DamageCalcUtils;

public class PistonAura extends ModuleHelper {
   private final SettingGroup sgToggles = this.settings.createGroup("Toggles");
   private final SettingGroup sgSwitch = this.settings.createGroup("Switch");
   private final Setting<PistonAura.DamageCalc> damageCalc = this.setting(
      "damage-calc-position", "Where to check for crystal damage", PistonAura.DamageCalc.PLACEPOS
   );
   private final Setting<Integer> targetRange = this.setting("target-range", "Target range", Integer.valueOf(3), 1.0, 6.0);
   private final Setting<Double> minimalDamage = this.setting("minimal-damage", "Minimal damage for the target to be valid", Double.valueOf(12.0), 0.0, 100.0);
   private final Setting<Integer> actionInterval = this.setting("action-interval", "Delay between actions", Integer.valueOf(0), 0.0, 10.0);
   private final Setting<Integer> switchDelay = this.setting("delay-between-switch", "Delay between swapping inventory slots", Integer.valueOf(3), 0.0, 10.0);
   private final Setting<Integer> blockingBreakDelay = this.setting(
      "blocking-break-delay", "Time in ticks for when to break a blocking crystal", Integer.valueOf(5), 0.0, 10.0
   );
   private final Setting<Boolean> swing = this.setting("swing", "Swing hand", Boolean.valueOf(true), this.sgToggles);
   private final Setting<Boolean> disableWhenNone = this.setting(
      "disable-when-none", "Disables the module when out of resources", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<Boolean> strict = this.setting("strict", "Strict mode", Boolean.valueOf(false), this.sgToggles);
   private final Setting<Boolean> antiSuicide = this.setting(
      "anti-suicide", "Prevents you from dying (doesnt seem to work)", Boolean.valueOf(false), this.sgToggles
   );
   private final Setting<Boolean> mine = this.setting("mine", "Mines redstone blocks (slower but more reliable)", Boolean.valueOf(false), this.sgToggles);
   private final Setting<Boolean> torchSupport = this.setting(
      "torch-support", "Whether to place support blocks for redstone torches", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<Boolean> crystalSupport = this.setting(
      "crystal-support", "whether to place support blocks for end crystals", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<Boolean> debugRender = this.setting(
      "render-place-positions", "whether to render placement positions", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<Boolean> pauseOnEat = this.setting("pause-on-eat", "Pauses while eating.", Boolean.valueOf(true), this.sgToggles);
   private final Setting<Boolean> pauseOnDrink = this.setting("pause-on-drink", "Pauses while drinking potions.", Boolean.valueOf(true), this.sgToggles);
   private final Setting<Boolean> pauseOnMine = this.setting("pause-on-mine", "Pauses while mining blocks.", Boolean.valueOf(true), this.sgToggles);
   private final Setting<Boolean> debugPrint = this.setting("debug-messages", "Prints out debug messages.", Boolean.valueOf(false), this.sgToggles);
   private PistonAura.Stage stage;
   private Runnable postAction;
   private PlayerEntity target;
   private BlockPos facePos;
   private Direction faceOffset;
   private BlockPos crystalPos;
   private EndCrystalEntity crystal;
   private BlockPos pistonPos;
   private BlockPos torchPos;
   private BlockPos currentMining;
   private boolean skipPiston;
   private boolean canSupport;
   private boolean canCrystalSupport;
   private boolean hasRotated;
   private boolean changePickItem;
   private boolean mining;
   private int miningTicks;
   private int tickCounter;
   private int delayAfterSwitch;

   public PistonAura() {
      super(Venomhack420.CATEGORY, "PistonAura", "Pushes end crystals into the enemy using pistons");
   }

   public void onActivate() {
      if (Utils.canUpdate()) {
         this.resetAll();
      }
   }

   public void onDeactivate() {
      this.resetAll();
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.debugRender.get()) {
         if (this.facePos != null) {
            event.renderer.box(this.facePos, Color.WHITE, Color.WHITE, ShapeMode.Lines, 0);
         }

         if (this.crystalPos != null) {
            event.renderer.box(this.crystalPos, Color.WHITE, Color.RED, ShapeMode.Lines, 0);
         }

         if (this.pistonPos != null) {
            event.renderer.box(this.pistonPos, Color.WHITE, new Color(0, 176, 255), ShapeMode.Lines, 0);
         }

         if (this.torchPos != null) {
            event.renderer.box(this.torchPos, Color.WHITE, new Color(255, 72, 0), ShapeMode.Lines, 0);
         }
      }
   }

   @EventHandler
   public void onTickPre(Pre event) {
      if (this.disableWhenNone.get()) {
         boolean redstoneSlot = InvUtils.findInHotbar(new Item[]{Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH}).found();
         boolean pistonSlot = InvUtils.findInHotbar(new Item[]{Items.PISTON, Items.STICKY_PISTON}).found();
         boolean crystalSlot = InvUtils.findInHotbar(new Item[]{Items.END_CRYSTAL}).found();
         if (!redstoneSlot || !pistonSlot || !crystalSlot) {
            this.info("Out of materials.", new Object[0]);
            this.toggle();
            return;
         }
      }

      if (!PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnDrink.get())) {
         if (this.torchPos != null && this.crystalPos == null && !this.mining) {
            this.mine(this.torchPos, false);
         }

         if (this.miningTicks < 10 && this.mining) {
            ++this.miningTicks;
         }

         if (this.miningTicks >= 10) {
            this.mine(this.currentMining, true);
            this.miningTicks = 0;
         }

         if (!this.mining) {
            if (this.tickCounter < this.actionInterval.get()) {
               ++this.tickCounter;
            }

            if (this.tickCounter < this.actionInterval.get()) {
               return;
            }

            if (this.postAction == null) {
               this.handleAction();
            }
         }
      }
   }

   @EventHandler
   public void onTickPost(Post event) {
      if (this.postAction != null && !this.mining) {
         if (this.stage != PistonAura.Stage.SEARCHING || this.pistonPos == null || this.faceOffset == null) {
            this.tickCounter = 0;
            this.postAction.run();
            this.postAction = null;
            this.handleAction();
         } else if (!this.hasRotated) {
            float yaw = (float)this.getRotationYaw(this.faceOffset.getOpposite());
            Rotations.rotate((double)yaw, 0.0, () -> this.hasRotated = true);
         } else {
            this.tickCounter = 0;
            this.postAction.run();
            this.postAction = null;
            this.handleAction();
         }
      } else {
         if (this.torchPos != null && this.mc.world.getBlockState(this.torchPos).isAir()) {
            this.mining = false;
         }
      }
   }

   @EventHandler
   public void onReceivePacket(Receive event) {
      Packet deadEntity = event.packet;
      if (deadEntity instanceof EntityStatusS2CPacket packet && this.target != null && packet.getStatus() == 3) {
         Entity deadEntityx = packet.getEntity(this.mc.world);
         if (deadEntityx instanceof PlayerEntity && this.target.equals(deadEntityx)) {
            this.stage = PistonAura.Stage.SEARCHING;
         }
      }

      deadEntity = event.packet;
      if (deadEntity instanceof BlockUpdateS2CPacket packet
         && this.torchPos != null
         && packet.getPos().equals(this.torchPos)
         && packet.getState().isAir()) {
         this.miningTicks = 0;
         this.currentMining = null;
         this.mining = false;
         this.torchPos = null;
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
         this.delayAfterSwitch = this.switchDelay.get();
      }
   }

   private void handleAction() {
      if (!this.strict.get() || !(this.mc.player.getVelocity().length() > 0.08)) {
         switch(this.stage) {
            case SEARCHING:
               for(PlayerEntity candidate : this.getTargets()) {
                  if (this.evaluateTarget(candidate)) {
                     if (this.debugPrint.get()) {
                        this.info("found target", new Object[0]);
                     }

                     this.target = candidate;
                     if (this.skipPiston) {
                        this.stage = PistonAura.Stage.CRYSTAL;
                        this.skipPiston = false;
                        return;
                     }

                     FindItemResult fir = InvUtils.findInHotbar(new Item[]{Items.PISTON, Items.STICKY_PISTON});
                     if (!this.mine.get() && this.torchPos != null && this.mc.world.getBlockState(this.torchPos).emitsRedstonePower()) {
                        return;
                     }

                     int prevSlot = this.mc.player.getInventory().selectedSlot;
                     boolean changeItem = prevSlot != fir.slot();
                     if (changeItem) {
                        InvUtils.swap(fir.slot(), false);
                     }

                     this.postAction = () -> {
                        int yaw = this.getRotationYaw(this.faceOffset.getOpposite());
                        Rotations.rotate((double)yaw, 0.0, () -> {
                           BlockUtils.place(this.pistonPos, fir, false, 0, this.swing.get(), false);
                           this.hasRotated = false;
                        });
                        this.stage = PistonAura.Stage.CRYSTAL;
                     };
                     return;
                  }
               }
               break;
            case CRYSTAL:
               this.crystal = this.getCrystalAtPos(this.crystalPos);
               if (this.crystal != null && this.pistonPos != null) {
                  this.stage = PistonAura.Stage.REDSTONE;
                  return;
               }

               if (!this.canPlaceCrystal(this.crystalPos.down(), this.canCrystalSupport)) {
                  this.stage = PistonAura.Stage.SEARCHING;
                  return;
               }

               FindItemResult fir = InvUtils.findInHotbar(new Item[]{Items.END_CRYSTAL});
               FindItemResult crystalSupport = null;
               if (this.canCrystalSupport) {
                  crystalSupport = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
               }

               int prevSlot = this.mc.player.getInventory().selectedSlot;
               boolean changeItem = prevSlot != fir.slot();
               if (changeItem) {
                  InvUtils.swap(fir.slot(), false);
               }

               this.postAction = () -> {
                  if (this.canCrystalSupport && this.crystalPos != null) {
                     assert crystalSupport != null;

                     BlockUtils.place(this.crystalPos.down(), crystalSupport, false, 0, this.swing.get(), false);
                     this.canCrystalSupport = false;
                  }

                  BlockUtils.place(this.crystalPos, fir, false, 0, this.swing.get(), false);
                  this.stage = PistonAura.Stage.REDSTONE;
               };
               break;
            case REDSTONE:
               if (this.facePos == null || this.torchPos == null || !this.mc.world.getBlockState(this.torchPos).getMaterial().isReplaceable()) {
                  this.stage = PistonAura.Stage.SEARCHING;
                  return;
               }

               if (this.canCrystalSupport && this.getBlock(this.crystalPos.down()) != Blocks.OBSIDIAN
                  || this.getCrystalAtPos(this.crystalPos) == null) {
                  this.stage = PistonAura.Stage.CRYSTAL;
                  return;
               }

               FindItemResult fir = InvUtils.findInHotbar(new Item[]{Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH});
               FindItemResult supportBlock = null;
               if (this.canSupport) {
                  supportBlock = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600.0F);
               }

               int prevSlot = this.mc.player.getInventory().selectedSlot;
               boolean changeItem = prevSlot != fir.slot();
               if (changeItem) {
                  InvUtils.swap(fir.slot(), false);
               }

               this.postAction = () -> {
                  if (this.canSupport && this.torchPos != null) {
                     assert supportBlock != null;

                     BlockUtils.place(this.torchPos.down(), supportBlock, false, 0, this.swing.get(), false);
                     this.canSupport = false;
                  }

                  BlockUtils.place(this.torchPos, fir, false, 0, this.swing.get(), false);
                  this.stage = PistonAura.Stage.BREAKING;
               };
               break;
            case BREAKING:
               if (!this.delayCheck()) {
                  return;
               }

               EndCrystalEntity crystalAtPos = this.getCrystalAtPos(this.crystalPos);
               this.crystal = crystalAtPos == null ? this.getCrystalAtPos(this.facePos) : crystalAtPos;
               if (this.crystal == null) {
                  return;
               }

               if (this.crystalPos != null) {
                  if (!(this.getBlock(this.pistonPos) instanceof PistonBlock)) {
                     this.stage = PistonAura.Stage.SEARCHING;
                  }

                  if (this.crystal.age > this.blockingBreakDelay.get()) {
                     this.stage = PistonAura.Stage.SEARCHING;
                  }

                  boolean blastResistantAtFace = this.getBlock(this.facePos).getBlastResistance() > 600.0F;
                  double offsetForBlastResistant = blastResistantAtFace ? 0.0 : 0.5;
                  double damage = this.damageCalc.get() == PistonAura.DamageCalc.PLACEPOS
                     ? DamageUtils.crystalDamage(
                        this.target,
                        new Vec3d(
                           (double)this.facePos.getX() + 0.5,
                           (double)this.facePos.getY() + offsetForBlastResistant,
                           (double)this.facePos.getZ() + 0.5
                        ),
                        false,
                        null,
                        blastResistantAtFace
                     )
                     : DamageUtils.crystalDamage(
                        this.target, this.crystal.getPos().add(0.0, blastResistantAtFace ? -0.5 : 0.0, 0.0), false, null, blastResistantAtFace
                     );
                  if (this.debugPrint.get()) {
                     this.info("Damage: " + damage, new Object[0]);
                  }

                  if (damage < this.minimalDamage.get() && !this.pistonHeadBlocking(this.pistonPos)) {
                     return;
                  }

                  this.postAction = () -> {
                     this.breakCrystal(this.crystal);
                     if (this.mine.get() && this.torchPos != null && this.torchPos.equals(this.pistonPos.down())) {
                        this.mine(this.torchPos, false);
                     }

                     this.resetStage();
                  };
               } else if (this.pistonPos != null && this.pistonHeadBlocking(this.pistonPos)
                  || this.crystalPos != null && !this.mc.world.getBlockState(this.crystalPos).getMaterial().isReplaceable()) {
                  this.postAction = () -> {
                     if (this.mine.get() && this.torchPos != null && this.torchPos.equals(this.pistonPos.down())) {
                        this.mine(this.torchPos, false);
                     }

                     this.resetStage();
                  };
               }
         }
      }
   }

   private boolean pistonHeadBlocking(BlockPos pos) {
      for(Direction direction : class_2353.HORIZONTAL) {
         if (this.getBlock(pos.offset(direction)) == Blocks.PISTON_HEAD) {
            return true;
         }
      }

      return false;
   }

   private boolean evaluateTarget(PlayerEntity candidate) {
      BlockPos tempFacePos = new BlockPos(candidate.getPos()).up();
      if (this.evaluateTarget(tempFacePos, candidate)) {
         return true;
      } else {
         return this.evaluateTarget(tempFacePos.up(), candidate) ? true : this.evaluateTarget(tempFacePos.up(2), candidate);
      }
   }

   private boolean evaluateTarget(BlockPos tempFacePos, PlayerEntity candidate) {
      BlockPos tempCrystalPos = null;
      BlockPos tempPistonPos = null;
      BlockPos tempTorchPos = null;
      Direction offset = null;
      List<EndCrystalEntity> crystalList = this.mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(tempFacePos).contract(0.2), Entity::isAlive);
      EndCrystalEntity blockingCrystal = null;

      for(EndCrystalEntity crystal : crystalList) {
         if (crystal.age > this.blockingBreakDelay.get()) {
            blockingCrystal = crystal;
            break;
         }
      }

      if (blockingCrystal != null) {
         if (this.debugPrint.get()) {
            this.info("breaking due to delay", new Object[0]);
         }

         if (this.delayCheck()) {
            this.breakCrystal(blockingCrystal);
         }

         return false;
      } else {
         this.skipPiston = false;
         this.canSupport = false;
         this.canCrystalSupport = false;

         for(Direction faceOffset : class_2353.HORIZONTAL) {
            BlockPos potentialCrystal = tempFacePos.offset(faceOffset);
            BlockState potCrystalState = this.mc.world.getBlockState(potentialCrystal);
            if (!EntityUtils.intersectsWithEntity(new Box(potentialCrystal), Entity::isLiving)) {
               FindItemResult supportBlock = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
               if (!supportBlock.found() || !this.crystalSupport.get()) {
                  continue;
               }

               if (potCrystalState.getMaterial().isReplaceable() || potCrystalState.isAir()) {
                  this.canCrystalSupport = true;
               }
            }

            if (this.canPlaceCrystal(potentialCrystal.down(), this.canCrystalSupport)) {
               boolean blastResistantAtFace = this.getBlock(tempFacePos).getBlastResistance() > 600.0F;
               double offsetForBlastResistant = blastResistantAtFace ? 0.0 : 0.5;
               Vec3d calculatedCrystalPos = new Vec3d(
                  (double)tempFacePos.getX() + 0.5,
                  (double)tempFacePos.getY() + offsetForBlastResistant,
                  (double)tempFacePos.getZ() + 0.5
               );
               float damage = DamageCalcUtils.explosionDamage(candidate, calculatedCrystalPos, blastResistantAtFace, false, true, 6);
               if (!((double)damage < this.minimalDamage.get())) {
                  if (this.antiSuicide.get()) {
                     float selfDamage = DamageCalcUtils.explosionDamage(this.mc.player, Vec3d.ofCenter(potentialCrystal), 6);
                     if (selfDamage >= EntityUtils.getTotalHealth(this.mc.player)) {
                        continue;
                     }
                  }

                  BlockPos potentialPiston = tempFacePos.offset(faceOffset, 2);
                  BlockState pistonState = this.mc.world.getBlockState(potentialPiston);
                  this.skipPiston = this.getBlock(potentialPiston) instanceof PistonBlock;
                  if (!BlockUtils2.outOfPlaceRange(potentialPiston, Origin.VANILLA, (double)this.mc.interactionManager.getReachDistance())
                     && (pistonState.isAir() || pistonState.getMaterial().isReplaceable() || this.skipPiston)
                     && (!pistonState.isAir() || !EntityUtils.intersectsWithEntity(new Box(potentialPiston), Entity::isLiving))) {
                     Item redstone = null;
                     FindItemResult firT = InvUtils.findInHotbar(new Item[]{Items.REDSTONE_TORCH});
                     FindItemResult firB = InvUtils.findInHotbar(new Item[]{Items.REDSTONE_BLOCK});
                     if (firT.found() && firB.found()) {
                        redstone = firT.slot() > firB.slot() ? Items.REDSTONE_BLOCK : Items.REDSTONE_TORCH;
                     }

                     if (firT.found() && !firB.found()) {
                        redstone = Items.REDSTONE_TORCH;
                     }

                     BlockPos[] places = new BlockPos[this.mine.get() ? 2 : 1];
                     places[0] = potentialPiston.offset(faceOffset);
                     if (this.mine.get()) {
                        places[1] = potentialPiston.offset(Direction.DOWN);
                     }

                     BlockPos[] var24 = places;
                     int var25 = places.length;
                     int var26 = 0;

                     while(var26 < var25) {
                        BlockPos potentialRedstone;
                        label184: {
                           potentialRedstone = var24[var26];
                           if (!BlockUtils2.outOfPlaceRange(potentialRedstone, Origin.VANILLA, (double)this.mc.interactionManager.getReachDistance())) {
                              BlockState state = this.mc.world.getBlockState(potentialRedstone);
                              if ((
                                    state.isAir()
                                       || state.getMaterial().isReplaceable()
                                          && state.emitsRedstonePower()
                                          && !pistonState.isAir()
                                          && this.mining
                                          && potentialRedstone.equals(this.torchPos)
                                 )
                                 && !EntityUtils.intersectsWithEntity(new Box(potentialRedstone), Entity::isLiving)) {
                                 if (potentialRedstone != places[0] || redstone == null || redstone != Items.REDSTONE_TORCH) {
                                    break label184;
                                 }

                                 FindItemResult supportBlock = InvUtils.findInHotbar(
                                    itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600.0F
                                 );
                                 if (supportBlock.found() && this.torchSupport.get()) {
                                    BlockPos downPos = potentialRedstone.down();
                                    if (!BlockUtils2.outOfPlaceRange(downPos, Origin.VANILLA, (double)this.mc.interactionManager.getReachDistance())) {
                                       BlockState supportState = this.mc.world.getBlockState(downPos);
                                       if ((
                                             supportState.isAir()
                                                || supportState.isFullCube(this.mc.world, downPos)
                                                   && !(supportState.getBlock().getBlastResistance() <= 600.0F)
                                          )
                                          && !EntityUtils.intersectsWithEntity(new Box(downPos), Entity::isLiving)) {
                                          this.canSupport = true;
                                          break label184;
                                       }
                                    }
                                 }
                              }
                           }

                           ++var26;
                           continue;
                        }

                        tempTorchPos = potentialRedstone;
                        break;
                     }

                     if (tempTorchPos != null) {
                        tempCrystalPos = potentialCrystal;
                        tempPistonPos = potentialPiston;
                        offset = faceOffset;
                        break;
                     }
                  }
               }
            }
         }

         if (tempCrystalPos != null) {
            this.faceOffset = offset;
            this.facePos = tempFacePos;
            this.crystalPos = tempCrystalPos;
            this.crystal = this.getCrystalAtPos(this.crystalPos);
            this.pistonPos = tempPistonPos;
            this.torchPos = tempTorchPos;
            return true;
         } else {
            return false;
         }
      }
   }

   private EndCrystalEntity getCrystalAtPos(BlockPos pos) {
      Vec3d middlePos = Vec3d.ofCenter(pos);
      List<EndCrystalEntity> crystalList = this.mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos).contract(0.5), Entity::isAlive);
      if (crystalList.isEmpty()) {
         return null;
      } else if (crystalList.size() == 1) {
         return (EndCrystalEntity)crystalList.get(0);
      } else {
         EndCrystalEntity nearestCrystal = null;

         for(EndCrystalEntity crystal : crystalList) {
            if (nearestCrystal == null) {
               nearestCrystal = crystal;
            }

            if (crystal.squaredDistanceTo(middlePos) < nearestCrystal.squaredDistanceTo(middlePos)) {
               nearestCrystal = crystal;
            }
         }

         return nearestCrystal;
      }
   }

   private boolean canPlaceCrystal(BlockPos blockPos, boolean support) {
      BlockState blockState = this.mc.world.getBlockState(blockPos);
      BlockPos blockPosUp = blockPos.up();
      if (!blockState.isOf(Blocks.BEDROCK) && !blockState.isOf(Blocks.OBSIDIAN) && !support) {
         return false;
      } else if (!this.mc.world.getBlockState(blockPosUp).isAir()) {
         return false;
      } else {
         int x = blockPosUp.getX();
         int y = blockPosUp.getY();
         int z = blockPosUp.getZ();
         return this.mc
            .world
            .getOtherEntities(null, new Box((double)x, (double)y, (double)z, (double)(x + 1), (double)(y + 2), (double)(z + 1)))
            .isEmpty();
      }
   }

   private List<PlayerEntity> getTargets() {
      List<PlayerEntity> players = this.mc
         .world
         .getEntitiesByClass(
            PlayerEntity.class,
            new Box(this.mc.player.getBlockPos()).expand((double)((Integer)this.targetRange.get()).intValue()),
            Predicate.not(PlayerEntity::isMainPlayer)
         );
      return players.isEmpty()
         ? players
         : players.stream()
            .filter(LivingEntity::isAlive)
            .filter(playerEntity -> Friends.get().shouldAttack(playerEntity))
            .sorted(Comparator.comparing(e -> this.mc.player.distanceTo(e)))
            .collect(Collectors.toList());
   }

   private Block getBlock(BlockPos bp) {
      return this.mc.world.getBlockState(bp).getBlock();
   }

   private void breakCrystal(Entity crystal) {
      if (crystal != null) {
         Hand hand = this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
         this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
         if (this.swing.get()) {
            this.mc.player.swingHand(hand);
         } else {
            this.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
         }
      }
   }

   private boolean delayCheck() {
      if (this.delayAfterSwitch > 0) {
         --this.delayAfterSwitch;
         return false;
      } else {
         return true;
      }
   }

   private int getRotationYaw(Direction dir) {
      return switch(dir) {
         case EAST -> 90;
         case SOUTH -> 180;
         case WEST -> -90;
         default -> 0;
      };
   }

   private void mine(BlockPos blockPos, boolean override) {
      if (blockPos != null) {
         BlockState state = this.mc.world.getBlockState(blockPos);
         if (!this.mining && this.getBlock(blockPos).getHardness() >= 0.0F && !state.isAir() || override) {
            FindItemResult pickaxe = InvUtils.findInHotbar(new Item[]{Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE});
            int pickPrevSlot = this.mc.player.getInventory().selectedSlot;
            this.changePickItem = pickaxe.slot() != pickPrevSlot;
            if (this.changePickItem) {
               InvUtils.swap(pickaxe.slot(), false);
            }

            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, blockPos, Direction.WEST));
            this.mc.player.swingHand(Hand.MAIN_HAND);
            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, blockPos, Direction.WEST));
            this.mining = true;
            this.currentMining = blockPos;
            if (this.mc.interactionManager.getCurrentGameMode().isCreative() && blockPos.equals(this.torchPos)) {
               this.mining = false;
               this.torchPos = null;
            }
         }
      }
   }

   private void resetAll() {
      this.stage = PistonAura.Stage.SEARCHING;
      this.postAction = null;
      this.target = null;
      this.facePos = null;
      this.faceOffset = null;
      this.crystalPos = null;
      this.crystal = null;
      this.skipPiston = false;
      this.pistonPos = null;
      this.torchPos = null;
      this.hasRotated = false;
      this.changePickItem = false;
      this.mining = false;
      this.canSupport = false;
      this.canCrystalSupport = false;
      this.currentMining = null;
      this.miningTicks = 0;
      this.tickCounter = 0;
      this.delayAfterSwitch = 0;
   }

   private void resetStage() {
      this.faceOffset = null;
      this.facePos = null;
      this.crystalPos = null;
      this.pistonPos = null;
      this.target = null;
      this.stage = PistonAura.Stage.SEARCHING;
   }

   public static enum DamageCalc {
      PLACEPOS,
      CRYSTALPOS;
   }

   private static enum Stage {
      SEARCHING,
      CRYSTAL,
      REDSTONE,
      BREAKING;
   }
}
