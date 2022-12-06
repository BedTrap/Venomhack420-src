package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.GameMode;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import venomhack.Venomhack420;
import venomhack.enums.Mode;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.CityUtils;
import venomhack.utils.RandUtils;
import venomhack.utils.UtilsPlus;

public class AutoCity extends ModuleHelper {
   private final SettingGroup sgBehaviour = this.group("Behaviour");
   private final SettingGroup sgSwitch = this.group("Switch");
   private final SettingGroup sgInstant = this.group("Instant");
   private final SettingGroup sgRender = this.group("Render");
   public final Setting<Double> targetRange = this.setting(
      "target-range", "The radius in which players get targeted.", Double.valueOf(6.0), this.sgGeneral, 0.0, 5.0
   );
   public final Setting<Double> range = this.setting(
      "range", "The radius in which blocks are allowed to get broken.", Double.valueOf(5.0), this.sgGeneral, 0.0, 6.0
   );
   private final Setting<Boolean> selfToggle = this.setting(
      "self-toggle", "Automatically toggles off after activation.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<Boolean> chatInfo = this.setting(
      "chat-info", "Sends a client-side message when you start to city a player.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<SortPriority> sortPrio = this.setting("priority", "What target to prioritize.", SortPriority.LowestDistance, this.sgGeneral);
   private final Setting<Boolean> pauseOnEat = this.setting("pause-on-eat", "Pauses while eating.", Boolean.valueOf(true), this.sgGeneral);
   private final Setting<Mode> mode = this.setting("mining-mode", "How to break blocks.", Mode.PACKET, this.sgBehaviour);
   private final Setting<Boolean> burrow = this.setting(
      "mine-burrow", "Whether to mine a targets burrow block or not.", Boolean.valueOf(true), this.sgBehaviour
   );
   private final Setting<Boolean> ignoreWebs = this.setting(
      "ignore-self-webs", "Will not try to mine webs the target is standing in.", Boolean.valueOf(true), this.sgBehaviour, this.burrow::get
   );
   private final Setting<Boolean> support = this.setting(
      "support", "If there is no block below a city block it will place obsidian there before mining.", Boolean.valueOf(true), this.sgBehaviour
   );
   private final Setting<Boolean> strictDirections = this.setting(
      "strict-directions", "Places only on visible sides.", Boolean.valueOf(false), this.sgBehaviour, this.support::get
   );
   private final Setting<Boolean> rotate = this.setting("rotate", "Automatically rotates you towards the city block.", Boolean.valueOf(true), this.sgBehaviour);
   public final Setting<Boolean> old = this.setting(
      "1.12-mode", "Requires an air block above in order to target it.", Boolean.valueOf(false), this.sgBehaviour
   );
   private final Setting<Boolean> delayed = this.setting(
      "delayed-switch",
      "Will only switch to the pickaxe when the block is ready to be broken.",
      Boolean.valueOf(true),
      this.sgSwitch,
      () -> this.mode.get() != Mode.BYPASS
   );
   private final Setting<Boolean> manual = this.setting(
      "manual-switch", "Will not switch automatically.", Boolean.valueOf(false), this.sgSwitch, () -> this.delayed.get() && this.mode.get() != Mode.BYPASS
   );
   private final Setting<Integer> delay = this.setting(
      "delay", "Delay between mining blocks in ticks.", Integer.valueOf(0), this.sgInstant, () -> this.mode.get() == Mode.INSTANT, 0.0, 10.0
   );
   private final Setting<Integer> toggle = this.setting(
      "auto-toggle",
      "Amount of ticks the block has to be air to auto toggle off.",
      Integer.valueOf(60),
      this.sgInstant,
      () -> this.mode.get() == Mode.INSTANT,
      0.0,
      60.0
   );
   private final Setting<Boolean> swing = this.setting("swing", "Renders your swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Renders the block you are mining.", Boolean.valueOf(true), this.sgRender);
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the shapes are rendered.", ShapeMode.Sides, this.sgRender, this.render::get);
   private final Setting<SettingColor> sideColor = this.setting("side-color", "The side color.", 255, 0, 0, 75, true, this.sgRender, this.render::get);
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color.", 255, 0, 0, 200, this.sgRender, this.render::get);
   private final Setting<Boolean> renderProgress = this.setting("render-progress", "Renders the block break progress.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Double> progressScale = this.setting(
      "progress-scale", "The scale of the progress text.", Double.valueOf(1.4), this.sgRender, this.renderProgress::get, 0.0, 5.0
   );
   private final Setting<SettingColor> progressColor = this.setting(
      "progress-color", "The color of the progress text.", 255, 255, 255, 255, this.sgRender, this.renderProgress::get
   );
   private PlayerEntity target;
   private BlockPos blockPosTarget;
   private BlockPos targetPos;
   private Direction direction;
   private int timer;
   private int max;
   private int instaCount;
   private FindItemResult result;
   private boolean firstTime;

   public AutoCity() {
      super(Venomhack420.CATEGORY, "auto-city-vh", "Automatically cities a target by mining the nearest block next to them.");
   }

   @EventHandler
   public void onActivate() {
      this.onFirstTime();
   }

   @EventHandler
   private void onTick(Pre event) {
      ++this.timer;
      if (this.result.found()) {
         this.max = CityUtils.getBlockBreakingSpeed(this.mc.world.getBlockState(this.blockPosTarget), this.blockPosTarget, this.result.slot());
         if (!this.pauseOnEat.get()
            || !this.mc.player.isUsingItem()
            || !this.mc.player.getMainHandStack().isFood() && !this.mc.player.getOffHandStack().isFood()) {
            int currentSlot = this.mc.player.getInventory().selectedSlot;
            boolean isAir = this.mc.world.isAir(this.blockPosTarget);
            if (this.mode.get() == Mode.BYPASS
               || !this.delayed.get()
               || !this.manual.get()
                  && this.timer >= this.max
                  && (this.mode.get() != Mode.INSTANT || !isAir && (this.delay.get() == 0 || this.timer % this.delay.get() == 0))) {
               this.mc.player.getInventory().selectedSlot = this.result.slot();
            }

            if (this.mode.get() == Mode.BYPASS) {
               if (this.rotate.get()) {
                  Rotations.rotate(
                     Rotations.getYaw(this.blockPosTarget),
                     Rotations.getPitch(this.blockPosTarget),
                     () -> this.mc.interactionManager.updateBlockBreakingProgress(this.blockPosTarget, this.direction)
                  );
               } else {
                  this.mc.interactionManager.updateBlockBreakingProgress(this.blockPosTarget, this.direction);
               }

               RandUtils.swing(this.swing.get());
            } else {
               if (this.firstTime) {
                  UtilsPlus.mine(this.blockPosTarget, this.swing.get(), this.rotate.get());
                  this.firstTime = false;
               }

               if (this.mode.get() == Mode.INSTANT) {
                  if (isAir) {
                     ++this.instaCount;
                  } else {
                     this.instaCount = 0;
                     if (this.timer >= this.max && (this.delay.get() == 0 || this.timer % this.delay.get() == 0) && currentSlot == this.result.slot()) {
                        if (this.rotate.get()) {
                           Rotations.rotate(
                              Rotations.getYaw(this.blockPosTarget),
                              Rotations.getPitch(this.blockPosTarget),
                              () -> this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, this.blockPosTarget, this.direction))
                           );
                        } else {
                           this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, this.blockPosTarget, this.direction));
                        }

                        RandUtils.swing(this.swing.get());
                     }
                  }
               }
            }

            if (this.timer >= this.max
               && (
                  this.target == null
                     || this.blockPosTarget == null
                     || !this.target.isAlive()
                     || (this.mode.get() != Mode.INSTANT || this.mode.get() == Mode.BYPASS) && (currentSlot == this.result.slot() || isAir)
                     || this.instaCount >= this.toggle.get()
                     || !this.blockPosTarget.isWithinDistance(this.mc.player.getPos(), this.range.get())
                     || this.target.getBlockPos() != this.targetPos
               )) {
               if (this.selfToggle.get()) {
                  this.toggle();
               } else {
                  this.onFirstTime();
               }
            }
         }
      }
   }

   private PlayerEntity findTarget() {
      List<PlayerEntity> targets = new ArrayList();

      for(Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof OtherClientPlayerEntity target
            && !target.equals(this.mc.player)
            && !((double)this.mc.player.distanceTo(target) > this.targetRange.get())
            && target.deathTime <= 0
            && target.isAlive()
            && Friends.get().shouldAttack(target)
            && (EntityUtils.getGameMode(target) != GameMode.CREATIVE || target instanceof FakePlayerEntity)) {
            boolean noBlock = true;
            BlockPos targetBlockPos = target.getBlockPos();

            for(Vec3i city : CityUtils.CITY_WITHOUT_BURROW) {
               BlockState state = this.mc.world.getBlockState(targetBlockPos.add(city));
               if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                  noBlock = false;
               }
            }

            if (!noBlock || this.burrow.get() && !this.mc.world.getBlockState(targetBlockPos).isAir()) {
               targets.add(target);
            }
         }
      }

      if (targets.isEmpty()) {
         return null;
      } else {
         targets.sort((e1, e2) -> UtilsPlus.sort(e1, e2, (SortPriority)this.sortPrio.get()));
         return (PlayerEntity)targets.get(0);
      }
   }

   private void onFirstTime() {
      this.target = this.findTarget();
      if (this.target == null) {
         if (this.chatInfo.get()) {
            this.error(Text.translatable("autocity.target"));
         }

         this.toggle();
      } else {
         this.blockPosTarget = CityUtils.getCity(this.target, this.range.get(), this.burrow.get(), this.ignoreWebs.get());
         if (this.blockPosTarget == null) {
            if (this.chatInfo.get()) {
               this.error(Text.translatable("autocity.block"));
            }

            this.toggle();
         } else if (PlayerUtils.distanceTo(this.blockPosTarget) > this.range.get()) {
            if (this.chatInfo.get()) {
               this.error(Text.translatable("autocity.reach"));
            }

            this.toggle();
         } else {
            this.result = InvUtils.findFastestTool(this.mc.world.getBlockState(this.blockPosTarget));
            if (this.result.slot() == -1 && !this.mc.player.getAbilities().creativeMode) {
               if (this.chatInfo.get()) {
                  this.error(Text.translatable("autocity.tool"));
               }

               this.toggle();
            } else {
               this.targetPos = this.target.getBlockPos();
               if (this.chatInfo.get()) {
                  this.info(Text.translatable("autocity.attempt").append(this.target.getEntityName()));
               }

               if (this.support.get()) {
                  FindItemResult obbyResult = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
                  if (obbyResult.found()) {
                     BlockPos blockPos = this.blockPosTarget.down();
                     if (this.mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN
                        && this.mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK
                        && !BlockUtils2.placeBlock(obbyResult, blockPos, this.rotate.get(), 30, true, false, this.swing.get(), this.strictDirections.get())
                        && this.chatInfo.get()) {
                        this.warning(Text.translatable("autocity.support"));
                     }
                  } else if (this.chatInfo.get()) {
                     this.warning(Text.translatable("autocity.obsidian"));
                  }
               }

               this.firstTime = true;
               this.timer = 0;
               this.instaCount = 0;
               this.direction = UtilsPlus.rayTraceCheck(this.blockPosTarget, true);
            }
         }
      }
   }

   @EventHandler
   private void onStartBreakingBlock(StartBreakingBlockEvent event) {
      if (this.firstTime) {
         this.timer = 0;
      }

      this.firstTime = false;
   }

   @EventHandler
   private void AttackEntityEvent(AttackEntityEvent event) {
      if (this.firstTime) {
         this.timer = 0;
      }

      this.firstTime = false;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.blockPosTarget != null && this.render.get() && this.target != null && !this.mc.player.getAbilities().creativeMode) {
         event.renderer.box(this.blockPosTarget, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.blockPosTarget != null && this.renderProgress.get() && this.target != null && !this.mc.player.getAbilities().creativeMode) {
         Vec3 pos = new Vec3(
            (double)this.blockPosTarget.getX() + 0.5,
            (double)this.blockPosTarget.getY() + 0.5,
            (double)this.blockPosTarget.getZ() + 0.5
         );
         if (NametagUtils.to2D(pos, this.progressScale.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            String progress = Math.min(100L, Math.round((double)this.timer / (double)this.max * 100.0)) + "%";
            TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, (Color)this.progressColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
         }
      }
   }

   public String getInfoString() {
      return this.target != null ? this.target.getEntityName() : null;
   }
}
