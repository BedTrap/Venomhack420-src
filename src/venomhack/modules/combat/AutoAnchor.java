package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.Safety;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import venomhack.Venomhack420;
import venomhack.enums.RenderShape;
import venomhack.enums.Type;
import venomhack.events.SoundInstanceEvent;
import venomhack.mixinInterface.IBlink;
import venomhack.mixinInterface.IModule;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RandUtils;
import venomhack.utils.TextUtils;
import venomhack.utils.ThreadedUtils;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.RenderBlock;

public class AutoAnchor extends ModuleHelper implements IModule {
   private final SettingGroup sgPlace = this.group("Place");
   private final SettingGroup sgBreak = this.group("Break");
   private final SettingGroup sgTargeting = this.group("Targeting");
   private final SettingGroup sgPause = this.group("Pause");
   private final SettingGroup sgRotations = this.group("Rotations");
   private final SettingGroup sgRender = this.group("Render");
   private final Setting<Double> range = this.setting("place-range", "The max interact radius.", Double.valueOf(5.0), 0.0, 6.0);
   private final Setting<Double> wallsRange = this.setting("place-walls-range", "TThe max interact radius through walls.", Double.valueOf(5.0), 0.0, 6.0);
   private final Setting<Boolean> threaded = this.setting("threaded", "Allows Auto Anchor to place anchors.", Boolean.valueOf(true));
   public final Setting<Boolean> holeDelays = this.setting("hole-delays", "Uses separate delays when the target is in a hole.", Boolean.valueOf(false));
   private final Setting<Boolean> place = this.setting("place", "Allows Auto Anchor to place anchors.", Boolean.valueOf(true), this.sgPlace);
   private final Setting<Boolean> airPlace = this.setting("air-place", "Places anchors in the air.", Boolean.valueOf(true), this.sgPlace);
   private final Setting<Safety> placeMode = this.setting("place-mode", "The way anchors are allowed to be placed near you.", Safety.Safe, this.sgPlace);
   public final Setting<Integer> placeDelay = this.setting("place-delay", "The tick delay between placing anchors.", Integer.valueOf(10), this.sgPlace);
   public final Setting<Integer> holePlaceDelay = this.setting(
      "hole-place-delay", "The tick delay between placing anchors on targets in holes.", Integer.valueOf(10), this.sgPlace, this.holeDelays::get
   );
   private final Setting<Double> minPlaceDamage = this.setting(
      "min-place-damage", "The minimum damage to inflict on your target.", Double.valueOf(6.5), this.sgPlace, 0.0, 36.0
   );
   private final Setting<Double> maxPlaceSelfDamage = this.setting(
      "max-place-self-damage", "The maximum damage to inflict on yourself.", Double.valueOf(4.0), this.sgPlace, 0.0, 36.0
   );
   private final Setting<Safety> breakMode = this.setting("break-mode", "The way anchors are allowed to be broken near you.", Safety.Safe, this.sgBreak);
   public final Setting<Integer> breakDelay = this.setting("break-delay", "The tick delay between breaking anchors.", Integer.valueOf(0), this.sgBreak);
   public final Setting<Integer> holeBreakDelay = this.setting(
      "hole-break-delay", "The tick delay between breaking anchors on targets in holes.", Integer.valueOf(0), this.sgBreak, this.holeDelays::get
   );
   public final Setting<Integer> retryDelay = this.setting("retry-delay", "The tick delay retrying to break anchors.", 10, this.sgBreak, 20);
   private final Setting<Boolean> packetBreak = this.setting("packet-break", "Packet breaks anchors.", Boolean.valueOf(true), this.sgBreak);
   private final Setting<Double> minBreakDamage = this.setting(
      "min-break-damage", "The minimum damage to inflict on your target.", Double.valueOf(6.5), this.sgBreak, 0.0, 36.0
   );
   private final Setting<Double> maxBreakSelfDamage = this.setting(
      "max-break-self-damage", "The maximum damage to inflict on yourself.", Double.valueOf(4.0), this.sgBreak, 0.0, 36.0
   );
   private final Setting<Boolean> predict = this.setting("predict", "Predicts the targets movement.", Boolean.valueOf(false), this.sgTargeting);
   private final Setting<Boolean> smartDelay = this.setting(
      "smart-delay", "Only considers targets that can take damage.", Boolean.valueOf(true), this.sgTargeting
   );
   private final Setting<Integer> threshold = this.setting(
      "hurt-time-threshold", "Thing for smart delay.", Integer.valueOf(0), this.sgTargeting, this.smartDelay::get
   );
   private final Setting<Boolean> ignoreTerrain = this.setting(
      "ignore-terrain", "Assumes non blast resistant blocks are air.", Boolean.valueOf(true), this.sgTargeting
   );
   private final Setting<Boolean> considerFriends = this.setting(
      "consider-friends", "Takes into account damage to friends when placing/breaking.", Boolean.valueOf(true), this.sgTargeting
   );
   private final Setting<Integer> targetRange = this.setting(
      "target-range", "The radius in which players get targeted.", Integer.valueOf(10), this.sgTargeting, 6.0, 16.0
   );
   private final Setting<SortPriority> sortPrio = this.setting("target-mode", "Target mode.", SortPriority.ClosestAngle, this.sgTargeting);
   private final Setting<Integer> maxTargets = this.setting(
      "number-of-targets",
      "Maximum number of targets to perform calculations on. Might lag your game when set too high.",
      Integer.valueOf(3),
      this.sgTargeting,
      1.0,
      5.0,
      1,
      Integer.MAX_VALUE
   );
   private final Setting<Boolean> pauseOnEat = this.setting("pause-on-eat", "Pauses while eating.", Boolean.valueOf(false), this.sgPause);
   private final Setting<Boolean> pauseOnDrink = this.setting("pause-on-drink", "Pauses while drinking potions.", Boolean.valueOf(false), this.sgPause);
   private final Setting<Boolean> pauseOnMine = this.setting("pause-on-mine", "Pauses while mining blocks.", Boolean.valueOf(false), this.sgPause);
   private final Setting<Boolean> strictDirections = this.setting(
      "strict-directions", "Places only on visible sides.", Boolean.valueOf(false), this.sgRotations
   );
   private final Setting<Type> rotationMode = this.setting("rotation-mode", "The mode to rotate you server-side.", Type.NONE, this.sgRotations);
   private final Setting<Boolean> swing = this.setting("swing", "Renders your hand swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Creates a render effect where you are placing.", Boolean.valueOf(true), this.sgRender);
   private final Setting<RenderShape> renderShape = this.setting("render-shape", "What shape mode to use.", RenderShape.CORNER_PYRAMIDS, this.sgRender);
   private final Setting<ShapeMode> shapeMode = this.setting(
      "shape-mode", "What part of the shapes to render.", ShapeMode.Both, this.sgRender, this.render::get
   );
   private final Setting<Double> height = this.setting("height", "The height of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 2.0);
   private final Setting<Double> width = this.setting("width", "The width of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 2.0);
   private final Setting<Double> yOffset = this.setting(
      "y-offset", "The height shift for the rendered shape.", Double.valueOf(0.0), this.sgRender, this.render::get, 1.0
   );
   private final Setting<Double> weirdOffset = this.setting(
      "weird-offset",
      "Offset for the pyramid render shape.",
      Double.valueOf(0.0),
      this.sgRender,
      () -> this.render.get() && this.renderShape.get() == RenderShape.CORNER_PYRAMIDS,
      -1.0,
      1.0
   );
   private final Setting<Integer> renderTime = this.setting(
      "render-time", "For how long a shape should be rendered.", Integer.valueOf(10), this.sgRender, this.render::get, 20.0
   );
   private final Setting<Boolean> fade = this.setting(
      "fade", "Will reduce the opacity of the rendered block over time.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<Integer> beforeFadeDelay = this.setting(
      "before-fade-delay",
      "How long to wait before starting to fade. This value has to be smaller than your render time.",
      Integer.valueOf(5),
      this.sgRender,
      () -> this.render.get() && this.fade.get()
   );
   private final Setting<Integer> maxBlocks = this.setting(
      "max-render-blocks",
      "How many blocks to be rendered at once max.",
      Integer.valueOf(5),
      this.sgRender,
      () -> this.render.get() && this.fade.get(),
      1.0,
      10.0,
      1,
      Integer.MAX_VALUE
   );
   private final Setting<SettingColor> sideColor = this.setting(
      "side-color", "The side color.", 255, 0, 0, 75, true, this.sgRender, () -> this.render.get() && ((ShapeMode)this.shapeMode.get()).sides()
   );
   private final Setting<SettingColor> lineColor = this.setting(
      "line-color", "The line color.", 255, 0, 0, 200, true, this.sgRender, () -> this.render.get() && ((ShapeMode)this.shapeMode.get()).lines()
   );
   private final Setting<Boolean> renderDamage = this.setting(
      "render-damage", "Renders the damage of the crystal where it is placing.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<Integer> roundDamage = this.setting(
      "round-damage", "Round damage to x decimal places.", Integer.valueOf(2), this.sgRender, () -> this.render.get() && this.renderDamage.get(), 3.0
   );
   private final Setting<Double> damageScale = this.setting(
      "damage-scale", "The scale of the damage text.", Double.valueOf(1.4), this.sgRender, () -> this.render.get() && this.renderDamage.get(), 5.0
   );
   private final Setting<SettingColor> damageColor = this.setting(
      "damage-color", "The color of the damage text.", 255, 255, 255, this.sgRender, () -> this.render.get() && this.renderDamage.get()
   );
   public int placeDelayLeft;
   public int breakDelayLeft;
   public int retryDelayLeft;
   public int anchorSlot;
   public int glowSlot;
   private FindItemResult anchorResult;
   private Vec3d playerPos;
   private Vec3d eyeHeight;
   private Vec3d breakPos;
   private Vec3d placePos;
   private final List<RenderBlock> renderBlocks = new ArrayList<>();
   public LivingEntity target;
   private final List<PlayerEntity> targets = new ArrayList();
   private final List<PlayerEntity> friends = new ArrayList();
   private final Map<String, Integer> breakAttempts = new Hashtable<>();
   private double bestDamage;
   private float displayDamage = 0.0F;
   private ThreadedUtils.AaCalcs placeStuff;
   private ThreadedUtils.AaCalcs breakStuff;

   public AutoAnchor() {
      super(Venomhack420.CATEGORY, "auto-anchor", "Best aa in da game.");
   }

   public void onActivate() {
      this.placeDelayLeft = 0;
      this.breakDelayLeft = 0;
      this.retryDelayLeft = 0;
      this.renderBlocks.clear();
      this.target = null;
      this.targets.clear();
      this.friends.clear();
      this.breakAttempts.clear();
      this.bestDamage = 0.0;
      this.placeStuff = new ThreadedUtils.AaCalcs(ThreadedUtils.CalcType.Place);
      this.breakStuff = new ThreadedUtils.AaCalcs(ThreadedUtils.CalcType.Break);
   }

   public void onDeactivate() {
      this.target = null;
   }

   @EventHandler(
      priority = 200
   )
   private void onTick(Pre event) {
      if (this.mc.world.getDimension().comp_649()) {
         ChatUtils.error("You are in the Nether... disabling.", new Object[0]);
         this.toggle();
      } else {
         this.playerPos = this.mc.player.getPos();
         if (Modules.get().isActive(Blink.class)) {
            this.playerPos = ((IBlink)Modules.get().get(Blink.class)).getOldPos();
         }

         this.eyeHeight = new Vec3d(0.0, (double)this.mc.player.getEyeHeight(this.mc.player.getPose()), 0.0);
         this.anchorResult = InvUtils.findInHotbar(new Item[]{Items.RESPAWN_ANCHOR});
         this.anchorSlot = this.anchorResult.slot();
         this.glowSlot = InvUtils.findInHotbar(new Item[]{Items.GLOWSTONE}).slot();
         RenderBlock.tick(this.renderBlocks);
         PlayerUtils2.collectTargets(
            this.targets,
            this.friends,
            this.targetRange.get(),
            this.maxTargets.get(),
            false,
            false,
            this.ignoreTerrain.get(),
            (SortPriority)this.sortPrio.get()
         );
         if (this.targets.isEmpty()) {
            this.target = null;
         } else {
            if (!this.threaded.get()) {
               --this.placeDelayLeft;
               --this.breakDelayLeft;
            }

            --this.retryDelayLeft;
            if (this.retryDelayLeft == 0) {
               this.breakAttempts.clear();
            }

            if (this.threaded.get()) {
               if (this.place.get() && this.anchorSlot != -1 && this.glowSlot != -1) {
                  if (PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnDrink.get())) {
                     return;
                  }

                  ThreadedUtils.aaExecutor.submit(this.placeStuff);
               }

               ThreadedUtils.aaExecutor.submit(this.breakStuff);
            } else {
               if (this.placeDelayLeft <= 0 && this.place.get() && this.anchorSlot != -1 && this.glowSlot != -1) {
                  if (PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnDrink.get())) {
                     return;
                  }

                  this.doPlaceThreaded();
               }

               if (this.breakDelayLeft <= 0) {
                  this.doBreak();
               }
            }
         }
      }
   }

   @EventHandler(
      priority = -100
   )
   private void onPostTick(Post event) {
      if (this.target != null && this.target.isDead()) {
         this.target = null;
      }
   }

   public void doPlaceThreaded() {
      this.placePos = null;
      this.bestDamage = 0.0;
      this.displayDamage = 0.0F;
      List<Vec3d> existingAnchors = new ArrayList();
      int reach = (int)Math.ceil(this.range.get());

      for(int x = -reach; x <= reach; ++x) {
         for(double y = Math.max((double)this.mc.world.getBottomY(), this.playerPos.y + this.eyeHeight.y - (double)reach);
            y <= Math.min((double)this.mc.world.getTopY(), this.playerPos.y + this.eyeHeight.y + (double)reach);
            ++y
         ) {
            for(int z = -reach; z <= reach; ++z) {
               Vec3d vec = new Vec3d(
                  (double)(this.mc.player.getBlockPos().getX() + x) + 0.5,
                  y + 0.5,
                  (double)(this.mc.player.getBlockPos().getZ() + z) + 0.5
               );
               BlockPos blockPos = new BlockPos(vec);
               if (!BlockUtils2.invalidPos(blockPos)
                  && !(
                     this.playerPos.add(this.eyeHeight).distanceTo(vec)
                        > UtilsPlus.cantSee(blockPos, this.strictDirections.get()) ? (Double)this.wallsRange.get() : (Double)this.range.get()
                  )
                  && this.isValidBreakBlockstate(this.mc.world.getBlockState(blockPos))) {
                  existingAnchors.add(vec);
               }
            }
         }
      }

      for(int x = -reach; x <= reach; ++x) {
         for(double y = Math.max((double)this.mc.world.getBottomY(), this.playerPos.y + this.eyeHeight.y - (double)reach);
            y <= Math.min((double)this.mc.world.getTopY(), this.playerPos.y + this.eyeHeight.y + (double)reach);
            ++y
         ) {
            for(int z = -reach; z <= reach; ++z) {
               Vec3d vec = new Vec3d(
                  (double)(this.mc.player.getBlockPos().getX() + x) + 0.5,
                  y + 0.5,
                  (double)(this.mc.player.getBlockPos().getZ() + z) + 0.5
               );
               BlockPos blockPos = new BlockPos(vec);
               if (!BlockUtils2.invalidPos(blockPos)
                  && !(
                     this.playerPos.add(this.eyeHeight).distanceTo(vec)
                        > UtilsPlus.cantSee(blockPos, this.strictDirections.get()) ? (Double)this.wallsRange.get() : (Double)this.range.get()
                  )
                  && this.isValidPlaceBlockState(this.mc.world.getBlockState(blockPos), blockPos)
                  && this.isEmpty(blockPos)) {
                  boolean unsafeForFriends = false;
                  if (this.considerFriends.get()) {
                     for(LivingEntity friend : this.friends) {
                        Vec3d v = this.predict.get()
                           ? UtilsPlus.smartPredictedPosition(friend, UtilsPlus.smartVelocity(friend))
                           : new Vec3d(0.0, 0.0, 0.0);
                        Vec3d friendPos = friend.getPos().add(v);
                        if (!(vec.distanceTo(friendPos) > 7.0) && (!this.smartDelay.get() || friend.hurtTime <= this.threshold.get())) {
                           double friendDmg = (double)DamageCalcUtils.explosionDamage(
                              friend, vec, this.predict.get(), 1, 1, this.ignoreTerrain.get(), false, true, 5
                           );
                           if (friendDmg > this.maxPlaceSelfDamage.get()) {
                              unsafeForFriends = true;
                           }
                        }
                     }
                  }

                  if (!unsafeForFriends) {
                     for(LivingEntity target : this.targets) {
                        Vec3d v = this.predict.get()
                           ? UtilsPlus.smartPredictedPosition(target, UtilsPlus.smartVelocity(target))
                           : new Vec3d(0.0, 0.0, 0.0);
                        Vec3d targetPos = target.getPos().add(v);
                        if (!(vec.distanceTo(targetPos) > 7.0) && (!this.smartDelay.get() || target.hurtTime <= this.threshold.get())) {
                           double selfDmg = this.placeMode.get() != Safety.Suicide && this.breakMode.get() != Safety.Suicide
                              ? (double)DamageCalcUtils.explosionDamage(this.mc.player, vec, false, 1, 1, this.ignoreTerrain.get(), false, true, 5)
                              : 0.0;
                           if (!(selfDmg > this.maxPlaceSelfDamage.get()) && !((double)EntityUtils.getTotalHealth(this.mc.player) - selfDmg <= 0.5)) {
                              float damage = DamageCalcUtils.explosionDamage(target, vec, this.predict.get(), 1, 1, this.ignoreTerrain.get(), false, true, 5);
                              if (!((double)damage < this.minPlaceDamage.get())) {
                                 boolean stop = false;

                                 for(Vec3d existingAnchor : existingAnchors) {
                                    if (this.playerPos.add(this.eyeHeight).distanceTo(existingAnchor)
                                          <= UtilsPlus.cantSee(new BlockPos(existingAnchor), this.strictDirections.get())
                                             ? (Double)this.wallsRange.get()
                                             : (Double)this.range.get()
                                       && !(targetPos.distanceTo(existingAnchor) > 7.0)) {
                                       double multiDamage = (double)DamageCalcUtils.explosionDamage(
                                          target, vec, this.predict.get(), 1, 1, this.ignoreTerrain.get(), false, true, 5
                                       );
                                       if (!(multiDamage < this.minBreakDamage.get())) {
                                          double multiSelfDmg = this.placeMode.get() != Safety.Suicide && this.breakMode.get() != Safety.Suicide
                                             ? (double)DamageCalcUtils.explosionDamage(
                                                this.mc.player, vec, false, 1, 1, this.ignoreTerrain.get(), false, true, 5
                                             )
                                             : 0.0;
                                          if (!(multiSelfDmg > this.maxBreakSelfDamage.get())) {
                                             stop = true;
                                          }
                                       }
                                    }
                                 }

                                 if (!stop && (double)damage > this.bestDamage) {
                                    this.placePos = vec;
                                    this.bestDamage = (double)damage;
                                    this.displayDamage = damage;
                                    this.target = target;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.placePos != null) {
         this.placeAnchor(new BlockPos(this.placePos));
         this.resetDelays(this.target);
      }
   }

   private void placeAnchor(BlockPos blockPos) {
      RenderBlock.addRenderBlock(this.renderBlocks, blockPos, this.renderTime.get(), this.fade.get() ? this.maxBlocks.get() : 1, this.displayDamage);
      FindItemResult anchorResult = InvUtils.findInHotbar(new Item[]{Items.RESPAWN_ANCHOR});
      this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.RELEASE_SHIFT_KEY));
      BlockUtils2.placeBlock(
         anchorResult, blockPos, ((Type)this.rotationMode.get()).placeTrue(), 50, this.airPlace.get(), false, this.swing.get(), this.strictDirections.get()
      );
   }

   public void doBreak() {
      this.breakPos = null;
      this.bestDamage = 0.0;

      for(int x = (int)(-this.range.get()); (double)x <= this.range.get(); ++x) {
         for(int y = (int)(-this.range.get()); (double)y <= this.range.get(); ++y) {
            for(int z = (int)(-this.range.get()); (double)z <= this.range.get(); ++z) {
               Vec3d vec = new Vec3d(
                  (double)(this.mc.player.getBlockPos().getX() + x) + 0.5,
                  (double)(this.mc.player.getBlockPos().getY() + y) + 0.5,
                  (double)(this.mc.player.getBlockPos().getZ() + z) + 0.5
               );
               BlockPos blockPos = new BlockPos(vec);
               if (!(this.playerPos.add(this.eyeHeight).distanceTo(vec) > this.range.get())
                  && this.isValidBreakBlockstate(this.mc.world.getBlockState(blockPos))) {
                  boolean unsafeForFriends = false;
                  if (this.considerFriends.get()) {
                     for(LivingEntity friend : this.friends) {
                        Vec3d v = this.predict.get()
                           ? UtilsPlus.smartPredictedPosition(friend, UtilsPlus.smartVelocity(friend))
                           : new Vec3d(0.0, 0.0, 0.0);
                        Vec3d friendPos = friend.getPos().add(v);
                        if (!(vec.distanceTo(friendPos) > 7.0) && (!this.smartDelay.get() || friend.hurtTime <= this.threshold.get())) {
                           double friendDmg = (double)DamageCalcUtils.explosionDamage(
                              friend, vec, this.predict.get(), 1, 1, this.ignoreTerrain.get(), false, true, 5
                           );
                           if (friendDmg > this.maxBreakSelfDamage.get()) {
                              unsafeForFriends = true;
                           }
                        }
                     }
                  }

                  if (!unsafeForFriends) {
                     for(LivingEntity target : this.targets) {
                        Vec3d v = this.predict.get()
                           ? UtilsPlus.smartPredictedPosition(target, UtilsPlus.smartVelocity(target))
                           : new Vec3d(0.0, 0.0, 0.0);
                        Vec3d targetPos = target.getPos().add(v);
                        if (!(vec.distanceTo(targetPos) > 7.0) && (!this.smartDelay.get() || target.hurtTime <= this.threshold.get())) {
                           double selfDmg = this.placeMode.get() != Safety.Suicide && this.breakMode.get() != Safety.Suicide
                              ? (double)DamageCalcUtils.explosionDamage(this.mc.player, vec, false, 1, 1, this.ignoreTerrain.get(), false, true, 5)
                              : 0.0;
                           if (!(selfDmg > this.maxBreakSelfDamage.get()) && !((double)EntityUtils.getTotalHealth(this.mc.player) - selfDmg <= 0.5)) {
                              float damage = DamageCalcUtils.explosionDamage(target, vec, this.predict.get(), 1, 1, this.ignoreTerrain.get(), false, true, 5);
                              if (!((double)damage < this.minBreakDamage.get()) && (double)damage > this.bestDamage) {
                                 this.breakPos = vec;
                                 this.bestDamage = (double)damage;
                                 this.displayDamage = damage;
                                 this.target = target;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.breakPos != null) {
         if (!this.breakAttempts.containsKey(this.uniqueVecId(this.breakPos))) {
            this.breakAttempts.put(this.uniqueVecId(this.breakPos), 0);
         }

         this.breakAttempts.put(this.uniqueVecId(this.breakPos), this.breakAttempts.get(this.uniqueVecId(this.breakPos)) + 1);
         if (this.breakAttempts.get(this.uniqueVecId(this.breakPos)) > 1) {
            if (this.retryDelayLeft < 0) {
               this.retryDelayLeft = this.retryDelay.get();
            }

            return;
         }

         if (((Type)this.rotationMode.get()).breakTrue()) {
            Rotations.rotate(
               Rotations.getYaw(this.breakPos),
               Rotations.getPitch(this.breakPos),
               50,
               () -> this.breakAnchor(new BlockPos(this.breakPos), this.glowSlot, this.anchorSlot)
            );
         } else {
            this.breakAnchor(new BlockPos(this.breakPos), this.glowSlot, this.anchorSlot);
         }
      }
   }

   private void breakAnchor(BlockPos pos, int glowSlot, int nonGlowSlot) {
      if (!this.place.get()) {
         RenderBlock.addRenderBlock(this.renderBlocks, pos, this.renderTime.get(), this.fade.get() ? this.maxBlocks.get() : 1, this.displayDamage);
      }

      if (pos != null && this.mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
         if (glowSlot != -1 && nonGlowSlot != -1) {
            this.mc.player.setSneaking(false);
            this.mc.options.sneakKey.setPressed(false);
            this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.RELEASE_SHIFT_KEY));
            int preSlot = this.mc.player.getInventory().selectedSlot;
            InvUtils.swap(glowSlot, false);
            if (this.packetBreak.get()) {
               this.mc
                  .getNetworkHandler()
                  .sendPacket(
                     new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(
                           new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
                           Direction.UP,
                           pos,
                           true
                        ),
                        0
                     )
                  );
               RandUtils.swing(this.swing.get());
            } else {
               this.mc
                  .interactionManager
                  .interactBlock(
                     this.mc.player,
                     Hand.MAIN_HAND,
                     new BlockHitResult(
                        new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
                        Direction.UP,
                        pos,
                        true
                     )
                  );
            }

            InvUtils.swap(nonGlowSlot, false);
            if (this.packetBreak.get()) {
               this.mc
                  .getNetworkHandler()
                  .sendPacket(
                     new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(
                           new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
                           Direction.UP,
                           pos,
                           true
                        ),
                        0
                     )
                  );
               RandUtils.swing(this.swing.get());
            } else {
               this.mc
                  .interactionManager
                  .interactBlock(
                     this.mc.player,
                     Hand.MAIN_HAND,
                     new BlockHitResult(
                        new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
                        Direction.UP,
                        pos,
                        true
                     )
                  );
            }

            InvUtils.swap(preSlot, false);
         }

         this.resetDelays(this.target);
      }
   }

   private boolean isValidPlaceBlockState(BlockState blockState, BlockPos pos) {
      return blockState.getMaterial().isReplaceable() && (this.airPlace.get() || BlockUtils.getPlaceSide(pos) != null);
   }

   private boolean isValidBreakBlockstate(BlockState blockState) {
      return blockState.isOf(Blocks.RESPAWN_ANCHOR);
   }

   private void resetDelays(LivingEntity targ) {
      if (this.threaded.get()) {
         ThreadedUtils.AaCalcs.resetDelays(targ);
      } else if (UtilsPlus.isSurrounded(targ, true, true) && this.holeDelays.get()) {
         this.placeDelayLeft = this.holePlaceDelay.get();
         this.breakDelayLeft = this.holeBreakDelay.get();
      } else {
         this.placeDelayLeft = this.placeDelay.get();
         this.breakDelayLeft = this.breakDelay.get();
      }
   }

   private String uniqueVecId(Vec3d vec) {
      return String.valueOf(vec.x).concat(";").concat(String.valueOf(vec.y)).concat(";").concat(String.valueOf(vec.z));
   }

   private boolean isEmpty(BlockPos pos) {
      return this.mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent());
   }

   @EventHandler
   public void onExplosionSound(SoundInstanceEvent event) {
      if (event.weightedSoundSet.getSubtitle() != null) {
         if (this.breakPos != null) {
            if (event.weightedSoundSet.getSubtitle().getString().equals("Explosion")
               && this.breakPos.equals(new Vec3d(event.soundInstance.getX(), event.soundInstance.getY(), event.soundInstance.getZ()))
               )
             {
               List<String> toRemove = new ArrayList<>();
               this.breakAttempts
                  .forEach(
                     (string, integer) -> {
                        Vec3d vec = new Vec3d(
                           Double.parseDouble(string.split(";")[0]), Double.parseDouble(string.split(";")[1]), Double.parseDouble(string.split(";")[2])
                        );
                        if (vec.equals(new Vec3d(event.soundInstance.getX(), event.soundInstance.getY(), event.soundInstance.getZ()))
                           )
                         {
                           toRemove.add(this.uniqueVecId(vec));
                        }
                     }
                  );
               toRemove.forEach(this.breakAttempts::remove);
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty() && this.render.get()) {
         this.renderBlocks
            .forEach(
               renderBlock -> renderBlock.complexRender(
                     event,
                     (SettingColor)this.sideColor.get(),
                     (SettingColor)this.lineColor.get(),
                     (ShapeMode)this.shapeMode.get(),
                     this.fade.get(),
                     this.beforeFadeDelay.get(),
                     false,
                     (RenderShape)this.renderShape.get(),
                     this.width.get(),
                     this.height.get(),
                     this.yOffset.get(),
                     this.weirdOffset.get()
                  )
            );
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (!this.renderBlocks.isEmpty() && this.render.get() && this.renderDamage.get()) {
         int preA = ((SettingColor)this.damageColor.get()).a;
         synchronized(this.renderBlocks) {
            this.renderBlocks
               .forEach(
                  damage -> {
                     Vec3 pos = new Vec3(
                        (double)damage.pos.getX() + 0.5,
                        (double)damage.pos.getY() + this.yOffset.get() + this.height.get() * 0.5,
                        (double)damage.pos.getZ() + 0.5
                     );
                     if (NametagUtils.to2D(pos, this.damageScale.get())) {
                        NametagUtils.begin(pos);
                        TextRenderer.get().begin(1.0, false, true);
                        String damageText = TextUtils.round(damage.damage, this.roundDamage.get()) + "";
                        if (this.fade.get()) {
                           SettingColor var10000 = (SettingColor)this.damageColor.get();
                           var10000.a = (int)(
                              (float)var10000.a
                                 * (
                                    damage.ticks > (float)(this.renderTime.get() - this.beforeFadeDelay.get())
                                       ? 1.0F
                                       : damage.ticks / (float)((Integer)this.renderTime.get()).intValue()
                                 )
                           );
                        }
      
                        TextRenderer.get().render(damageText, TextRenderer.get().getWidth(damageText) * -0.5, 0.0, (Color)this.damageColor.get(), true);
                        TextRenderer.get().end();
                        NametagUtils.end();
                        ((SettingColor)this.damageColor.get()).a = preA;
                     }
                  }
               );
         }
      }
   }

   @Override
   public PlayerEntity getTarget() {
      if (this.target == null) {
         return null;
      } else {
         return this.target instanceof PlayerEntity ? (PlayerEntity)this.target : null;
      }
   }

   public String getInfoString() {
      if (this.target == null) {
         return null;
      } else {
         return this.target instanceof PlayerEntity ? this.target.getEntityName() : null;
      }
   }
}
