package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Box2;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import venomhack.Venomhack420;
import venomhack.enums.Mode;
import venomhack.enums.RenderShape;
import venomhack.mixinInterface.IModule;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.CityUtils;
import venomhack.utils.ColorUtils;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.PathUtils;
import venomhack.utils.RandUtils;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.RenderBlock;

public class AutoFunnyCrystal extends ModuleHelper implements IModule {
   private final SettingGroup sgRender = this.group("Render");
   private final Setting<Mode> mode = this.setting("mode", "Which mode to use for breaking the obsidian.", Mode.PACKET);
   private final Setting<Boolean> strictDirections = this.setting("strict-directions", "Places only on visible sides.", Boolean.valueOf(false));
   private final Setting<Boolean> rotate = this.setting("rotate", "Whether to rotate or not.", Boolean.valueOf(false));
   private final Setting<Integer> delay = this.setting(
      "delay", "How many ticks to wait between breaking the obsidian.", Integer.valueOf(10), this.sgGeneral, () -> this.mode.get() == Mode.INSTANT, 0.0, 10.0
   );
   private final Setting<Boolean> pauseOnEat = this.setting("pause-on-eat", "Pauses while eating.", Boolean.valueOf(true));
   private final Setting<Boolean> airPlace = this.setting(
      "air-place", "Places blocks midair, will try to find support blocks when off.", Boolean.valueOf(true)
   );
   private final Setting<Boolean> swapBack = this.setting(
      "swap-back", "Will swap to your last selected slot after placing the crystal.", Boolean.valueOf(true), () -> this.mode.get() != Mode.BYPASS
   );
   private final Setting<Boolean> trap = this.setting("trap", "Will trap the target so it can't jump out.", Boolean.valueOf(false));
   private final Setting<Boolean> swing = this.setting("swing", "Renders your swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting(
      "render", "Renders the block under where it is placing a crystal.", Boolean.valueOf(true), this.sgRender
   );
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the shape is rendered.", ShapeMode.Both, this.sgRender, this.render::get);
   private final Setting<RenderShape> renderShape = this.setting("render-shape", "Funky rendering stuff", RenderShape.CUBOID, this.sgRender);
   private final Setting<Double> height = this.setting("height", "The height.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 1.0);
   private final Setting<Double> width = this.setting("width", "The width.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 1.0);
   private final Setting<Double> yOffset = this.setting(
      "y-offset", "The height shift for the rendered box.", Double.valueOf(0.0), this.sgRender, this.render::get, 0.0, 1.0
   );
   private final Setting<Boolean> renderProgress = this.setting("render-progress", "Renders the block break progress.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Double> progressScale = this.setting(
      "progress-scale", "The scale of the progress text.", Double.valueOf(1.4), this.sgRender, this.renderProgress::get, 0.0, 5.0
   );
   private final Setting<SettingColor> progressColor = this.setting(
      "progress-color", "The color of the progress text.", 255, 255, 255, 255, this.sgRender, this.renderProgress::get
   );
   private final Setting<Boolean> percentageColor = this.setting(
      "use-percentage-color", "Will render red when starting and green when finished with gradient.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<Integer> percentageAlpha = this.setting(
      "percentage-alpha",
      "The alpha setting for percentage color.",
      Integer.valueOf(100),
      this.sgRender,
      () -> this.render.get() && this.percentageColor.get(),
      0.0,
      255.0,
      0,
      255
   );
   private final Setting<SettingColor> sideColor = this.setting(
      "side-color", "The side color.", 255, 0, 0, 75, true, this.sgRender, () -> this.render.get() && !this.percentageColor.get()
   );
   private final Setting<SettingColor> lineColor = this.setting(
      "line-color", "The line color.", 255, 0, 0, 200, true, this.sgRender, () -> this.render.get() && !this.percentageColor.get()
   );
   private PlayerEntity target;
   private BlockPos targetPos;
   private BlockPos mineTarget;
   private boolean startedYet;
   private int switchDelayLeft;
   private int delayLeft;
   private int max;
   private int lastSlot;
   private int stage;
   private final List<PlayerEntity> blacklisted = new ArrayList();
   private final List<EndCrystalEntity> crystals = new ArrayList();

   public AutoFunnyCrystal() {
      super(Venomhack420.CATEGORY, "auto-funny-crystal", "Aka CEV Breaker.");
   }

   @EventHandler
   private void onTick(Pre event) {
      --this.switchDelayLeft;
      --this.delayLeft;
      Item mItem = this.mc.player.getMainHandStack().getItem();
      Item oItem = this.mc.player.getOffHandStack().getItem();
      if (!this.pauseOnEat.get() || !this.mc.player.isUsingItem() || !mItem.isFood() && !oItem.isFood()) {
         if (this.noEntities(null)) {
            this.error("No target found, disabling...", new Object[0]);
            this.toggle();
         } else if (!this.target.isDead() && !(this.target.distanceTo(this.mc.player) > 6.0F)) {
            int crystalSlot = InvUtils.findInHotbar(new Item[]{Items.END_CRYSTAL}).slot();
            FindItemResult obsidianResult = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
            int pickSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem).slot();
            int slot = this.mc.player.getInventory().selectedSlot;
            if (slot != crystalSlot && slot != obsidianResult.slot() && slot != pickSlot) {
               this.lastSlot = slot;
            }

            this.mineTarget = this.targetPos.add(0.0, Math.ceil((double)this.target.getEyeHeight(this.target.getPose())), 0.0);
            boolean crystalThere = false;

            for(EndCrystalEntity crystal : this.crystals) {
               if (crystal.getBlockPos().add(0, -1, 0).equals(this.mineTarget)) {
                  crystalThere = true;
                  break;
               }
            }

            String errorMsg = crystalSlot == -1 && !crystalThere && !(oItem instanceof EndCrystalItem)
               ? "crystals"
               : (obsidianResult.slot() == -1 ? "obsidian" : (pickSlot == -1 ? "pickaxe" : ""));
            if (!errorMsg.isEmpty()) {
               this.error("No " + errorMsg + " found, disabling...", new Object[0]);
               this.toggle();
            } else {
               BlockState blockState = this.mc.world.getBlockState(this.mineTarget);
               float[] rotation = PlayerUtils.calculateAngle(
                  new Vec3d(
                     (double)this.mineTarget.getX() + 0.5, (double)this.mineTarget.getY() + 0.5, (double)this.mineTarget.getZ() + 0.5
                  )
               );
               if (!blockState.isOf(Blocks.OBSIDIAN) && !crystalThere && (mItem.equals(Items.OBSIDIAN) || this.switchDelayLeft <= 0)) {
                  FindItemResult result = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
                  if (this.airPlace.get()) {
                     if (!BlockUtils2.placeBlock(
                        result, this.mineTarget, this.rotate.get(), 25, this.airPlace.get(), false, this.swing.get(), this.strictDirections.get()
                     )) {
                        this.badTarget(this.target, "Can't place obsidian above the target! Disabling...");
                        return;
                     }
                  } else {
                     List<BlockPos> list = new ArrayList();
                     boolean couldPlace = false;
                     if (this.findNeighbour(list, this.mineTarget, 0)) {
                        this.add(list, this.mineTarget);

                        for(BlockPos pos : list) {
                           if (BlockUtils2.placeBlock(
                              result, pos, this.rotate.get(), 25, this.airPlace.get(), false, this.swing.get(), this.strictDirections.get()
                           )) {
                              couldPlace = true;
                              this.stage = 1;
                           }
                        }

                        if (!couldPlace) {
                           this.badTarget(this.target, "Can't place obsidian above the target! Disabling...");
                           return;
                        }
                     }
                  }
               }

               boolean offhand = oItem instanceof EndCrystalItem;
               boolean mainhand = mItem instanceof EndCrystalItem;
               if (!crystalThere && blockState.isOf(Blocks.OBSIDIAN)) {
                  if (!offhand && !mainhand && this.switchDelayLeft > 0) {
                     return;
                  }

                  double x = (double)this.mineTarget.up().getX();
                  double y = (double)this.mineTarget.up().getY();
                  double z = (double)this.mineTarget.up().getZ();
                  if (EntityUtils.intersectsWithEntity(new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0), entity -> true)) {
                     this.badTarget(this.target, "Can't place the crystal, there are entities in its way! Disabling...");
                     return;
                  }

                  if (!this.mc.world.getBlockState(this.mineTarget.up()).isAir()) {
                     this.badTarget(this.target, "Can't place the crystal, there is not enough space! Disabling...");
                     return;
                  }

                  if (!offhand && !mainhand) {
                     this.mc.player.getInventory().selectedSlot = crystalSlot;
                  }

                  Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  BlockHitResult result = new BlockHitResult(
                     this.mc.player.getPos(),
                     (double)this.mineTarget.getY() < this.mc.player.getY() ? Direction.UP : Direction.DOWN,
                     this.mineTarget,
                     false
                  );
                  if (this.rotate.get()) {
                     Rotations.rotate(
                        (double)rotation[0], (double)rotation[1], 25, () -> this.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0))
                     );
                  } else {
                     this.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));
                  }

                  RandUtils.swing(this.swing.get(), hand);
                  this.stage = 2;
               }

               if (this.trap.get() && (mItem.equals(Items.OBSIDIAN) || this.switchDelayLeft <= 0)) {
                  for(Box2 offset : CityUtils.CITY_WITHOUT_BURROW) {
                     BlockPos trapPos = this.targetPos.add(offset).up();
                     if (this.mc.world.getBlockState(trapPos).getMaterial().isReplaceable()
                        && this.mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), trapPos, ShapeContext.absent())) {
                        BlockUtils.place(trapPos, obsidianResult, 25);
                     }
                  }
               }

               if (this.swapBack.get()
                  && this.switchDelayLeft <= 0
                  && !blockState.isAir()
                  && this.mode.get() != Mode.BYPASS
                  && this.delayLeft > 2
                  && this.switchDelayLeft <= 0) {
                  this.mc.player.getInventory().selectedSlot = this.lastSlot;
               }

               if ((this.mc.player.getInventory().selectedSlot == pickSlot || this.switchDelayLeft <= 0)
                  && crystalThere
                  && blockState.isOf(Blocks.OBSIDIAN)) {
                  Direction direction = UtilsPlus.rayTraceCheck(this.mineTarget, true);
                  this.max = CityUtils.getBlockBreakingSpeed(blockState, this.mineTarget, pickSlot);
                  switch((Mode)this.mode.get()) {
                     case PACKET:
                        if (this.startedYet && !blockState.isAir() && (!this.startedYet || this.delayLeft > -2)) {
                           if (this.delayLeft <= 0) {
                              this.mc.player.getInventory().selectedSlot = pickSlot;
                           }
                        } else {
                           this.delayLeft = this.max;
                           UtilsPlus.mine(this.mineTarget, this.swing.get(), this.rotate.get());
                           this.startedYet = true;
                        }
                        break;
                     case INSTANT:
                        if (!this.startedYet) {
                           if (this.rotate.get()) {
                              Rotations.rotate(
                                 (double)rotation[0],
                                 (double)rotation[1],
                                 25,
                                 () -> this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, this.mineTarget, direction))
                              );
                           } else {
                              this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, this.mineTarget, direction));
                           }

                           RandUtils.swing(this.swing.get());
                           this.delayLeft = this.max;
                           this.startedYet = true;
                        } else if (this.delayLeft <= 0) {
                           this.mc.player.getInventory().selectedSlot = pickSlot;
                           if (this.rotate.get()) {
                              Rotations.rotate(
                                 (double)rotation[0],
                                 (double)rotation[1],
                                 25,
                                 () -> this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, this.mineTarget, direction))
                              );
                           } else {
                              this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, this.mineTarget, direction));
                           }

                           RandUtils.swing(this.swing.get());
                           this.delayLeft = this.delay.get();
                        }
                        break;
                     default:
                        this.mc.player.getInventory().selectedSlot = pickSlot;
                        if (this.rotate.get()) {
                           Rotations.rotate((double)rotation[0], (double)rotation[1], 25, () -> this.mc.interactionManager.updateBlockBreakingProgress(this.mineTarget, direction));
                        } else {
                           this.mc.interactionManager.updateBlockBreakingProgress(this.mineTarget, direction);
                        }

                        RandUtils.swing(this.swing.get());
                        if (!this.startedYet || blockState.isAir() || this.startedYet && this.delayLeft <= -2) {
                           this.startedYet = true;
                           this.delayLeft = this.max;
                        }
                  }
               }

               if (blockState.isAir()) {
                  if (crystalThere) {
                     this.stage = 3;
                  } else {
                     this.stage = 0;
                  }
               }

               if (!Modules.get().isActive(AutoCrystal.class)) {
                  for(EndCrystalEntity crystal : this.crystals) {
                     if (DamageCalcUtils.explosionDamage(this.target, crystal.getPos(), 6) >= 10.0F) {
                        float[] breakRotation = PlayerUtils.calculateAngle(crystal.getEyePos());
                        if (this.rotate.get()) {
                           Rotations.rotate(
                              (double)breakRotation[0],
                              (double)breakRotation[1],
                              30,
                              () -> this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false))
                           );
                        } else {
                           this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
                        }

                        RandUtils.swing(this.swing.get());
                        this.stage = 0;
                        break;
                     }
                  }
               }
            }
         }
      } else {
         if (this.delayLeft < 0) {
            ++this.delayLeft;
         }
      }
   }

   private void add(List<BlockPos> list, BlockPos pos) {
      if (!list.contains(pos)) {
         if (this.mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
            if (this.airPlace.get()) {
               list.add(pos);
            } else if (this.findNeighbour(list, pos, 0)) {
               list.add(pos);
            }
         }
      }
   }

   private boolean findNeighbour(List<BlockPos> list, BlockPos pos, int iteration) {
      if (iteration > 3) {
         return false;
      } else {
         BlockState placeState = Blocks.OBSIDIAN.getDefaultState();

         for(Direction direction : Direction.values()) {
            if (this.mc.world.canPlace(placeState, pos.offset(direction), ShapeContext.absent())
               && (list.contains(pos.offset(direction)) || !this.mc.world.getBlockState(pos.offset(direction)).getMaterial().isReplaceable())) {
               return true;
            }
         }

         for(Direction direction : Direction.values()) {
            if (this.mc.world.canPlace(placeState, pos.offset(direction), ShapeContext.absent())
               && this.findNeighbour(list, pos.offset(direction), iteration + 1)) {
               PathUtils.smartAdd(list, pos.offset(direction));
               return true;
            }
         }

         return false;
      }
   }

   private void badTarget(PlayerEntity target, String message) {
      if (this.noEntities(target)) {
         this.error(message, new Object[0]);
         this.toggle();
      } else {
         this.info("Funny crystalling " + this.target.getEntityName() + "...", new Object[0]);
      }
   }

   private boolean noEntities(PlayerEntity blacklist) {
      this.crystals.clear();
      if (blacklist != null) {
         this.blacklisted.add(blacklist);
      }

      if (this.blacklisted.contains(this.target)) {
         this.target = null;
      }

      for(Entity entity : this.mc.world.getEntities()) {
         if (entity.isInRange(this.mc.player, 6.0) && entity.isAlive() && entity != this.mc.player) {
            if (entity instanceof OtherClientPlayerEntity) {
               if (!this.blacklisted.contains(entity)
                  && Friends.get().shouldAttack((PlayerEntity)entity)
                  && (this.target == null || this.mc.player.distanceTo(entity) < this.mc.player.distanceTo(this.target))) {
                  this.target = (PlayerEntity)entity;
                  this.targetPos = this.target.getBlockPos();
               }
            } else if (entity instanceof EndCrystalEntity) {
               this.crystals.add((EndCrystalEntity)entity);
            }
         }
      }

      return this.target == null;
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
         this.switchDelayLeft = 1;
      }
   }

   @EventHandler
   public void onActivate() {
      this.target = null;
      this.targetPos = null;
      this.mineTarget = null;
      this.startedYet = false;
      this.switchDelayLeft = 0;
      this.delayLeft = 0;
      this.max = 0;
      this.lastSlot = 0;
      this.stage = 0;
      this.blacklisted.clear();
      if (this.mc.player != null) {
         this.badTarget(this.target, "No target found, disabling...");
      }
   }

   @EventHandler(
      priority = -100
   )
   private void onPostTick(Post event) {
      if (this.target != null) {
         if (this.target.isDead() || this.target.distanceTo(this.mc.player) > 6.0F || !this.target.getBlockPos().equals(this.targetPos)) {
            this.target = null;
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mineTarget != null && this.render.get() && this.target != null) {
         SettingColor centColor = ColorUtils.getColorFromPercent((double)(this.max - this.delayLeft) / (double)this.max);
         SettingColor cColor = new SettingColor(centColor.r, centColor.g, centColor.b, this.percentageAlpha.get());
         RenderBlock renderBlock = new RenderBlock(this.mineTarget, 1);
         renderBlock.complexRender(
            event,
            this.percentageColor.get() ? cColor : (SettingColor)this.sideColor.get(),
            this.percentageColor.get() ? cColor : (SettingColor)this.lineColor.get(),
            (ShapeMode)this.shapeMode.get(),
            false,
            false,
            (RenderShape)this.renderShape.get(),
            this.width.get(),
            this.height.get(),
            this.yOffset.get(),
            1.0
         );
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mineTarget != null && this.renderProgress.get() && this.target != null && !this.mc.player.getAbilities().creativeMode) {
         Vec3 pos = new Vec3(
            (double)this.mineTarget.getX() + 0.5,
            (double)this.mineTarget.getY() + this.yOffset.get() + this.height.get() * 0.5,
            (double)this.mineTarget.getZ() + 0.5
         );
         if (NametagUtils.to2D(pos, this.progressScale.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            String progress = Math.min(100L, Math.round((double)(this.max - this.delayLeft) / (double)this.max * 100.0)) + "%";
            TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, (Color)this.progressColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
         }
      }
   }

   @Override
   public PlayerEntity getTarget() {
      return this.target == null ? null : this.target;
   }

   public String getInfoString() {
      return this.target != null ? this.target.getEntityName() : null;
   }
}
