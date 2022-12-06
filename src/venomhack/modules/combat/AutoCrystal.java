package venomhack.modules.combat;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.GameMode;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Box2;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos.class_2339;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import net.minecraft.entity.data.DataTracker.class_2946;
import org.jetbrains.annotations.Nullable;
import venomhack.Venomhack420;
import venomhack.enums.FacePlaceBreak;
import venomhack.enums.Origin;
import venomhack.enums.RenderShape;
import venomhack.enums.SurroundBreak;
import venomhack.enums.SwapMode;
import venomhack.enums.SwitchMode;
import venomhack.enums.Threading;
import venomhack.enums.Type;
import venomhack.mixinInterface.IBlink;
import venomhack.mixinInterface.IModule;
import venomhack.mixinInterface.IVec3d;
import venomhack.modules.ModuleHelper;
import venomhack.modules.misc.PacketMine;
import venomhack.utils.AntiCheatHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.CityUtils;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.PingUtils;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RandUtils;
import venomhack.utils.TextUtils;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.PlacementInfo;
import venomhack.utils.customObjects.RenderBlock;
import venomhack.utils.customObjects.Timer;

public class AutoCrystal extends ModuleHelper implements IModule {
   private final SettingGroup sgBreak = this.group("Break");
   private final SettingGroup sgMisc = this.group("Misc");
   private final SettingGroup sgFacePlace = this.group("FacePlace");
   private final SettingGroup sgEnvironment = this.group("Environment");
   private final SettingGroup sgPause = this.group("Pause");
   private final SettingGroup sgTargeting = this.group("Target");
   private final SettingGroup sgSupport = this.group("Support");
   private final SettingGroup sgSwitch = this.group("Switch");
   private final SettingGroup sgAntiCheat = this.group("Anti Cheat");
   private final SettingGroup sgExperimental = this.group("Experimental");
   private final SettingGroup sgRender = this.group("Rendering");
   private final SettingGroup sgDebug = this.group("Debug");
   private final Setting<Origin> placeOrigin = this.setting("origin", "How to calculate ranges.", Origin.NCP, this.sgMisc);
   private final Setting<Boolean> sequential = this.setting(
      "fast-timing", "Speeds up the ca at the cost of anti cheat compatibility.", Boolean.valueOf(false), this.sgMisc
   );
   private final Setting<Boolean> accurate = this.setting(
      "accurate-damage", "Will slow down ca, while keeping the same lethality. Works best on low ping.", Boolean.valueOf(false), this.sgMisc
   );
   private final Setting<Type> antiFriendPop = this.setting("anti-friend-pop", "Avoids popping your friends.", Type.BOTH, this.sgMisc);
   private final Setting<Boolean> wallsRanges = this.setting("wall-ranges", "Enables walls range settings.", Boolean.valueOf(false), this.sgMisc);
   private final Setting<Boolean> oldMode = this.setting("1.12-mode", "Won't try to place in 1x1x1 holes.", Boolean.valueOf(false), this.sgMisc);
   private final Setting<Threading> threadMode = this.setting("thread-mode", "Will impact performance and can impact speed.", Threading.THREADED, this.sgMisc);
   private final Setting<Integer> placeDelay = this.setting(
      "place-delay", "The amount of delay in ticks before placing.", Integer.valueOf(0), this.sgGeneral, 0.0, 10.0
   );
   private final Setting<Double> placeRange = this.setting("place-range", "The radius in which crystals can be placed.", 5.0, this.sgGeneral, 0.0, 6.0, 1);
   private final Setting<Double> placeWallsRange = this.setting(
      "walls-range", "The radius in which crystals can be placed through walls.", 3.0, this.sgGeneral, this.wallsRanges::get, 0.0, 6.0, 1
   );
   private final Setting<Double> minPlaceDamage = this.setting("min-damage", "The minimum damage the crystal will place.", 6.0, this.sgGeneral, 0.0, 12.0, 1);
   private final Setting<Double> maxPlaceDamage = this.setting(
      "max-damage", "The maximum self damage the crystal will place.", 4.0, this.sgGeneral, 0.0, 20.0, 1
   );
   private final Setting<Boolean> noSuicidePlace = this.setting(
      "anti-suicide-place", "Will not place crystals that are lethal to yourself.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<Integer> breakDelay = this.setting(
      "break-delay", "The amount of delay in ticks before breaking.", Integer.valueOf(0), this.sgBreak, 0.0, 10.0
   );
   private final Setting<Double> minBreakDamage = this.setting(
      "min-damage", "The minimum damage for a crystal to get broken.", 3.0, this.sgBreak, 0.0, 12.0, 1
   );
   private final Setting<Double> maxBreakDamage = this.setting("max-self-damage", "The maximum self-damage allowed.", 4.0, this.sgBreak, 0.0, 20.0, 1);
   private final Setting<Double> breakRatio = this.setting(
      "break-ratio",
      "The damage / self damage ratio at which to ignore the max self damage. Set to 0 to disable.",
      Double.valueOf(0.0),
      this.sgBreak,
      null,
      0.0,
      5.0
   );
   private final Setting<Double> breakRange = this.setting(
      "break-range", "The maximum range that crystals can be to be broken.", 5.0, this.sgBreak, 0.0, 6.0, 1
   );
   private final Setting<Double> breakWallsRange = this.setting(
      "walls-range", "The maximum range that crystals can be to be broken through walls.", 3.0, this.sgBreak, this.wallsRanges::get, 0.0, 6.0, 1
   );
   private final Setting<Boolean> precision = this.setting(
      "hit-boxes", "Increases range by considering the crystals hitboxes into range calculations.", Boolean.valueOf(false), this.sgBreak
   );
   private final Setting<Integer> breakAttempts = this.setting(
      "break-attempts", "How many times to hit a crystal before stopping to target it.", Integer.valueOf(2), this.sgBreak, 1.0, 10.0
   );
   private final Setting<Integer> retryDelay = this.setting(
      "retry-delay",
      "How many ticks to wait until you try to hit a crystal again that reached max break attempts.",
      Integer.valueOf(10),
      this.sgBreak,
      0.0,
      20.0
   );
   private final Setting<Boolean> inhibit = this.setting("no-unnecessary-breaks", "Will prevent unnecessary packets.", Boolean.valueOf(false), this.sgBreak);
   private final Setting<Integer> minAge = this.setting(
      "min-age", "How many ticks a crystal has to exist in order to consider it.", Integer.valueOf(0), this.sgBreak, 0.0, 5.0
   );
   private final Setting<Boolean> noSuicideBreak = this.setting(
      "anti-suicide-break", "Will not break crystals that are lethal to yourself.", Boolean.valueOf(true), this.sgBreak
   );
   private final Setting<Integer> maxBreaksPerSecond = this.setting(
      "breaks-per-second", "How many crystals to attack per second at max.", Integer.valueOf(25), this.sgBreak, 0.0, 25.0
   );
   private final Setting<Boolean> facePlace = this.setting(
      "face-place", "Will face-place when target is below a certain health or armor durability threshold.", Boolean.valueOf(true), this.sgFacePlace
   );
   private final Setting<FacePlaceBreak> facePlaceSpeedMode = this.setting(
      "break-speed-mode",
      "Default uses the normal break speed, custom allows you to set a custom break delay.",
      FacePlaceBreak.DEFAULT,
      this.sgFacePlace,
      this.facePlace::get
   );
   private final Setting<Integer> facePlaceSpeed = this.setting(
      "break-speed",
      "The delay to wait before breaking faceplace crystals.",
      Integer.valueOf(5),
      this.sgFacePlace,
      () -> this.facePlace.get() && this.facePlaceSpeedMode.get() == FacePlaceBreak.CUSTOM,
      0.0,
      10.0
   );
   private final Setting<Integer> facePlaceHealth = this.setting(
      "health", "The health the target has to be at to start faceplacing.", Integer.valueOf(12), this.sgFacePlace, this.facePlace::get, 0.0, 36.0
   );
   private final Setting<Integer> facePlaceDurability = this.setting(
      "durability", "The durability threshold to be able to face-place.", Integer.valueOf(10), this.sgFacePlace, this.facePlace::get, 0.0, 100.0, 0, 100
   );
   private final Setting<Boolean> facePlaceSelf = this.setting(
      "face-place-self", "Whether to faceplace when you are in the same hole as your target.", Boolean.valueOf(true), this.sgFacePlace, this.facePlace::get
   );
   private final Setting<Boolean> facePlaceHole = this.setting(
      "hole-fags", "Automatically starts faceplacing surrounded or burrowed targets.", Boolean.valueOf(false), this.sgFacePlace, this.facePlace::get
   );
   private final Setting<Boolean> facePlaceArmor = this.setting(
      "missing-armor", "Automatically starts faceplacing when a target misses a piece of armor.", Boolean.valueOf(true), this.sgFacePlace, this.facePlace::get
   );
   private final Setting<Keybind> forceFacePlace = this.setting(
      "force-face-place", "Starts faceplacing when this button is pressed", Keybind.fromKey(-1), this.sgFacePlace, this.facePlace::get
   );
   private final Setting<Boolean> pauseSword = this.setting(
      "pause-when-swording", "Doesn't faceplace when you are holding a sword.", Boolean.valueOf(true), this.sgFacePlace, this.facePlace::get
   );
   private final Setting<Boolean> ignoreTerrain = this.setting(
      "ignore-terrain", "Ignores non blast resistant blocks in damage calcs (useful during terrain pvp).", Boolean.valueOf(true), this.sgEnvironment
   );
   private final Setting<SurroundBreak> surroundBreak = this.setting(
      "surround-break",
      "Places a crystal next to a surrounded player and keeps it there so they cannot use Surround again.",
      SurroundBreak.OFF,
      this.sgEnvironment
   );
   private final Setting<Keybind> forceSurroundBreak = this.setting(
      "surround-break-key",
      "Will surround break when pressing this button.",
      Keybind.fromKey(-1),
      this.sgEnvironment,
      () -> this.surroundBreak.get() == SurroundBreak.KEYBIND
   );
   private final Setting<Boolean> antiSurroundBreak = this.setting(
      "surround-protect", "Breaks crystals that could surround break you.", Boolean.valueOf(true), this.sgEnvironment
   );
   private final Setting<Boolean> antiCev = this.setting(
      "anti-funny-crystal", "Breaks crystals that could funny crystal you.", Boolean.valueOf(true), this.sgEnvironment
   );
   private final Setting<Boolean> ccEntities = this.setting(
      "cc-entities", "Crystals only check in a 1x1x1 for other entities, instead of 1x2x1.", Boolean.valueOf(false), this.sgEnvironment
   );
   private final Setting<Boolean> pauseBedAura = this.setting("auto-bed-compat", "Will pause while auto bed is working.", Boolean.valueOf(false), this.sgPause);
   private final Setting<Type> pauseMode = this.setting("pause-mode", "What to pause.", Type.NONE, this.sgPause);
   private final Setting<Boolean> pauseOnEat = this.setting(
      "pause-on-eat", "Pauses Auto Crystal while eating.", Boolean.valueOf(false), this.sgPause, () -> this.pauseMode.get() != Type.NONE
   );
   private final Setting<Boolean> pauseOnMine = this.setting(
      "pause-on-mine", "Pauses Auto Crystal while mining blocks.", Boolean.valueOf(false), this.sgPause, () -> this.pauseMode.get() != Type.NONE
   );
   private final Setting<Boolean> facePlacePause = this.setting(
      "pause-face-placing",
      "When to interrupt face-placing.",
      Boolean.valueOf(false),
      this.sgPause,
      () -> this.pauseMode.get() != Type.NONE && this.facePlace.get()
   );
   private final Setting<Boolean> pauseFacePlaceCev = this.setting(
      "fp-pause-on-funny-crystal",
      "Will stop face placing while auto funny crystal is active.",
      Boolean.valueOf(true),
      this.sgPause,
      () -> this.pauseMode.get() != Type.NONE && this.facePlacePause.get() && this.facePlace.get()
   );
   private final Setting<Boolean> facePlacePauseEat = this.setting(
      "fp-pause-on-eat",
      "Pauses face placing while eating.",
      Boolean.valueOf(false),
      this.sgPause,
      () -> this.pauseMode.get() != Type.NONE && this.facePlacePause.get() && this.facePlace.get()
   );
   private final Setting<Boolean> facePlacePauseMine = this.setting(
      "fp-pause-on-mine",
      "Pauses face placing while mining.",
      Boolean.valueOf(false),
      this.sgPause,
      () -> this.pauseMode.get() != Type.NONE && this.facePlacePause.get() && this.facePlace.get()
   );
   private final Setting<Object2BooleanMap<EntityType<?>>> entities = this.setting(
      "entities", "The entities to attack.", this.sgTargeting, true, null, null, null, new EntityType[]{EntityType.PLAYER}
   );
   private final Setting<Boolean> ignoreNakeds = this.setting("ignore-nakeds", "Will not target naked players.", Boolean.valueOf(false), this.sgTargeting);
   private final Setting<Integer> targetRange = this.setting(
      "target-range", "The maximum range the entity can be to be targeted.", Integer.valueOf(10), this.sgTargeting, 0.0, 18.0
   );
   private final Setting<Boolean> predict = this.setting("predict-movement", "Predicts the targets movement.", Boolean.valueOf(false), this.sgTargeting);
   private final Setting<Integer> predictTicks = this.setting(
      "predict-ticks", "By how many ticks to advance the targets movement forward.", Integer.valueOf(0), this.sgTargeting, this.predict::get, 0.0, 4.0
   );
   private final Setting<Integer> antiStepOffset = this.setting(
      "predict-anti-step-offset", "The maximum step height on the server.", Integer.valueOf(1), this.sgTargeting, this.predict::get, 1.0, 4.0
   );
   private final Setting<Boolean> netDamage = this.setting(
      "total-damage", "Adds up the damage dealt to all targets to find the most efficient location.", Boolean.valueOf(false), this.sgTargeting
   );
   private final Setting<SortPriority> sortPriority = this.setting("target-mode", "Which target to target first.", SortPriority.LowestHealth, this.sgTargeting);
   private final Setting<Boolean> support = this.setting(
      "support", "Places a block in the air and crystals on it. Helps with killing players that are flying.", Boolean.valueOf(false), this.sgSupport
   );
   private final Setting<Integer> supportDelay = this.setting(
      "support-delay", "The delay between support blocks being placed.", Integer.valueOf(5), this.sgSupport, this.support::get, 0.0, 10.0
   );
   private final Setting<Boolean> supportBackup = this.setting(
      "support-backup", "Makes it so support only works if there are no other options.", Boolean.valueOf(true), this.sgSupport, this.support::get
   );
   private final Setting<Boolean> supportAirPlace = this.setting(
      "airplace", "Whether to airplace the support block or not.", Boolean.valueOf(true), this.sgSupport, this.support::get
   );
   private final Setting<Double> supportDamage = this.setting(
      "support-min-damage", "Minimum damage to allow support (has to be higher than min place damage).", 10.0, this.sgSupport, this.support::get, 0.0, 36.0, 2
   );
   private final Setting<Boolean> antiWeakness = this.setting(
      "anti-weakness", "Switches to tools to break crystals with to bypass weakness effect.", Boolean.valueOf(true), this.sgSwitch
   );
   private final Setting<SwitchMode> switchMode = this.setting("switch-mode", "How to switch items.", SwitchMode.NONE, this.sgSwitch);
   private final Setting<SwapMode> silentMode = this.setting(
      "silent-mode",
      "New bypasses certain anti cheat configurations, but might cause desync.",
      SwapMode.OLD,
      this.sgSwitch,
      () -> this.switchMode.get() == SwitchMode.SILENT
   );
   private final Setting<Integer> switchHealth = this.setting(
      "switch-health",
      "The health to stop switching to crystals.",
      Integer.valueOf(0),
      this.sgSwitch,
      () -> this.switchMode.get() != SwitchMode.NONE,
      0.0,
      36.0
   );
   private final Setting<Integer> afterSwitchBreakPause = this.setting(
      "pause-breaking-after-switch.",
      "The amount of milliseconds to wait after switching slots before attempting to break crystals again.",
      Integer.valueOf(0),
      this.sgSwitch,
      0.0,
      500.0
   );
   private final Setting<Boolean> strictDirections = this.setting(
      "strict-directions", "Places only on visible sides.", Boolean.valueOf(false), this.sgAntiCheat
   );
   private final Setting<Type> rotationMode = this.setting("rotation-mode", "The method of rotating when using Auto Crystal.", Type.NONE, this.sgAntiCheat);
   private final Setting<Integer> yawstep = this.setting(
      "max-yawstep",
      "How far to rotate with each step.",
      Integer.valueOf(180),
      this.sgAntiCheat,
      () -> this.rotationMode.get() != Type.NONE,
      1.0,
      180.0,
      1,
      180
   );
   private final Setting<Boolean> strictLook = this.setting(
      "strict-look", "Looks at exactly where you're placing.", Boolean.valueOf(false), this.sgAntiCheat, () -> ((Type)this.rotationMode.get()).placeTrue()
   );
   private final Setting<Target> breakRotationMode = this.setting(
      "break-rotation-target",
      "What part of the crystal to face before breaking.",
      Target.Head,
      this.sgAntiCheat,
      () -> ((Type)this.rotationMode.get()).breakTrue()
   );
   private final Setting<Integer> delayBypass = this.setting(
      "delay-bypass",
      "When below this break threshold ignore break delay. 3 works best for mpvp. Set to 0 to disable.",
      Integer.valueOf(0),
      this.sgExperimental,
      0.0,
      5.0
   );
   private final Setting<Boolean> idPredict = this.setting("crystal-predict", "Tries to speed up breaking.", Boolean.valueOf(false), this.sgExperimental);
   private final Setting<Boolean> aggressivePredict = this.setting(
      "aggressive-predict", "Spams attacks in an attempt to increase speed even further.", Boolean.valueOf(false), this.sgExperimental, this.idPredict::get
   );
   private final Setting<Boolean> instantPredict = this.setting(
      "instant-predict",
      "Attempts to destroy the placed crystal right after spawning it in.",
      Boolean.valueOf(false),
      this.sgExperimental,
      this.idPredict::get
   );
   private final Setting<Integer> minIdOffset = this.setting(
      "min-id-offset", "Minimum id offset.", Integer.valueOf(3), this.sgExperimental, this.idPredict::get, 0.0, 10.0
   );
   private final Setting<Integer> maxIdOffset = this.setting(
      "max-predict-ids", "How many ids to predict in advance max.", Integer.valueOf(5), this.sgExperimental, this.idPredict::get, 1.0, 10.0
   );
   private final Setting<Boolean> swing = this.setting("swing", "Renders your hand swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Creates a render effect where you are placing.", Boolean.valueOf(true), this.sgRender);
   private final Setting<RenderShape> renderShape = this.setting(
      "render-shape", "What shape mode to use.", RenderShape.CUBOID, this.sgRender, this.render::get
   );
   private final Setting<ShapeMode> shapeMode = this.setting(
      "shape-mode", "What part of the shapes to render.", ShapeMode.Both, this.sgRender, this.render::get
   );
   private final Setting<Double> height = this.setting("height", "The height of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 2.0);
   private final Setting<Double> width = this.setting("width", "The width of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 2.0);
   private final Setting<Double> yOffset = this.setting(
      "y-offset", "The height shift for the rendered shape.", Double.valueOf(0.0), this.sgRender, this.render::get, 0.0, 1.0
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
      "render-time", "For how long a shape should be rendered.", Integer.valueOf(10), this.sgRender, this.render::get, 0.0, 20.0
   );
   private final Setting<Boolean> fade = this.setting(
      "fade", "Will reduce the opacity of the rendered block over time.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<Boolean> shrink = this.setting(
      "shrink", "Will reduce the size of the renderer.", Boolean.valueOf(false), this.sgRender, this.render::get
   );
   private final Setting<Boolean> slide = this.setting(
      "slide", "Will make the render slide smoothly from one position to the next.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<Integer> slideSpeed = this.setting(
      "slide-speed",
      "How much of the difference between the last and new position the slider should progress each tick in percent.",
      Integer.valueOf(10),
      this.sgRender,
      () -> this.render.get() && this.slide.get(),
      1.0,
      100.0
   );
   private final Setting<Integer> beforeFadeDelay = this.setting(
      "before-fade-delay",
      "How long to wait before starting to fade. This value has to be smaller than your render time.",
      Integer.valueOf(5),
      this.sgRender,
      () -> this.render.get() && (this.fade.get() || this.shrink.get()),
      0.0,
      10.0
   );
   private final Setting<Integer> maxBlocks = this.setting(
      "max-render-blocks",
      "How many blocks to be rendered at once max.",
      Integer.valueOf(1),
      this.sgRender,
      () -> this.render.get() && !this.slide.get(),
      1.0,
      10.0
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
   private final Setting<Boolean> renderSelfDamage = this.setting(
      "render-self-damage",
      "Renders the damage the crystal is dealing to yourself.",
      Boolean.valueOf(false),
      this.sgRender,
      () -> this.render.get() && this.renderDamage.get()
   );
   private final Setting<Integer> damageRenderTime = this.setting(
      "damage-render-time",
      "For how long the damage text should be rendered.",
      Integer.valueOf(10),
      this.sgRender,
      () -> this.render.get() && this.renderDamage.get(),
      0.0,
      20.0
   );
   private final Setting<Integer> roundDamage = this.setting(
      "round-damage", "Round damage to x decimal places.", Integer.valueOf(2), this.sgRender, () -> this.render.get() && this.renderDamage.get(), 0.0, 3.0
   );
   private final Setting<Double> damageScale = this.setting(
      "damage-scale", "The scale of the damage text.", 1.4, this.sgRender, () -> this.render.get() && this.renderDamage.get(), 0.0, 5.0, 1
   );
   private final Setting<SettingColor> damageColor = this.setting(
      "damage-color", "The color of the damage text.", 255, 255, 255, this.sgRender, () -> this.render.get() && this.renderDamage.get()
   );
   private final Setting<SettingColor> selfDamageColor = this.setting(
      "self-damage-color",
      "The color of the self damage text.",
      255,
      0,
      0,
      this.sgRender,
      () -> this.render.get() && this.renderDamage.get() && this.renderSelfDamage.get()
   );
   private final Setting<Boolean> debug = this.setting("debug", "debug", Boolean.valueOf(false), this.sgDebug);
   private final Setting<Boolean> debugFailedPlace = this.setting("failed-placement", "debug", Boolean.valueOf(false), this.sgDebug, this.debug::get);
   private final Setting<Boolean> debugActualDamage = this.setting("actual-damage", "debug", Boolean.valueOf(false), this.sgDebug, this.debug::get);
   private final Setting<Boolean> debugPlaceDirection = this.setting("interact-direction", "debug", Boolean.valueOf(false), this.sgDebug, this.debug::get);
   private final Setting<Boolean> debugAttackedId = this.setting("attacked-id", "debug", Boolean.valueOf(false), this.sgDebug, this.debug::get);
   private final Setting<Boolean> debugIsPop = this.setting("is-pop", "debug", Boolean.valueOf(false), this.sgDebug, this.debug::get);
   private volatile int lastId;
   private float serverYaw;
   private boolean weak;
   private boolean hasRotatedThisTick;
   private short breaks;
   private short places;
   private Vec3d playerPos;
   private Vec3d rotationTarget;
   private BlockPos lastBlock;
   private LivingEntity lastPlaceTarget;
   private final Timer breakTimer = new Timer();
   private final Timer switchTimer = new Timer();
   private final Timer sinceLastActionTimer = new Timer();
   private int placeDelayLeft;
   private int breakDelayLeft;
   private int supportDelayLeft;
   private int retryDelayLeft;
   private int lastPredictedId;
   private int ticksSinceLastBreak;
   private int antiWeaknessSlot;
   private final List<RenderBlock> renderBlocks = Collections.synchronizedList(new ArrayList<>());
   private final List<PlayerEntity> friends = Collections.synchronizedList(new ArrayList());
   private final List<LivingEntity> targets = new CopyOnWriteArrayList();
   private final Int2FloatOpenHashMap lastTakenDamages = new Int2FloatOpenHashMap();
   private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
   private final Map<Integer, Long> crystalsToRemove = new ConcurrentHashMap<>();
   private final Map<Integer, Long> crystalAliveMap = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> pendingPlacements = new ConcurrentHashMap<>();
   private ExecutorService executor;
   private float slideProgress = 0.0F;

   public AutoCrystal() {
      super(Venomhack420.CATEGORY, "auto-crystal", "Automatically places and breaks crystals to damage other players.");
   }

   @EventHandler(
      priority = 1000
   )
   private void onTick(Pre event) {
      synchronized(this.renderBlocks) {
         for(RenderBlock block : this.renderBlocks) {
            --block.ticks;
            if (block.ticks <= 0.0F) {
               this.slideProgress = 0.0F;
            }
         }

         this.renderBlocks.removeIf(blockx -> blockx.ticks <= 0.0F);
      }

      this.hasRotatedThisTick = false;
      --this.supportDelayLeft;
      --this.retryDelayLeft;
      ++this.ticksSinceLastBreak;
      if (this.retryDelayLeft == 0) {
         this.attemptedBreaks.clear();
      }

      this.handleHurtTime();
      if (PlayerUtils.getGameMode() != GameMode.SPECTATOR) {
         this.playerPos = this.mc.player.getPos();
         if (Modules.get().isActive(Blink.class)) {
            this.playerPos = ((IBlink)Modules.get().get(Blink.class)).getOldPos();
         }

         PlayerUtils2.collectTargets(
            this.targets,
            this.friends,
            this.targetRange.get(),
            Integer.MAX_VALUE,
            this.ignoreNakeds.get(),
            false,
            this.ignoreTerrain.get(),
            (SortPriority)this.sortPriority.get(),
            (Object2BooleanMap<EntityType<?>>)this.entities.get()
         );
         if (this.targets.isEmpty()) {
            this.rotationTarget = null;
         } else if (!this.pauseBedAura.get() || !((AutoBed)Modules.get().get(AutoBed.class)).active()) {
            if ((!this.pauseBedAura.get() || !Modules.get().isActive(AutoBed.class))
               && (this.breakDelayLeft < 1 || this.delayBypass.get() != 0 && this.breaks < this.delayBypass.get())
               && !this.pauseBreak()) {
               this.doBreak();
            } else {
               --this.breakDelayLeft;
            }

            if (this.placeDelayLeft < 1) {
               if (this.canPlace()) {
                  if (this.threadMode.get() == Threading.THREADED && !this.executor.isShutdown() && !this.executor.isTerminated()) {
                     this.executor.submit(this::doPlace);
                  } else {
                     this.doPlace();
                  }
               } else {
                  this.places = 0;
               }
            } else {
               --this.placeDelayLeft;
            }
         }
      }
   }

   @EventHandler(
      priority = Integer.MIN_VALUE
   )
   private void lateTick(Pre event) {
      if (this.rotationTarget != null && !this.hasRotatedThisTick) {
         if (this.rotationMode.get() != Type.NONE && this.yawstep.get() != 180) {
            double targetYaw = Rotations.getYaw(this.rotationTarget);
            if (distanceBetweenAngles((double)this.serverYaw, targetYaw) <= (double)((Integer)this.yawstep.get()).intValue()) {
               Rotations.rotate(targetYaw, Rotations.getPitch(this.rotationTarget));
               this.rotationTarget = null;
            } else {
               targetYaw = MathHelper.wrapDegrees(targetYaw) + 180.0;
               float serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180.0F;
               double delta = Math.abs(targetYaw - (double)serverYaw);
               float yaw = this.serverYaw;
               if ((double)serverYaw < targetYaw) {
                  if (delta < 180.0) {
                     yaw += (float)((Integer)this.yawstep.get()).intValue();
                  } else {
                     yaw -= (float)((Integer)this.yawstep.get()).intValue();
                  }
               } else if (delta < 180.0) {
                  yaw -= (float)((Integer)this.yawstep.get()).intValue();
               } else {
                  yaw += (float)((Integer)this.yawstep.get()).intValue();
               }

               Rotations.rotate((double)yaw, Rotations.getPitch(this.rotationTarget));
            }
         }
      }
   }

   private boolean validRotation(Vec3d target) {
      return distanceBetweenAngles((double)this.serverYaw, Rotations.getYaw(target)) <= (double)((Integer)this.yawstep.get()).intValue();
   }

   private static double distanceBetweenAngles(double alpha, double beta) {
      double phi = Math.abs(MathHelper.wrapDegrees(beta) - MathHelper.wrapDegrees(alpha)) % 360.0;
      return phi > 180.0 ? 360.0 - phi : phi;
   }

   @EventHandler(
      priority = Integer.MAX_VALUE
   )
   private void onPacketRecieve(Receive event) {
      if (this.playerPos != null) {
         if (this.mc.player != null && this.mc.world != null) {
            if (this.breakTimer.millisPassed() >= 1000L) {
               this.breaks = 0;
            }

            if (this.breaks == 0) {
               this.breakTimer.reset();
            }

            if ((double)TickRate.INSTANCE.getTimeSinceLastTick() < 1.5) {
               long considerDeadTime = (long)((float)this.getPing() * 20.0F / TickRate.INSTANCE.getTickRate());

               for(Entry<Integer, Long> breakEntry : this.crystalsToRemove.entrySet()) {
                  if (System.currentTimeMillis() - considerDeadTime > breakEntry.getValue()) {
                     this.crystalsToRemove.remove(breakEntry.getKey());
                  }
               }

               for(Entry<BlockPos, Long> placeEntry : this.pendingPlacements.entrySet()) {
                  if (System.currentTimeMillis() - considerDeadTime > placeEntry.getValue()) {
                     if (this.debug.get() && this.debugFailedPlace.get()) {
                        this.info(
                           "failed placement. distance: "
                              + Vec3d.ofCenter((Box2)placeEntry.getKey()).distanceTo(PlayerUtils2.eyePos(this.mc.player)),
                           new Object[0]
                        );
                     }

                     this.pendingPlacements.remove(placeEntry.getKey());
                  }
               }
            } else {
               this.attemptedBreaks.clear();
               this.crystalsToRemove.clear();
            }

            Packet living = event.packet;
            if (living instanceof EntitySpawnS2CPacket packet) {
               if (packet.getId() > this.lastId) {
                  this.lastId = packet.getId();
               }

               this.crystalAliveMap.put(packet.getId(), System.currentTimeMillis());
               if (packet.getEntityTypeId() != EntityType.END_CRYSTAL) {
                  return;
               }

               EndCrystalEntity crystal = new EndCrystalEntity(this.mc.world, packet.getX(), packet.getY(), packet.getZ());
               crystal.setId(packet.getId());
               this.pendingPlacements.remove(crystal.getBlockPos().down());
               if (this.minAge.get() > 0) {
                  return;
               }

               if (this.breakDelayLeft > 0 && (this.delayBypass.get() == 0 || this.breaks >= this.delayBypass.get())) {
                  return;
               }

               if (this.pauseBreak()) {
                  return;
               }

               if (this.shouldNotBreak(crystal, this.inhibit.get(), true)) {
                  return;
               }

               for(LivingEntity target : this.targets) {
                  if (!target.isDead() && !(crystal.distanceTo(target) > 9.0F)) {
                     float damage = this.applyHurtTimeToDamage(
                        target,
                        DamageCalcUtils.explosionDamage(
                           target,
                           crystal.getPos(),
                           this.predict.get(),
                           this.predictTicks.get(),
                           this.antiStepOffset.get(),
                           this.ignoreTerrain.get(),
                           false,
                           true,
                           6
                        ),
                        false,
                        false
                     );
                     if (!((double)damage < (this.shouldFacePlace(target) ? 1.5 : this.minBreakDamage.get()))) {
                        if (this.unsafeBreak(this.mc.player, crystal.getPos(), (double)damage)) {
                           return;
                        }

                        if (((Type)this.antiFriendPop.get()).breakTrue()) {
                           for(PlayerEntity friend : this.friends) {
                              if (this.unsafeBreak(friend, crystal.getPos(), (double)damage)) {
                                 return;
                              }
                           }
                        }

                        this.doAttack(crystal, false, null);
                        return;
                     }
                  }
               }
            } else {
               living = event.packet;
               if (living instanceof ExplosionS2CPacket packet) {
                  Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

                  for(Entity entity : this.mc.world.getEntities()) {
                     if (entity instanceof EndCrystalEntity && !(entity.squaredDistanceTo(pos) > Math.pow((double)(packet.getRadius() * 2.0F), 2.0))) {
                        this.crystalsToRemove.put(entity.getId(), System.currentTimeMillis());
                     }
                  }
               } else {
                  living = event.packet;
                  if (living instanceof EntitiesDestroyS2CPacket packet) {
                     IntListIterator var22 = packet.getEntityIds().iterator();

                     while(var22.hasNext()) {
                        int id = var22.next();
                        if (id > this.lastId) {
                           this.lastId = id;
                        }

                        this.crystalAliveMap.remove(id);
                        this.attemptedBreaks.remove(id);
                     }
                  } else {
                     living = event.packet;
                     if (living instanceof EntityTrackerUpdateS2CPacket packet) {
                        if (packet.id() > this.lastId) {
                           this.lastId = packet.id();
                        }

                        if (packet.getTrackedValues() == null) {
                           return;
                        }

                        try {
                           Entity entity = this.mc.world.getEntityById(packet.id());
                           if (!(entity instanceof LivingEntity)) {
                              return;
                           }

                           LivingEntity livingEntity = (LivingEntity)entity;
                           living = livingEntity;
                        } catch (ArrayIndexOutOfBoundsException var12) {
                           return;
                        }

                        float healthDelta = 0.0F;
                        float absorbtionDelta = 0.0F;

                        for(class_2946<?> entry : packet.getTrackedValues()) {
                           if (entry.getData() == PlayerEntity.ABSORPTION_AMOUNT) {
                              absorbtionDelta = living.getAbsorptionAmount() - entry.get();
                           }

                           if (entry.getData() == LivingEntity.HEALTH) {
                              healthDelta = living.getHealth() - entry.get();
                           }
                        }

                        if (healthDelta + absorbtionDelta > 0.0F) {
                           if (this.debug.get() && this.debugActualDamage.get()) {
                              this.info("actual damage: " + TextUtils.round(healthDelta + absorbtionDelta, 4), new Object[0]);
                           }

                           if ((long)living.timeUntilRegen - Math.round((double)this.getPing() / 50.0) > 10L) {
                              this.lastTakenDamages
                                 .put(packet.id(), this.lastTakenDamages.getOrDefault(packet.id(), 0.0F) + healthDelta + absorbtionDelta);
                           } else {
                              this.lastTakenDamages.put(packet.id(), healthDelta + absorbtionDelta);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void doFacePlaceCity(BlockPos pos) {
      if (this.breakDelayLeft <= 0 || this.delayBypass.get() != 0 && this.breaks < this.delayBypass.get()) {
         BlockPos up = pos.up();
         List<EndCrystalEntity> entities = this.mc.world.getOtherEntities(null, new Box(up), entity -> {
            if (entity instanceof EndCrystalEntity crystalx && !this.shouldNotBreak(crystalx, this.inhibit.get(), true) && entity.getBlockPos().equals(up)) {
               return true;
            }

            return false;
         }).stream().map(entity -> (EndCrystalEntity)entity).toList();
         if (!entities.isEmpty()) {
            EndCrystalEntity crystal = (EndCrystalEntity)entities.get(0);
            boolean notSurround = true;

            for(LivingEntity target : this.targets) {
               if (BlockUtils2.getCity(target, false, true).contains(pos)) {
                  notSurround = false;
                  break;
               }
            }

            if (!notSurround) {
               PlacementInfo replace = new PlacementInfo(pos.down());
               if (this.placeDelayLeft > 0
                  || this.invalidPlace(
                     replace.getBlockPos(), this.mc.world.getBlockState(replace.getBlockPos()), false, replace, crystal.getId(), null, true
                  )) {
                  replace = null;
               }

               this.doAttack(crystal, false, replace);
            }
         }
      }
   }

   private float applyHurtTimeToDamage(LivingEntity entity, float damage, boolean placing, boolean force) {
      if (this.getPing() == 0) {
         return damage;
      } else {
         if (!force && !this.accurate.get() && entity != this.mc.player) {
            if (!(entity instanceof PlayerEntity)) {
               return damage;
            }

            PlayerEntity player = (PlayerEntity)entity;
            if (Friends.get().shouldAttack(player)) {
               return damage;
            }
         }

         if ((long)entity.timeUntilRegen - Math.round((double)(this.getPing() * (placing ? 3 : 2)) / 50.0) > 10L) {
            float adjustedSelfDamage = damage - this.lastTakenDamages.getOrDefault(entity.getId(), 0.0F);
            if (adjustedSelfDamage < 0.0F) {
               adjustedSelfDamage = 0.0F;
            }

            return adjustedSelfDamage;
         } else {
            return damage;
         }
      }
   }

   private boolean canPlace() {
      if (((Type)this.pauseMode.get()).placeTrue() && PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnEat.get())) {
         return false;
      } else if (this.mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL
         || this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
         return this.placeDelayLeft <= 0;
      } else if (this.switchMode.get() == SwitchMode.NONE) {
         return false;
      } else if (PlayerUtils.getTotalHealth() <= (double)((Integer)this.switchHealth.get()).intValue()) {
         return false;
      } else {
         return this.switchMode.get() == SwitchMode.SILENT && this.silentMode.get() == SwapMode.NEW
            ? InvUtils.find(new Item[]{Items.END_CRYSTAL}).found()
            : InvUtils.find(item -> item.getItem() instanceof EndCrystalItem, 0, 8).found();
      }
   }

   private void doPlace() {
      PlacementInfo bestPlace = new PlacementInfo();
      boolean canSupport = this.support.get() && this.supportDelayLeft < 1 && InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN}).found();
      Vec3d playerPos = this.mc.player.getPos();
      if (this.placeOrigin.get() == Origin.NCP) {
         playerPos = PlayerUtils2.eyePos(this.mc.player);
      }

      int range = (int)Math.ceil(this.placeRange.get()) + 1;
      class_2339 blockPos = new class_2339();
      Vec3d crystalVec = new Vec3d(0.0, 0.0, 0.0);
      IVec3d iCrystalVec = (IVec3d)crystalVec;

      for(int x = -range; x <= range; ++x) {
         for(int y = -range; y <= range; ++y) {
            label281:
            for(int z = -range; z <= range; ++z) {
               blockPos.set(playerPos.x + (double)x, playerPos.y + (double)y, playerPos.z + (double)z);
               BlockState blockState = this.mc.world.getBlockState(blockPos);
               PlacementInfo place = new PlacementInfo(blockPos);
               if (!this.invalidPlace(blockPos, blockState, canSupport, place, 0, bestPlace, false)) {
                  float totalDamage = 0.0F;
                  boolean facePlaceLimit = false;
                  boolean foundPos = false;
                  iCrystalVec.set((double)blockPos.getX() + 0.5, (double)(blockPos.getY() + 1), (double)blockPos.getZ() + 0.5);
                  Iterator var16 = this.targets.iterator();

                  while(true) {
                     label276:
                     while(true) {
                        if (!var16.hasNext()) {
                           if (!this.netDamage.get() || !foundPos) {
                              continue label281;
                           }

                           place.setDamage(totalDamage);
                           if (place.value(this.lastPlaceTarget) > bestPlace.value(this.lastPlaceTarget)
                              || this.supportBackup.get() && bestPlace.isSupport() && !place.isSupport()) {
                              bestPlace.copy(place);
                           }
                           continue label281;
                        }

                        LivingEntity target = (LivingEntity)var16.next();
                        if (!target.isDead()
                           && (
                              bestPlace.getBlockPos() == null
                                 || !(
                                    DamageCalcUtils.explosionDamage(
                                          target, crystalVec, this.predict.get(), this.predictTicks.get(), this.antiStepOffset.get(), true, true, false, 6
                                       )
                                       < bestPlace.getDamage()
                                 )
                           )) {
                           float damage = this.applyHurtTimeToDamage(
                              target,
                              DamageCalcUtils.explosionDamage(
                                 target,
                                 crystalVec,
                                 this.predict.get(),
                                 this.predictTicks.get(),
                                 this.antiStepOffset.get(),
                                 this.ignoreTerrain.get(),
                                 true,
                                 true,
                                 6
                              ),
                              true,
                              false
                           );
                           totalDamage += damage;
                           if (!this.accurate.get()
                                 && (double)this.applyHurtTimeToDamage(target, damage, true, true) > (double)UtilsPlus.getTotalHealth(target) + 0.5
                              || this.accurate.get() && (double)damage > (double)UtilsPlus.getTotalHealth(target) + 0.5) {
                              place.incrementPops();
                           }

                           place.reset(target, damage);
                           if (blockPos.getY() <= target.getBlockY()
                              && (!place.isSupport() || !((double)damage < this.supportDamage.get()))
                              && (
                                 this.netDamage.get()
                                    || bestPlace.getSurroundBreak() != 0
                                    || !(place.value(this.lastPlaceTarget) < bestPlace.value(this.lastPlaceTarget))
                                    || canSupport && this.supportBackup.get() && bestPlace.isSupport() && !place.isSupport()
                              )) {
                              if (!facePlaceLimit && this.shouldFacePlace(target)) {
                                 place.setFaceplace(true);
                              }

                              float minDamage = place.isFaceplace() ? 1.5F : ((Double)this.minPlaceDamage.get()).floatValue();
                              Iterator breakValue = this.mc.world.getEntities().iterator();

                              label247:
                              while(true) {
                                 while(true) {
                                    if (!breakValue.hasNext()) {
                                       if (!(damage < minDamage)) {
                                          break label276;
                                       }

                                       switch((SurroundBreak)this.surroundBreak.get()) {
                                          case OFF:
                                             continue label276;
                                          case PICKAXE:
                                             if (!(this.mc.player.getMainHandStack().getItem() instanceof PickaxeItem)) {
                                                continue label276;
                                             }
                                             break;
                                          case KEYBIND:
                                             if (!((Keybind)this.forceSurroundBreak.get()).isPressed() || this.mc.currentScreen != null) {
                                                continue label276;
                                             }
                                       }

                                       if ((bestPlace.getBlockPos() == null || bestPlace.getSurroundBreak() != 0)
                                          && !Modules.get().isActive(AutoFunnyCrystal.class)
                                          && UtilsPlus.isSurrounded(target, true, this.ignoreTerrain.get())) {
                                          for(int i = 0; i < this.targets.indexOf(target) + 1; ++i) {
                                             if (UtilsPlus.isSurroundBroken((LivingEntity)this.targets.get(i))) {
                                                continue label276;
                                             }
                                          }

                                          byte breakValuex = UtilsPlus.getSurroundBreak(target, blockPos);
                                          if (bestPlace.getSurroundBreak() < breakValuex) {
                                             place.setSurroundBreak(breakValuex);
                                             break label276;
                                          }
                                       }
                                       continue label276;
                                    }

                                    Entity entity = (Entity)breakValue.next();
                                    if (entity instanceof EndCrystalEntity crystal) {
                                       if ((this.mc.player.airStrafingSpeed != 0.0F || this.mc.player.forwardSpeed != 0.0F)
                                          && crystal.getPos()
                                                .squaredDistanceTo(
                                                   playerPos.x + this.mc.player.getX() - this.mc.player.prevX,
                                                   playerPos.y + this.mc.player.getY() - this.mc.player.prevY,
                                                   playerPos.z + this.mc.player.getZ() - this.mc.player.prevZ
                                                )
                                             < crystal.getPos().squaredDistanceTo(playerPos)) {
                                          if (!this.shouldNotBreak(crystal, true, false)) {
                                             double distance = Math.sqrt(
                                                playerPos.squaredDistanceTo(crystal.getX(), crystal.getY() - 0.5, crystal.getZ())
                                             );
                                             if (!(distance >= 8.0)) {
                                                distance = Math.sqrt(
                                                   ((Origin)this.placeOrigin.get())
                                                      .getOrigin(playerPos)
                                                      .squaredDistanceTo(crystal.getX(), crystal.getY() - 0.5, crystal.getZ())
                                                );
                                                if (distance > this.placeRange.get()
                                                   || this.wallsRanges.get()
                                                      && distance > this.placeWallsRange.get()
                                                      && !UtilsPlus.canSeeBlock(blockPos, ((Origin)this.placeOrigin.get()).getOrigin(playerPos))) {
                                                   continue;
                                                }
                                                break;
                                             }
                                          }
                                       } else if (!this.shouldNotBreak(crystal, true, true)) {
                                          break;
                                       }
                                    }
                                 }

                                 float multiDamage = this.applyHurtTimeToDamage(
                                    target,
                                    DamageCalcUtils.explosionDamage(
                                       target,
                                       crystal.getPos(),
                                       this.predict.get(),
                                       this.predictTicks.get(),
                                       this.antiStepOffset.get(),
                                       this.ignoreTerrain.get(),
                                       false,
                                       true,
                                       6
                                    ),
                                    false,
                                    false
                                 );
                                 if (!(multiDamage < minDamage) && !this.unsafeBreak(this.mc.player, crystal.getPos(), (double)multiDamage)) {
                                    if (!((Type)this.antiFriendPop.get()).breakTrue()) {
                                       return;
                                    }

                                    for(PlayerEntity friend : this.friends) {
                                       if (this.unsafeBreak(friend, crystal.getPos(), (double)multiDamage)) {
                                          continue label247;
                                       }
                                    }

                                    return;
                                 }
                              }
                           }
                        }
                     }

                     foundPos = true;
                     if (!this.netDamage.get()) {
                        if (place.isFaceplace()) {
                           facePlaceLimit = true;
                        }

                        bestPlace.copy(place);
                     }
                  }
               }
            }
         }
      }

      if (bestPlace.getBlockPos() != null) {
         this.place(bestPlace, ((Type)this.rotationMode.get()).placeTrue());
      }
   }

   private BlockState getState(BlockPos pos) {
      return ((PacketMine)Modules.get().get(PacketMine.class)).getState(pos);
   }

   private boolean invalidPlace(
      BlockPos blockPos,
      BlockState blockState,
      boolean canSupport,
      PlacementInfo place,
      int idToIgnore,
      @Nullable PlacementInfo bestPlace,
      boolean upStateGotMined
   ) {
      if (BlockUtils2.invalidPos(blockPos)) {
         return true;
      } else if (this.pendingPlacements.isEmpty() || this.pendingPlacements.size() <= 1 && this.pendingPlacements.get(blockPos) != null) {
         Vec3d pos = Vec3d.ofCenter(blockPos);
         if (!upStateGotMined) {
            BlockState upState = this.getState(blockPos.up());
            if (!upState.isAir()) {
               if (upState.getBlock().getHardness() != 0.0F) {
                  return true;
               }

               place.setShouldBreak(true);
            }
         }

         if (this.oldMode.get() && !this.getState(blockPos.up(2)).isAir()) {
            return true;
         } else {
            if (blockState.getBlock() != Blocks.BEDROCK && blockState.getBlock() != Blocks.OBSIDIAN) {
               if (!canSupport) {
                  return true;
               }

               if (this.supportBackup.get() && bestPlace != null && bestPlace.getPos() != null && !bestPlace.isSupport()) {
                  return true;
               }

               if (!blockState.getMaterial().isReplaceable()) {
                  return true;
               }

               if (!this.supportAirPlace.get() && BlockUtils2.noSupport(blockPos)) {
                  return true;
               }

               place.setSupport(true);
            }

            Direction dir = BlockUtils2.getClosestDirection(blockPos, false);
            if (this.strictDirections.get()) {
               dir = BlockUtils2.getStrictSide(blockPos);
               if (dir == null) {
                  return true;
               }
            }

            place.setDirection(dir);
            ((IVec3d)pos)
               .set(
                  pos.x + (double)dir.getVector().getX() * 0.5,
                  pos.y + (double)dir.getVector().getY() * 0.5,
                  pos.z + (double)dir.getVector().getZ() * 0.5
               );
            place.setVec3d(pos);
            if (BlockUtils2.outOfPlaceRange(blockPos, (Origin)this.placeOrigin.get(), this.placeRange.get())) {
               return true;
            } else if (this.wallsRanges.get()
               && BlockUtils2.outOfPlaceRange(blockPos, (Origin)this.placeOrigin.get(), this.placeWallsRange.get())
               && !UtilsPlus.canSeeBlock(blockPos, PlayerUtils2.eyePos(this.mc.player))) {
               return true;
            } else {
               EndCrystalEntity crystal = new EndCrystalEntity(
                  this.mc.world, (double)blockPos.getX() + 0.5, (double)(blockPos.getY() + 1), (double)blockPos.getZ() + 0.5
               );
               boolean movingTowardsCrystal = (this.mc.player.airStrafingSpeed != 0.0F || this.mc.player.forwardSpeed != 0.0F)
                  && crystal.getPos()
                        .squaredDistanceTo(
                           this.playerPos.x + this.mc.player.getX() - this.mc.player.prevX,
                           this.playerPos.y + this.mc.player.getY() - this.mc.player.prevY,
                           this.playerPos.z + this.mc.player.getZ() - this.mc.player.prevZ
                        )
                     < crystal.getPos().squaredDistanceTo(this.playerPos);
               if (!movingTowardsCrystal && this.notInBreakRange(crystal)) {
                  return true;
               } else if (this.intersectsWithEntity(blockPos, place.isSupport(), idToIgnore)) {
                  return true;
               } else {
                  float selfDamage = this.applyHurtTimeToDamage(
                     this.mc.player,
                     DamageCalcUtils.explosionDamage(this.mc.player, crystal.getPos(), false, 0, 0, this.ignoreTerrain.get(), true, true, 6),
                     false,
                     false
                  );
                  place.setSelfDamage(selfDamage);
                  if ((double)selfDamage > this.maxPlaceDamage.get()) {
                     return true;
                  } else {
                     if ((double)(EntityUtils.getTotalHealth(this.mc.player) - selfDamage) < 0.5) {
                        if (this.noSuicidePlace.get()) {
                           return true;
                        }

                        place.setSelfPop();
                     }

                     if (((Type)this.antiFriendPop.get()).placeTrue()) {
                        for(PlayerEntity friend : this.friends) {
                           float friendDamage = this.applyHurtTimeToDamage(
                              friend,
                              DamageCalcUtils.explosionDamage(
                                 friend,
                                 crystal.getPos(),
                                 this.predict.get(),
                                 this.predictTicks.get(),
                                 this.antiStepOffset.get(),
                                 this.ignoreTerrain.get(),
                                 true,
                                 true,
                                 6
                              ),
                              true,
                              false
                           );
                           if ((double)friendDamage > this.maxPlaceDamage.get()) {
                              return true;
                           }

                           if ((double)(EntityUtils.getTotalHealth(friend) - friendDamage) < 0.5) {
                              if (this.noSuicidePlace.get()) {
                                 return true;
                              }

                              place.incrementFriendPops();
                           }

                           place.addFriendDamage(friendDamage);
                        }
                     }

                     return false;
                  }
               }
            }
         }
      } else {
         return true;
      }
   }

   private void place(PlacementInfo place, boolean rotate) {
      if (rotate) {
         Vec3d vec = place.getPos();
         if (this.strictLook.get()) {
            vec = Vec3d.ofCenter(place.getBlockPos());
         }

         if (this.hasRotatedThisTick || this.yawstep.get() != 180 && !this.validRotation(place.getPos())) {
            if (this.rotationTarget == null) {
               this.rotationTarget = place.getPos();
            }
         } else {
            Rotations.rotate(Rotations.getYaw(vec), Rotations.getPitch(vec), 85, () -> this.place(place, false));
            this.hasRotatedThisTick = true;
         }
      } else {
         BlockHitResult result = new BlockHitResult(place.getPos(), place.getDirection(), place.getBlockPos(), false);
         if (place.isSupport()) {
            BlockUtils2.justPlace(
               InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN}), result, this.swing.get(), ((Type)this.rotationMode.get()).placeTrue(), 45
            );
            this.supportDelayLeft = this.supportDelay.get();
         }

         if (place.shouldBreak()) {
            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, place.getBlockPos().up(), Direction.DOWN));
            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, place.getBlockPos().up(), Direction.DOWN));
         }

         int preSlot = -1;
         if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
            && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            preSlot = this.mc.player.getInventory().selectedSlot;
            int slot = this.switchMode.get() == SwitchMode.SILENT && this.silentMode.get() == SwapMode.NEW
               ? InvUtils.find(new Item[]{Items.END_CRYSTAL}).slot()
               : InvUtils.find(item -> item.getItem() instanceof EndCrystalItem, 0, 8).slot();
            if (slot == -1) {
               return;
            }

            if (this.switchMode.get() == SwitchMode.AUTO) {
               this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
               this.mc.player.getInventory().selectedSlot = slot;
            } else if (this.silentMode.get() == SwapMode.OLD) {
               this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            } else {
               RandUtils.clickSlotPacket(this.mc.player.getInventory().selectedSlot + 36, slot, SlotActionType.SWAP);
               preSlot = slot;
            }
         }

         this.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(this.getHand(), result, 0));
         RandUtils.swing(this.swing.get(), this.getHand());
         if (preSlot != -1 && this.switchMode.get() == SwitchMode.SILENT) {
            if (this.silentMode.get() == SwapMode.OLD) {
               this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(preSlot));
            } else {
               RandUtils.clickSlotPacket(this.mc.player.getInventory().selectedSlot + 36, preSlot, SlotActionType.SWAP);
            }
         }

         if (this.debug.get()) {
            if (this.debugPlaceDirection.get()) {
               this.info("dir:" + place.getDirection(), new Object[0]);
            }

            if (this.debugIsPop.get() && place.getPops() > 0) {
               this.info("pops", new Object[0]);
            }
         }

         this.placeDelayLeft = this.placeDelay.get();
         if (place.isFaceplace()
            && !this.netDamage.get()
            && this.facePlaceSpeedMode.get() == FacePlaceBreak.CUSTOM
            && UtilsPlus.isSurrounded(place.getTarget(), true, this.ignoreTerrain.get())) {
            this.breakDelayLeft = Math.max(this.facePlaceSpeed.get(), this.breakDelayLeft);
         }

         if ((this.breakDelayLeft < 1 || this.delayBypass.get() != 0 && this.breaks < this.delayBypass.get())
            && this.idPredict.get()
            && this.instantPredict.get()) {
            EndCrystalEntity crystal = new EndCrystalEntity(this.mc.world, place.getPos().x, place.getPos().y, place.getPos().z);
            crystal.setId(this.lastId + this.minIdOffset.get());
            this.doAttack(crystal, true, null);
         }

         this.queueRenderBlock(place.getBlockPos(), place.getDamage(), place.getSelfDamage());
         this.pendingPlacements.putIfAbsent(place.getBlockPos(), System.currentTimeMillis());
         ++this.places;
         this.lastPlaceTarget = place.getTarget();
         this.lastBlock = place.getBlockPos();
         this.sinceLastActionTimer.reset();
      }
   }

   private void queueRenderBlock(BlockPos pos, float damage, float selfDamage) {
      boolean found = false;

      for(RenderBlock block : this.renderBlocks) {
         if (block.pos.equals(pos)) {
            block.set(this.renderTime.get(), damage, selfDamage, null);
            found = true;
         }
      }

      if (!found) {
         RenderBlock block = new RenderBlock(pos, this.renderTime.get(), damage, selfDamage, null);
         if (this.slide.get()) {
            if (this.renderBlocks.size() > 1) {
               this.renderBlocks.set(1, block);
            } else {
               this.renderBlocks.add(block);
               this.slideProgress = 0.0F;
            }
         } else {
            if (!this.renderBlocks.isEmpty()) {
               while(this.renderBlocks.size() >= this.maxBlocks.get()) {
                  this.renderBlocks.remove(0);
               }
            }

            this.renderBlocks.add(block);
         }
      }
   }

   private void doBreak() {
      PlacementInfo surroundReplace = null;
      float bestDamage = 0.0F;
      EndCrystalEntity bestCrystal = null;
      Iterator predictedCrystal = this.mc.world.getEntities().iterator();

      label220:
      while(true) {
         while(true) {
            if (!predictedCrystal.hasNext()) {
               if (bestCrystal != null) {
                  this.doAttack(bestCrystal, false, surroundReplace);
               } else if (this.idPredict.get() && this.places > 0) {
                  if (this.lastBlock == null) {
                     return;
                  }

                  EndCrystalEntity predictedCrystalx = new EndCrystalEntity(
                     this.mc.world,
                     (double)this.lastBlock.getX() + 0.5,
                     (double)(this.lastBlock.getY() + 1),
                     (double)this.lastBlock.getZ() + 0.5
                  );
                  if (this.aggressivePredict.get()) {
                     for(int id = this.minIdOffset.get(); id <= this.maxIdOffset.get(); ++id) {
                        if (!this.badId(id)) {
                           predictedCrystalx.setId(this.lastId + id);
                           this.doAttack(predictedCrystalx, true, null);
                        }
                     }
                  } else {
                     if (this.places + this.minIdOffset.get() >= this.maxIdOffset.get()) {
                        return;
                     }

                     if (this.lastPredictedId == this.places + this.minIdOffset.get()) {
                        return;
                     }

                     if (this.badId(this.lastId + this.places + this.minIdOffset.get())) {
                        ++this.places;
                        return;
                     }

                     predictedCrystalx.setId(this.lastId + this.places + this.minIdOffset.get());
                     this.doAttack(predictedCrystalx, true, null);
                     this.lastPredictedId = this.places + this.minIdOffset.get();
                  }
               }

               return;
            }

            Entity e = (Entity)predictedCrystal.next();
            if (e instanceof EndCrystalEntity crystal) {
               if (this.minAge.get() <= 0) {
                  break;
               }

               long yawstepsNeeded = Math.round(
                  distanceBetweenAngles((double)this.serverYaw, Rotations.getYaw(crystal)) / (double)((Integer)this.yawstep.get()).intValue()
               );
               Long msAge = this.crystalAliveMap.getOrDefault(crystal.getId(), 0L);
               float msRequired = (float)((Integer)this.minAge.get()).intValue() * 1000.0F / TickRate.INSTANCE.getTickRate();
               if (this.getPing() != 100) {
                  msRequired -= (float)PingUtils.getAveragePing();
               }

               if (!((float)(System.currentTimeMillis() - msAge) < msRequired)
                  || ((Type)this.rotationMode.get()).breakTrue()
                     && this.yawstep.get() != 180
                     && Math.round((double)(msRequired - (float)System.currentTimeMillis() + (float)msAge.longValue()) / 50.0) <= yawstepsNeeded) {
                  break;
               }
            }
         }

         if (!this.shouldNotBreak(crystal, this.inhibit.get(), true)) {
            Vec3d crystalPos = crystal.getPos();
            int totalDamage = 0;
            boolean foundBreak = false;
            Iterator var25 = this.targets.iterator();

            while(true) {
               float damage;
               while(true) {
                  if (!var25.hasNext()) {
                     if (this.netDamage.get() && foundBreak && !((float)totalDamage < bestDamage)) {
                        bestDamage = (float)totalDamage;
                        bestCrystal = crystal;
                     }
                     continue label220;
                  }

                  LivingEntity target = (LivingEntity)var25.next();
                  if (!target.isDead()
                     && (
                        this.netDamage.get()
                           || bestDamage == 0.0F
                           || !(
                              bestDamage
                                 > DamageCalcUtils.explosionDamage(
                                    target,
                                    crystalPos,
                                    this.predict.get(),
                                    this.predictTicks.get(),
                                    this.antiStepOffset.get(),
                                    this.ignoreTerrain.get(),
                                    false,
                                    false,
                                    6
                                 )
                           )
                     )) {
                     damage = this.applyHurtTimeToDamage(
                        target,
                        DamageCalcUtils.explosionDamage(
                           target,
                           crystalPos,
                           this.predict.get(),
                           this.predictTicks.get(),
                           this.antiStepOffset.get(),
                           this.ignoreTerrain.get(),
                           false,
                           true,
                           6
                        ),
                        false,
                        false
                     );
                     totalDamage = (int)((float)totalDamage + damage);
                     if (this.unsafeBreak(this.mc.player, crystalPos, (double)damage)) {
                        continue label220;
                     }

                     if (((Type)this.antiFriendPop.get()).breakTrue()) {
                        for(PlayerEntity friend : this.friends) {
                           if (this.unsafeBreak(friend, crystalPos, (double)damage)) {
                              continue label220;
                           }
                        }
                     }

                     if (bestCrystal == null) {
                        if (this.antiSurroundBreak.get()
                           && UtilsPlus.isSurrounded(this.mc.player, true, this.ignoreTerrain.get())
                           && UtilsPlus.getSurroundBreak(this.mc.player, crystal.getBlockPos().down()) > 0) {
                           bestCrystal = crystal;
                           bestDamage = damage;
                        }

                        if (this.antiCev.get()
                           && this.getState(this.mc.player.getBlockPos().up(2)).getBlock() == Blocks.OBSIDIAN
                           && crystal.getBlockPos().equals(this.mc.player.getBlockPos().add(0, 3, 0))) {
                           bestCrystal = crystal;
                           bestDamage = damage;
                        }
                     }

                     if (bestCrystal == null) {
                        BlockPos targetBlockPos = target.getBlockPos().down();
                        BlockPos crystalPosDown = crystal.getBlockPos().down(2);

                        for(Box2 city : CityUtils.CITY_WITHOUT_BURROW) {
                           BlockPos pos = targetBlockPos.add(city);
                           if (crystalPosDown.equals(pos)) {
                              PlacementInfo placeInfo = new PlacementInfo(pos);
                              if (!this.invalidPlace(pos, this.getState(pos), false, placeInfo, -69, null, false)) {
                                 bestCrystal = crystal;
                                 bestDamage = damage;
                                 surroundReplace = placeInfo;
                                 break;
                              }
                           }
                        }
                     }

                     if (!((double)damage < (this.shouldFacePlace(target) ? 1.5 : this.minBreakDamage.get()))) {
                        break;
                     }

                     if (!(bestDamage > damage) && this.placeDelayLeft <= 0) {
                        for(Box2 city : CityUtils.CITY_WITHOUT_BURROW) {
                           BlockPos pos = target.getBlockPos().add(city);
                           if (UtilsPlus.isBlockSurroundBrokenByCrystal(pos, crystal)) {
                              BlockPos down = pos.down();
                              PlacementInfo placeInfo = new PlacementInfo(down);
                              if (!this.invalidPlace(down, this.getState(down), false, placeInfo, crystal.getId(), null, false)) {
                                 bestCrystal = crystal;
                                 bestDamage = damage;
                                 surroundReplace = placeInfo;
                                 break;
                              }
                           }
                        }

                        if (surroundReplace != null) {
                           break;
                        }
                     }
                  }
               }

               foundBreak = true;
               if (!this.netDamage.get() && damage > bestDamage) {
                  bestDamage = damage;
                  bestCrystal = crystal;
               }
            }
         }
      }
   }

   private boolean badId(int id) {
      try {
         Entity entity = this.mc.world.getEntityById(id);
         return entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof PersistentProjectileEntity || entity == this.mc.player;
      } catch (ArrayIndexOutOfBoundsException var3) {
         return false;
      }
   }

   private void doAttack(EndCrystalEntity crystal, boolean predict, @Nullable PlacementInfo surroundReplace) {
      boolean canSilentWeaknessSwitch = this.antiWeaknessSlot != -1
         && this.canBreakCrystal(this.mc.player.getInventory().getStack(this.antiWeaknessSlot), 0);
      boolean cantBreakRn = this.weak && !this.canBreakCrystal(this.mc.player.getMainHandStack(), this.ticksSinceLastBreak);
      if (cantBreakRn) {
         if (this.antiWeaknessSlot == -1) {
            return;
         }

         if (!canSilentWeaknessSwitch && this.switchMode.get() == SwitchMode.SILENT && this.silentMode.get() == SwapMode.NEW) {
            RandUtils.clickSlotPacket(this.mc.player.getInventory().selectedSlot + 36, this.antiWeaknessSlot, SlotActionType.SWAP);
         } else {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.antiWeaknessSlot));
         }

         if (!canSilentWeaknessSwitch
            || this.switchMode.get() != SwitchMode.SILENT
            || this.afterSwitchBreakPause.get() > 0 && this.silentMode.get() == SwapMode.OLD) {
            this.mc.player.getInventory().selectedSlot = this.antiWeaknessSlot;
            return;
         }
      }

      if (((Type)this.rotationMode.get()).breakTrue()) {
         if (this.hasRotatedThisTick || this.yawstep.get() != 180 && !this.validRotation(crystal.getPos())) {
            if (this.rotationTarget == null) {
               this.rotationTarget = crystal.getPos();
            }
         } else {
            Rotations.rotate(
               Rotations.getYaw(crystal),
               Rotations.getPitch(crystal, (Target)this.breakRotationMode.get()),
               85,
               () -> this.attackCrystal(crystal, predict, cantBreakRn, surroundReplace)
            );
            this.hasRotatedThisTick = true;
         }
      } else {
         this.attackCrystal(crystal, predict, cantBreakRn, surroundReplace);
      }
   }

   private void attackCrystal(EndCrystalEntity crystal, boolean predict, boolean shouldAntiWeakness, @Nullable PlacementInfo surroundReplace) {
      this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
      RandUtils.swing(this.swing.get(), this.getHand());
      if (shouldAntiWeakness && this.switchMode.get() == SwitchMode.SILENT) {
         if (this.silentMode.get() == SwapMode.OLD) {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
         } else {
            RandUtils.clickSlotPacket(this.mc.player.getInventory().selectedSlot + 36, this.antiWeaknessSlot, SlotActionType.SWAP);
         }
      }

      this.crystalsToRemove.put(crystal.getId(), System.currentTimeMillis());
      int tries = this.attemptedBreaks.getOrDefault(crystal.getId(), 0);
      this.attemptedBreaks.put(crystal.getId(), tries + 1);
      if (surroundReplace != null && this.placeDelayLeft < 1) {
         this.place(surroundReplace, ((Type)this.rotationMode.get()).placeTrue());
      }

      if (!predict) {
         this.places = 0;
         this.lastPredictedId = 0;
      }

      if (tries == 0) {
         for(Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && !(crystal.distanceTo(entity) > 11.0F)) {
               this.crystalsToRemove.put(entity.getId(), System.currentTimeMillis());
            }
         }

         if (this.sequential.get() && this.canPlace()) {
            if (this.threadMode.get() == Threading.THREADED && !this.executor.isShutdown() && !this.executor.isTerminated()) {
               this.executor.submit(this::doPlace);
            } else {
               this.doPlace();
            }
         }
      }

      ++this.breaks;
      this.breakDelayLeft = this.breakDelay.get();
      this.sinceLastActionTimer.reset();
      if (this.debug.get()) {
         if (this.debugActualDamage.get() && tries == 0) {
            float selfDamage = this.applyHurtTimeToDamage(
               this.mc.player, DamageCalcUtils.explosionDamage(this.mc.player, crystal.getPos(), 6), false, false
            );
            if (TextUtils.round(selfDamage, 4) > 0.0) {
               this.info("calculated damage: " + TextUtils.round(selfDamage, 4), new Object[0]);
            }
         }

         if (this.debugAttackedId.get()) {
            this.info("id: " + crystal.getId(), new Object[0]);
         }
      }
   }

   private boolean shouldNotBreak(EndCrystalEntity crystal, boolean inhibit, boolean checkRange) {
      if (crystal.isRemoved() || !crystal.isAlive()) {
         return true;
      } else if (checkRange && this.notInBreakRange(crystal)) {
         return true;
      } else if (inhibit && this.crystalsToRemove.containsKey(crystal.getId())) {
         return true;
      } else if (this.attemptedBreaks.getOrDefault(crystal.getId(), 0) >= this.breakAttempts.get()) {
         if (this.retryDelayLeft < 0) {
            this.retryDelayLeft = this.retryDelay.get();
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean notInBreakRange(EndCrystalEntity crystal) {
      return AntiCheatHelper.outOfHitRange(
         crystal,
         Origin.NCP,
         this.breakRange.get(),
         this.wallsRanges.get() ? (Double)this.breakWallsRange.get() : (Double)this.breakRange.get(),
         this.precision.get()
      );
   }

   private boolean unsafeBreak(LivingEntity entity, Vec3d crystalPos, double targetDamage) {
      float selfDamage = this.applyHurtTimeToDamage(
         entity,
         DamageCalcUtils.explosionDamage(
            entity,
            crystalPos,
            entity != this.mc.player && this.predict.get(),
            this.predictTicks.get(),
            this.antiStepOffset.get(),
            this.ignoreTerrain.get(),
            false,
            true,
            6
         ),
         false,
         false
      );
      if (this.noSuicideBreak.get() && (double)(EntityUtils.getTotalHealth((PlayerEntity)entity) - selfDamage) < 0.5) {
         return true;
      } else if ((double)selfDamage > this.maxBreakDamage.get()) {
         if (this.breakRatio.get() == 0.0) {
            return true;
         } else {
            return targetDamage / (double)selfDamage < this.breakRatio.get();
         }
      } else {
         return false;
      }
   }

   private boolean pauseBreak() {
      if (this.breaks >= this.maxBreaksPerSecond.get()) {
         return true;
      } else if (!this.switchTimer.passedMillis((long)((Integer)this.afterSwitchBreakPause.get()).intValue())) {
         return true;
      } else if (((Type)this.pauseMode.get()).breakTrue() && PlayerUtils.shouldPause(this.pauseOnMine.get(), this.pauseOnEat.get(), this.pauseOnEat.get())) {
         return true;
      } else {
         this.weak = false;
         if (this.antiWeakness.get()) {
            Map<StatusEffect, StatusEffectInstance> effects = this.mc.player.getActiveStatusEffects();
            StatusEffectInstance weakness = (StatusEffectInstance)effects.get(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = (StatusEffectInstance)effects.get(StatusEffects.STRENGTH);
            if (weakness != null && (strength == null || weakness.getAmplifier() >= strength.getAmplifier())) {
               this.weak = true;
               this.antiWeaknessSlot = InvUtils.find(stack -> this.canBreakCrystal(stack, 0), 0, 8).slot();
               if (this.antiWeaknessSlot == -1) {
                  this.antiWeaknessSlot = InvUtils.find(stack -> this.canBreakCrystal(stack, this.ticksSinceLastBreak), 0, 8).slot();
               }

               if (PlayerUtils.shouldPause(false, this.pauseMode.get() != Type.NONE && this.pauseOnEat.get(), false)) {
                  return true;
               }

               return this.antiWeaknessSlot == -1;
            }
         }

         return false;
      }
   }

   private boolean shouldFacePlace(LivingEntity target) {
      if (!this.facePlace.get()) {
         return false;
      } else if (((Keybind)this.forceFacePlace.get()).isPressed() && this.mc.currentScreen == null) {
         return true;
      } else if (!(target instanceof PlayerEntity)) {
         return false;
      } else {
         if (this.facePlacePause.get()) {
            if (PlayerUtils.shouldPause(this.facePlacePauseMine.get(), this.facePlacePauseEat.get(), this.facePlacePauseEat.get())) {
               return false;
            }

            if (this.pauseFacePlaceCev.get() && Modules.get().isActive(AutoFunnyCrystal.class)) {
               return false;
            }
         }

         if (!this.facePlaceSelf.get() && this.mc.player.distanceTo(target) < 1.0F) {
            return false;
         } else if (!this.pauseSword.get()
            || !(this.mc.player.getMainHandStack().getItem() instanceof SwordItem)
               && !(this.mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
            if (EntityUtils.getTotalHealth((PlayerEntity)target) <= (float)((Integer)this.facePlaceHealth.get()).intValue()) {
               return true;
            } else if (!this.facePlaceHole.get() || !UtilsPlus.isSurrounded(target, true, this.ignoreTerrain.get()) && !UtilsPlus.isBurrowed(target)) {
               for(ItemStack itemStack : target.getArmorItems()) {
                  if (itemStack != null && !itemStack.isEmpty()) {
                     if (RandUtils.durabilityPercentage(itemStack) <= (double)((Integer)this.facePlaceDurability.get()).intValue()) {
                        return true;
                     }
                  } else if (this.facePlaceArmor.get()) {
                     return true;
                  }
               }

               return false;
            } else {
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private Hand getHand() {
      return this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
            && this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL
         ? Hand.OFF_HAND
         : Hand.MAIN_HAND;
   }

   private boolean canBreakCrystal(ItemStack stack, int timeSinceLastCooldownReset) {
      double damage = this.mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

      for(EntityAttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
         damage += modifier.getValue();
      }

      float enchantDamage = EnchantmentHelper.getAttackDamage(stack, EntityGroup.DEFAULT);

      for(StatusEffectInstance instance : this.mc.player.getStatusEffects()) {
         if (instance.getEffectType() == StatusEffects.STRENGTH) {
            damage += (double)(3 * (instance.getAmplifier() + 1));
         } else if (instance.getEffectType() == StatusEffects.WEAKNESS) {
            damage -= (double)(4 * (instance.getAmplifier() + 1));
            if (timeSinceLastCooldownReset == 0 && enchantDamage == 0.0F) {
               return false;
            }
         }
      }

      if (damage < 0.0) {
         damage = 0.0;
      }

      double progress = 1.0;

      for(EntityAttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
         progress = MathHelper.clamp(((double)timeSinceLastCooldownReset + 0.5) / (1.0 / (4.0 + modifier.getValue()) * 20.0), 0.0, 1.0);
      }

      damage *= 0.2F + progress * progress * 0.8F;
      damage += (double)enchantDamage;
      return damage > 0.0;
   }

   private void handleHurtTime() {
      long ticksBehind = Math.round((double)this.getPing() / 50.0);
      if ((long)this.mc.player.timeUntilRegen - ticksBehind <= 10L) {
         this.lastTakenDamages.put(this.mc.player.getId(), 0.0F);
      }

      for(PlayerEntity friend : this.friends) {
         if ((long)friend.timeUntilRegen - ticksBehind <= 10L) {
            this.lastTakenDamages.put(friend.getId(), 0.0F);
         }
      }

      for(LivingEntity target : this.targets) {
         if ((long)target.timeUntilRegen - ticksBehind <= 10L) {
            this.lastTakenDamages.put(target.getId(), 0.0F);
         }
      }
   }

   private boolean intersectsWithEntity(BlockPos blockPos, boolean support, int idToIgnore) {
      if (this.getState(blockPos.up()).isAir()
         && !this.mc.world.getBlockState(blockPos.up()).isAir()
         && idToIgnore != -69) {
         return true;
      } else {
         int x = blockPos.getX();
         int y = blockPos.getY();
         int z = blockPos.getZ();
         Box box = new Box((double)x, (double)(y + 1), (double)z, (double)(x + 1), (double)(y + (this.ccEntities.get() ? 2 : 3)), (double)(z + 1));
         Box supportBox = new Box(blockPos);

         for(Entity entity : this.mc.world.getEntities()) {
            if (entity != this.mc.player && entity instanceof PlayerEntity player && !entity.isRemoved() && !(player.getHealth() <= 0.0F)) {
               if (entity.isSpectator()) {
                  return false;
               }

               if (!(entity.getBlockPos().getSquaredDistance(blockPos) > 16.0)) {
                  Box playerBox = player.getBoundingBox();
                  if (this.predict.get()) {
                     playerBox = PlayerUtils2.predictBox(player, this.predictTicks.get(), this.antiStepOffset.get());
                  }

                  if (playerBox.intersects(box) || support && playerBox.intersects(supportBox)) {
                     return true;
                  }
               }
            }
         }

         if (EntityUtils.intersectsWithEntity(box, entityx -> {
            if (entityx.getId() == idToIgnore) {
               return false;
            } else if (entityx.isRemoved()) {
               return false;
            } else if (entityx instanceof PlayerEntity && entityx != this.mc.player) {
               return false;
            } else {
               if (entityx instanceof LivingEntity living && living.getHealth() <= 0.0F) {
                  return false;
               }

               if (entityx.isSpectator()) {
                  return false;
               } else if (idToIgnore != -69 || !(entityx instanceof EndCrystalEntity) && !(entityx instanceof ItemEntity)) {
                  return !this.crystalsToRemove.containsKey(entityx.getId());
               } else {
                  return false;
               }
            }
         })) {
            return true;
         } else {
            return !support ? false : EntityUtils.intersectsWithEntity(supportBox, entityx -> {
               if (entityx.getId() == idToIgnore) {
                  return false;
               } else if (entityx.isRemoved()) {
                  return false;
               } else if (!entityx.canHit()) {
                  return false;
               } else if (entityx instanceof PlayerEntity && entityx != this.mc.player) {
                  return false;
               } else {
                  if (entityx instanceof LivingEntity living && living.getHealth() <= 0.0F) {
                     return false;
                  }

                  if (entityx.isSpectator()) {
                     return false;
                  } else {
                     return !this.crystalsToRemove.containsKey(entityx.getId());
                  }
               }
            });
         }
      }
   }

   private int getPing() {
      return PingUtils.enabled.get() ? (int)((double)PingUtils.getPing() * 1.1) : 100;
   }

   @EventHandler
   private void onPacketSent(Sent event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
         this.switchTimer.reset();
         this.ticksSinceLastBreak = 0;
      } else {
         Packet var4 = event.packet;
         if (var4 instanceof PlayerInteractItemC2SPacket packet) {
            if (packet.getHand() == Hand.OFF_HAND) {
               return;
            }

            if (!((Type)this.pauseMode.get()).placeTrue()) {
               return;
            }

            if (!PlayerUtils.shouldPause(false, this.pauseOnEat.get(), this.pauseOnEat.get())) {
               return;
            }

            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
         } else if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            this.ticksSinceLastBreak = 0;
         } else {
            var4 = event.packet;
            if (var4 instanceof PlayerMoveC2SPacket packet) {
               this.serverYaw = packet.getYaw(this.serverYaw);
            }
         }
      }
   }

   public void onActivate() {
      this.executor = Executors.newSingleThreadExecutor();
      this.playerPos = null;
      this.weak = false;
      this.breaks = 0;
      this.places = 0;
      this.ticksSinceLastBreak = 0;
      this.lastBlock = null;
      this.breakTimer.reset();
      this.sinceLastActionTimer.reset();
      this.switchTimer.setMs((long)((Integer)this.afterSwitchBreakPause.get()).intValue());
      this.placeDelayLeft = 0;
      this.breakDelayLeft = 0;
      this.supportDelayLeft = 0;
      this.retryDelayLeft = 0;
      this.lastId = 0;
      this.lastPredictedId = 0;
      this.lastPlaceTarget = null;
      this.friends.clear();
      this.attemptedBreaks.clear();
      this.lastTakenDamages.clear();
      this.targets.clear();
      this.crystalsToRemove.clear();
      this.crystalAliveMap.clear();
      this.pendingPlacements.clear();
      this.serverYaw = this.mc.player.getYaw();
      this.rotationTarget = null;
      synchronized(this.renderBlocks) {
         this.renderBlocks.clear();
      }
   }

   public void onDeactivate() {
      this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
      if (this.executor != null && !this.executor.isShutdown()) {
         this.executor.shutdownNow();
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get()) {
         synchronized(this.renderBlocks) {
            if (this.slide.get() && this.renderBlocks.size() > 1) {
               RenderBlock block0 = this.renderBlocks.get(0);
               double xPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getX() - block0.pos.getX()) * this.slideProgress + (float)block0.pos.getX()
               );
               double yPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getY() - block0.pos.getY()) * this.slideProgress + (float)block0.pos.getY()
               );
               double zPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getZ() - block0.pos.getZ()) * this.slideProgress + (float)block0.pos.getZ()
               );
               RenderBlock.complexRender(
                  event,
                  xPos,
                  yPos,
                  zPos,
                  null,
                  (SettingColor)this.sideColor.get(),
                  (SettingColor)this.lineColor.get(),
                  (ShapeMode)this.shapeMode.get(),
                  this.fade.get(),
                  block0.ticks,
                  this.renderTime.get(),
                  this.beforeFadeDelay.get(),
                  false,
                  (RenderShape)this.renderShape.get(),
                  this.width.get(),
                  this.height.get(),
                  this.yOffset.get(),
                  this.weirdOffset.get(),
                  this.shrink.get()
               );
               this.slideProgress = (float)((double)this.slideProgress + (double)((Integer)this.slideSpeed.get()).intValue() / 100.0);
               if (this.slideProgress > 1.0F) {
                  this.slideProgress = 1.0F;
                  this.renderBlocks.set(0, this.renderBlocks.remove(1));
               }
            } else {
               for(RenderBlock block : this.renderBlocks) {
                  block.complexRender(
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
                     this.weirdOffset.get(),
                     this.shrink.get()
                  );
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.render.get() && this.renderDamage.get()) {
         int preA = ((SettingColor)this.damageColor.get()).a;
         int preSelfA = ((SettingColor)this.selfDamageColor.get()).a;
         synchronized(this.renderBlocks) {
            if (this.slide.get() && this.renderBlocks.size() > 1) {
               RenderBlock damage0 = this.renderBlocks.get(0);
               double xPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getX() - damage0.pos.getX()) * this.slideProgress + (float)damage0.pos.getX()
               );
               double yPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getY() - damage0.pos.getY()) * this.slideProgress + (float)damage0.pos.getY()
               );
               double zPos = (double)(
                  (float)(this.renderBlocks.get(1).pos.getZ() - damage0.pos.getZ()) * this.slideProgress + (float)damage0.pos.getZ()
               );
               Vec3 pos = new Vec3(xPos + 0.5, yPos + this.yOffset.get() + (this.renderSelfDamage.get() ? 0.6667 : 0.5), zPos + 0.5);
               float factor = this.shrink.get() && !(damage0.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get()))
                  ? damage0.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                  : 1.0F;
               if (NametagUtils.to2D(pos, this.damageScale.get() * (double)factor)) {
                  TextRenderer renderer = TextRenderer.get();
                  NametagUtils.begin(pos);
                  renderer.begin((double)factor, false, true);
                  if (this.fade.get()) {
                     SettingColor var24 = (SettingColor)this.damageColor.get();
                     var24.a = (int)(
                        (float)var24.a
                           * (
                              damage0.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get())
                                 ? 1.0F
                                 : damage0.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                           )
                     );
                     var24 = (SettingColor)this.selfDamageColor.get();
                     var24.a = (int)(
                        (float)var24.a
                           * (
                              damage0.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get())
                                 ? 1.0F
                                 : damage0.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                           )
                     );
                  }

                  String damageText = String.valueOf(TextUtils.round(damage0.damage, this.roundDamage.get()));
                  renderer.render(damageText, -renderer.getWidth(damageText) * 0.5, -renderer.getHeight(true) * 0.5, (Color)this.damageColor.get(), true);
                  if (this.renderSelfDamage.get()) {
                     String selfDamageText = String.valueOf(TextUtils.round(damage0.selfDamage, this.roundDamage.get()));
                     renderer.render(
                        selfDamageText, -renderer.getWidth(selfDamageText) * 0.5, renderer.getHeight(true) * 0.5, (Color)this.selfDamageColor.get(), true
                     );
                  }

                  renderer.end();
                  NametagUtils.end();
                  ((SettingColor)this.damageColor.get()).a = preA;
                  ((SettingColor)this.selfDamageColor.get()).a = preSelfA;
               }
            } else {
               for(RenderBlock damage : this.renderBlocks) {
                  if (!(damage.ticks < (float)(this.renderTime.get() - this.damageRenderTime.get()))) {
                     float factor = this.shrink.get() && !(damage.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get()))
                        ? damage.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                        : 1.0F;
                     Vec3 pos = new Vec3(
                        (double)damage.pos.getX() + 0.5,
                        (double)damage.pos.getY() + this.yOffset.get() + (this.renderSelfDamage.get() ? 0.6667 : 0.5),
                        (double)damage.pos.getZ() + 0.5
                     );
                     if (NametagUtils.to2D(pos, this.damageScale.get() * (double)factor)) {
                        TextRenderer renderer = TextRenderer.get();
                        NametagUtils.begin(pos);
                        renderer.begin((double)factor, false, true);
                        if (this.fade.get()) {
                           SettingColor var10000 = (SettingColor)this.damageColor.get();
                           var10000.a = (int)(
                              (float)var10000.a
                                 * (
                                    damage.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get())
                                       ? 1.0F
                                       : damage.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                                 )
                           );
                           var10000 = (SettingColor)this.selfDamageColor.get();
                           var10000.a = (int)(
                              (float)var10000.a
                                 * (
                                    damage.ticks > (float)(this.damageRenderTime.get() - this.beforeFadeDelay.get())
                                       ? 1.0F
                                       : damage.ticks / (float)((Integer)this.damageRenderTime.get()).intValue()
                                 )
                           );
                        }

                        String damageText = String.valueOf(TextUtils.round(damage.damage, this.roundDamage.get()));
                        renderer.render(damageText, -renderer.getWidth(damageText) * 0.5, -renderer.getHeight(true) * 0.5, (Color)this.damageColor.get(), true);
                        if (this.renderSelfDamage.get()) {
                           String selfDamageText = String.valueOf(TextUtils.round(damage.selfDamage, this.roundDamage.get()));
                           renderer.render(
                              selfDamageText,
                              -renderer.getWidth(selfDamageText) * 0.5,
                              renderer.getHeight(true) * 0.5,
                              (Color)this.selfDamageColor.get(),
                              true
                           );
                        }

                        renderer.end();
                        NametagUtils.end();
                        ((SettingColor)this.damageColor.get()).a = preA;
                        ((SettingColor)this.selfDamageColor.get()).a = preSelfA;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public PlayerEntity getTarget() {
      if (this.sinceLastActionTimer.passedMillis(5000L)) {
         return null;
      } else {
         for(LivingEntity target : this.targets) {
            if (target instanceof PlayerEntity) {
               return (PlayerEntity)target;
            }
         }

         return null;
      }
   }

   public String getInfoString() {
      return this.targets.isEmpty() ? null : ((LivingEntity)this.targets.get(0)).getEntityName();
   }

   public static enum SyncMode {
      OFF("Off"),
      NORMAL("Normal"),
      STRICT("Strict");

      private final String title;

      private SyncMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
