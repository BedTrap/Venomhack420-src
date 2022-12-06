package venomhack.modules.chat;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import venomhack.Venomhack420;
import venomhack.enums.JoinLeaveEvent;
import venomhack.enums.RenderShape;
import venomhack.gui.screens.GuideScreen;
import venomhack.modules.ModuleHelper;
import venomhack.modules.combat.Surround;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.RenderBlock;

public class Notifier extends ModuleHelper {
   private final SettingGroup sgVisualRange = this.group("Visual Range");
   private final SettingGroup sgBurrow = this.group("Burrow Detector");
   private final SettingGroup sgEffects = this.group("Effects");
   private final Setting<Boolean> visualRange = this.setting(
      "visual-range", "Notifies you when an entity enters your render distance.", Boolean.valueOf(false), this.sgGeneral
   );
   private final Setting<Boolean> burrowNotify = this.setting(
      "notify-burrow", "Notifies you when someone tries to city you.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<Boolean> surroundNotify = this.setting(
      "notify-surround-break", "Notifies you when someone tries to city you.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<Boolean> surroundToggle = this.setting(
      "toggle-surround", "Turns on surround when someone tries to city you.", Boolean.valueOf(false), this.sgGeneral, this.surroundNotify::get
   );
   private final Setting<Boolean> effectNotifier = this.setting(
      "effect-notifier", "Notifies you about certain effects.", Boolean.valueOf(false), this.sgGeneral
   );
   private final Setting<Boolean> playSound = this.setting(
      "play-sound", "Plays a sound when a player enters your render distance.", Boolean.valueOf(false), this.sgVisualRange, this.visualRange::get
   );
   private final Setting<JoinLeaveEvent> event = this.setting(
      "event", "When to log the entities.", JoinLeaveEvent.BOTH, this.sgVisualRange, this.visualRange::get
   );
   private final Setting<Object2BooleanMap<EntityType<?>>> entities = this.setting(
      "entities", "Which entities to nofity about.", this.sgVisualRange, true, this.visualRange::get, null, null, new EntityType[]{EntityType.PLAYER}
   );
   private final Setting<Boolean> visualRangeIgnoreFriends = this.setting(
      "ignore-friends", "Ignores friends.", Boolean.valueOf(true), this.sgVisualRange, this.visualRange::get
   );
   private final Setting<Boolean> burrowFriends = this.setting(
      "friends", "Whether to notify when a friend gets an effect.", Boolean.valueOf(false), this.sgBurrow, this.burrowNotify::get
   );
   private final Setting<Boolean> renderBurrow = this.setting(
      "render-burrow", "Renders an overlay when someone burrows.", Boolean.valueOf(true), this.sgBurrow, this.burrowNotify::get
   );
   private final Setting<RenderShape> renderShape = this.setting(
      "render-shape", "What shape mode to use.", RenderShape.CUBOID, this.sgBurrow, () -> this.renderBurrow.get() && this.burrowNotify.get()
   );
   private final Setting<ShapeMode> shapeMode = this.setting(
      "shape-mode", "What part of the shapes to render.", ShapeMode.Both, this.sgBurrow, () -> this.renderBurrow.get() && this.burrowNotify.get()
   );
   private final Setting<Double> height = this.setting(
      "height", "The height of the render.", Double.valueOf(1.0), this.sgBurrow, () -> this.renderBurrow.get() && this.burrowNotify.get(), 0.0, 2.0
   );
   private final Setting<Double> width = this.setting(
      "width", "The width of the render.", Double.valueOf(1.0), this.sgBurrow, () -> this.renderBurrow.get() && this.burrowNotify.get(), 0.0, 2.0
   );
   private final Setting<Double> yOffset = this.setting(
      "y-offset",
      "The height shift for the rendered shape.",
      Double.valueOf(0.0),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get(),
      0.0,
      1.0
   );
   private final Setting<Double> weirdOffset = this.setting(
      "weird-offset",
      "Offset for the pyramid render shape.",
      Double.valueOf(0.0),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get() && this.renderShape.get() == RenderShape.CORNER_PYRAMIDS,
      -1.0,
      1.0
   );
   private final Setting<Integer> renderTime = this.setting(
      "render-time",
      "For how long a shape should be rendered.",
      Integer.valueOf(20),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get(),
      0.0,
      40.0
   );
   private final Setting<Boolean> fade = this.setting(
      "fade",
      "Will reduce the opacity of the rendered block over time.",
      Boolean.valueOf(true),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get()
   );
   private final Setting<SettingColor> sideColor = this.setting(
      "side-color",
      "The side color of the target block rendering.",
      new SettingColor(197, 137, 232, 10),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get()
   );
   private final Setting<SettingColor> lineColor = this.setting(
      "line-color",
      "The line color of the target block rendering.",
      new SettingColor(197, 137, 232),
      this.sgBurrow,
      () -> this.renderBurrow.get() && this.burrowNotify.get()
   );
   private final Setting<Boolean> strength = this.setting(
      "strength", "Notifies you when someone gets the strength effect.", Boolean.valueOf(true), this.sgEffects, this.effectNotifier::get
   );
   private final Setting<Boolean> weakness = this.setting(
      "weakness", "Notifies you when someone gets the weakness effect.", Boolean.valueOf(true), this.sgEffects, this.effectNotifier::get
   );
   private final Setting<Boolean> turtle = this.setting(
      "turtle-master", "Notifies you when someone gets the turtle master effects.", Boolean.valueOf(true), this.sgEffects, this.effectNotifier::get
   );
   private final Setting<Boolean> egap = this.setting(
      "egap", "Notifies you when someone eats an enchanted golden apple.", Boolean.valueOf(true), this.sgEffects, this.effectNotifier::get
   );
   private final Setting<Boolean> friendsEffects = this.setting(
      "friends", "Whether to notify when a friend gets an effect.", Boolean.valueOf(true), this.sgEffects, this.effectNotifier::get
   );
   private final List<PlayerEntity> strengthPlayers = new ArrayList();
   private final List<PlayerEntity> weaknessPlayers = new ArrayList();
   private final List<PlayerEntity> turtlePlayers = new ArrayList();
   private final List<PlayerEntity> burrowedPlayers = new ArrayList();
   private final List<RenderBlock> renderBlocks = new ArrayList<>();
   private final Int2IntMap egapPlayers = new Int2IntOpenHashMap();
   private PlayerEntity prevBreakingPlayer;

   public Notifier() {
      super(Venomhack420.CATEGORY, "notifier-vh", "Notifies you of different events.");
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      Entity entity = event.entity;
      if (this.visualRange.get()
         && ((JoinLeaveEvent)this.event.get()).join()
         && ((Object2BooleanMap)this.entities.get()).getBoolean(event.entity.getType())
         && !entity.equals(this.mc.player)) {
         if (event.entity instanceof PlayerEntity) {
            if ((!this.visualRangeIgnoreFriends.get() || !Friends.get().isFriend((PlayerEntity)event.entity)) && !(event.entity instanceof FakePlayerEntity)) {
               ChatUtils.sendMsg(
                  event.entity.getId() + 100,
                  this.title(),
                  Formatting.LIGHT_PURPLE,
                  Text.literal(event.entity.getEntityName())
                     .append(Text.translatable("notifier.spawn").formatted(Formatting.RED))
               );
               if (this.playSound.get()) {
                  this.mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0F, 1.0F);
               }
            }
         } else {
            MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
            text.append(Text.literal(" has spawned at ").formatted(Formatting.RED));
            text.append(ChatUtils.formatCoords(event.entity.getPos()));
            text.append(Text.literal(".").formatted(Formatting.RED));
            this.info(text);
         }
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      Entity entity = event.entity;
      if (this.visualRange.get()
         && ((JoinLeaveEvent)this.event.get()).leave()
         && !entity.equals(this.mc.player)
         && ((Object2BooleanMap)this.entities.get()).getBoolean(event.entity.getType())) {
         if (event.entity instanceof PlayerEntity) {
            if ((!this.visualRangeIgnoreFriends.get() || !Friends.get().isFriend((PlayerEntity)event.entity)) && !(event.entity instanceof FakePlayerEntity)) {
               ChatUtils.sendMsg(
                  event.entity.getId() + 100,
                  this.title(),
                  Formatting.LIGHT_PURPLE,
                  Text.literal(event.entity.getEntityName())
                     .append(Text.translatable("notifier.despawn").formatted(Formatting.DARK_GREEN))
               );
            }
         } else {
            MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
            text.append(Text.literal(" has despawned at ").formatted(Formatting.DARK_GREEN));
            text.append(ChatUtils.formatCoords(event.entity.getPos()));
            text.append(Text.literal(".").formatted(Formatting.DARK_GREEN));
            this.info(text);
         }
      }
   }

   @EventHandler
   public void onBreakPacket(Receive event) {
      Packet breakingPlayer = event.packet;
      if (breakingPlayer instanceof BlockBreakingProgressS2CPacket packet) {
         if (this.surroundNotify.get()) {
            PlayerEntity breakingPlayerx = (PlayerEntity)this.mc.world.getEntityById(packet.getEntityId());
            if (breakingPlayerx != null && !breakingPlayerx.equals(this.mc.player)) {
               if (packet.getProgress() <= 0 && !breakingPlayerx.equals(this.prevBreakingPlayer)) {
                  BlockPos playerBlockPos = this.mc.player.getBlockPos();

                  for(Direction dir : Direction.values()) {
                     if (packet.getPos().equals(playerBlockPos.offset(dir))) {
                        ChatUtils.warning("Your " + dir.getName() + " surround block is being broken by " + breakingPlayerx.getEntityName(), new Object[0]);
                        if (this.surroundToggle.get() && !Modules.get().isActive(Surround.class)) {
                           this.info("Enabling surround...", new Object[0]);
                           ((Surround)Modules.get().get(Surround.class)).toggle();
                        }

                        this.prevBreakingPlayer = breakingPlayerx;
                        return;
                     }
                  }

                  if (packet.getPos().equals(this.mc.player.getBlockPos())) {
                     ChatUtils.warning("Your burrow is being mined by " + breakingPlayerx.getEntityName(), new Object[0]);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      RenderBlock.tick(this.renderBlocks);

      for(Entity p : this.mc.world.getEntities()) {
         if (p instanceof PlayerEntity player && player != this.mc.player) {
            if (this.burrowNotify.get() && (this.burrowFriends.get() || Friends.get().shouldAttack(player))) {
               if (UtilsPlus.isBurrowed(player)) {
                  if (!this.burrowedPlayers.contains(player)) {
                     this.burrowedPlayers.add(player);
                     BlockState state = this.mc.world.getBlockState(player.getBlockPos());
                     if (state.getBlock().collidable) {
                        if (!this.renderBurrow.get()) {
                           this.info(player.getEntityName() + " just burrowed in " + state.getBlock().getName().getString() + "!", new Object[0]);
                        }

                        RenderBlock.addRenderBlock(this.renderBlocks, player.getBlockPos(), this.renderTime.get(), state);
                     }
                  }
               } else {
                  this.burrowedPlayers.remove(player);
               }
            }

            if (this.effectNotifier.get() && (this.friendsEffects.get() || Friends.get().shouldAttack(player))) {
               Map<StatusEffect, StatusEffectInstance> playerEffects = player.getActiveStatusEffects();
               if (this.strength.get()) {
                  if (playerEffects.containsKey(StatusEffects.STRENGTH)) {
                     if (!this.strengthPlayers.contains(player)) {
                        this.strengthPlayers.add(player);
                        StatusEffectInstance effect = (StatusEffectInstance)playerEffects.get(StatusEffects.STRENGTH);
                        this.notifyEffect(player, effect);
                     }
                  } else {
                     this.strengthPlayers.remove(player);
                  }
               }

               if (this.egap.get() && playerEffects.containsKey(StatusEffects.ABSORPTION)) {
                  StatusEffectInstance effect = (StatusEffectInstance)playerEffects.get(StatusEffects.ABSORPTION);
                  if (effect.getDuration() == 2400) {
                     int id = player.getId();
                     this.egapPlayers.put(id, this.egapPlayers.get(id) + 1);
                     this.info(player.getEntityName() + " just ate an Egap! " + this.egapPlayers.get(id) + " in total.", new Object[0]);
                  }
               }

               if (this.weakness.get()) {
                  if (playerEffects.containsKey(StatusEffects.WEAKNESS)) {
                     if (!this.weaknessPlayers.contains(player)) {
                        this.weaknessPlayers.add(player);
                        StatusEffectInstance effect = (StatusEffectInstance)playerEffects.get(StatusEffects.WEAKNESS);
                        this.notifyEffect(player, effect);
                     }
                  } else {
                     this.weaknessPlayers.remove(player);
                  }
               }

               if (this.turtle.get() && playerEffects.containsKey(StatusEffects.RESISTANCE)) {
                  StatusEffectInstance effect = (StatusEffectInstance)playerEffects.get(StatusEffects.RESISTANCE);
                  if (effect.getAmplifier() > 1) {
                     if (!this.turtlePlayers.contains(player)) {
                        this.turtlePlayers.add(player);
                        this.notifyEffect(player, effect);
                     }
                  } else {
                     this.turtlePlayers.remove(player);
                  }
               }
            }
         }
      }
   }

   private void notifyEffect(PlayerEntity player, StatusEffectInstance effect) {
      int seconds = effect.getDuration() / 20;
      int minutes = seconds / 60;
      int modulo = minutes % 60;
      this.info(
         player.getEntityName()
            + " now has "
            + Text.translatable(effect.getTranslationKey())
            + " "
            + (effect.getAmplifier() + 1)
            + " for "
            + (minutes >= 1 ? minutes + ":" + (modulo < 10 ? "0" : "") + modulo + " minutes!" : seconds + " seconds!"),
         new Object[0]
      );
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty() && this.renderBurrow.get() && this.burrowNotify.get()) {
         this.renderBlocks
            .forEach(
               renderBlock -> renderBlock.complexRender(
                     event,
                     (SettingColor)this.sideColor.get(),
                     (SettingColor)this.lineColor.get(),
                     (ShapeMode)this.shapeMode.get(),
                     this.fade.get(),
                     true,
                     (RenderShape)this.renderShape.get(),
                     this.width.get(),
                     this.height.get(),
                     this.yOffset.get(),
                     this.weirdOffset.get()
                  )
            );
      }
   }

   public void onActivate() {
      this.strengthPlayers.clear();
      this.weaknessPlayers.clear();
      this.turtlePlayers.clear();
      this.egapPlayers.clear();
      this.burrowedPlayers.clear();
      this.renderBlocks.clear();
      this.prevBreakingPlayer = null;
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.strengthPlayers.clear();
      this.weaknessPlayers.clear();
      this.turtlePlayers.clear();
      this.egapPlayers.clear();
      this.prevBreakingPlayer = null;
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton placeholders = (WButton)list.add(theme.button("Placeholders")).expandX().widget();
      placeholders.action = () -> new GuideScreen().show();
      return list;
   }
}
