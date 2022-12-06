package venomhack;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import venomhack.commands.HeadItemCommand;
import venomhack.commands.LogoutSpotsCommand;
import venomhack.modules.chat.ArmorMessage;
import venomhack.modules.chat.AutoCope;
import venomhack.modules.chat.AutoEz;
import venomhack.modules.chat.ChatControl;
import venomhack.modules.chat.Greeter;
import venomhack.modules.chat.LogDetection;
import venomhack.modules.chat.Notifier;
import venomhack.modules.combat.AutoAnchor;
import venomhack.modules.combat.AutoBed;
import venomhack.modules.combat.AutoChase;
import venomhack.modules.combat.AutoCity;
import venomhack.modules.combat.AutoCrystal;
import venomhack.modules.combat.AutoFunnyCrystal;
import venomhack.modules.combat.AutoTrap;
import venomhack.modules.combat.Burrow;
import venomhack.modules.combat.HoleFill;
import venomhack.modules.combat.Offhand;
import venomhack.modules.combat.OneShot;
import venomhack.modules.combat.PistonAura;
import venomhack.modules.combat.SelfTrap;
import venomhack.modules.combat.Surround;
import venomhack.modules.combat.TotemLog;
import venomhack.modules.hud.ItemHud;
import venomhack.modules.hud.StatsHud;
import venomhack.modules.misc.AutoCrafter;
import venomhack.modules.misc.AutoSort;
import venomhack.modules.misc.DiscordPresence;
import venomhack.modules.misc.PacketMine;
import venomhack.modules.misc.PacketPlace;
import venomhack.modules.misc.PearlPredict;
import venomhack.modules.misc.PingSpoof;
import venomhack.modules.movement.Anchor;
import venomhack.modules.movement.Moses;
import venomhack.modules.movement.PacketFly;
import venomhack.modules.movement.Scaffold;
import venomhack.modules.movement.speed.Speed;
import venomhack.modules.player.FloRida;
import venomhack.modules.player.XpThrower;
import venomhack.modules.render.BetterChams;
import venomhack.modules.render.BetterPops;
import venomhack.modules.render.BurrowEsp;
import venomhack.modules.render.DroppedItemsView;
import venomhack.modules.render.HoleEsp;
import venomhack.modules.render.KillEffects;
import venomhack.modules.render.LogoutSpotsRewrite;
import venomhack.modules.render.SoundEsp;
import venomhack.modules.render.TanukiOutline;
import venomhack.modules.world.AutoBamboo;
import venomhack.modules.world.AutoWatercube;
import venomhack.modules.world.EgapFinder;
import venomhack.modules.world.ItemDropper;
import venomhack.modules.world.ObsidianFarm;
import venomhack.modules.world.WaypointDeleter;
import venomhack.modules.world.villager_trader.VillagerTrader;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.PingUtils;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.Statistics;
import venomhack.utils.ThreadedUtils;

public class Venomhack420 extends MeteorAddon {
   public static final Logger LOG = LogManager.getLogger();
   public static final Category CATEGORY = new Category("Venomhack420", Items.COMMAND_BLOCK_MINECART.getDefaultStack());
   public static final HudGroup HUD_GROUP = new HudGroup("Venomhack420");
   public static final String VERSION = ((ModContainer)FabricLoader.getInstance().getModContainer("venomhack").get())
      .getMetadata()
      .getVersion()
      .getFriendlyString();
   public static final Statistics STATS = Statistics.get();

   public void onInitialize() {
      LOG.info("Initializing Venomhack420");
      DamageCalcUtils.init();
      ThreadedUtils.init();
      PlayerUtils2.init();
      PingUtils.init();
      this.initModules();
      MeteorClient.EVENT_BUS.subscribe(STATS);
      if (Config.get().customWindowTitle.get()) {
         MeteorClient.mc.getWindow().setTitle((String)Config.get().customWindowTitleText.get());
      }
   }

   private void initModules() {
      Modules modules = Modules.get();
      modules.add(new ArmorMessage());
      modules.add(new AutoEz());
      modules.add(new AutoCope());
      modules.add(new ChatControl());
      modules.add(new Greeter());
      modules.add(new LogDetection());
      modules.add(new Notifier());
      modules.add(new AutoAnchor());
      modules.add(new AutoBed());
      modules.add(new AutoCity());
      modules.add(new AutoCrystal());
      modules.add(new AutoFunnyCrystal());
      modules.add(new AutoTrap());
      modules.add(new Burrow());
      modules.add(new HoleFill());
      modules.add(new Offhand());
      modules.add(new OneShot());
      modules.add(new SelfTrap());
      modules.add(new Surround());
      modules.add(new TotemLog());
      modules.add(new AutoChase());
      modules.add(new PistonAura());
      modules.add(new AutoCrafter());
      modules.add(new AutoSort());
      modules.add(new DiscordPresence());
      modules.add(new PacketMine());
      modules.add(new PacketPlace());
      modules.add(new PearlPredict());
      modules.add(new PingSpoof());
      modules.add(new Anchor());
      modules.add(new Speed());
      modules.add(new Moses());
      modules.add(new PacketFly());
      modules.add(new Scaffold());
      modules.add(new FloRida());
      modules.add(new XpThrower());
      modules.add(new BetterChams());
      modules.add(new BetterPops());
      modules.add(new BurrowEsp());
      modules.add(new DroppedItemsView());
      modules.add(new HoleEsp());
      modules.add(new LogoutSpotsRewrite());
      modules.add(new SoundEsp());
      modules.add(new TanukiOutline());
      modules.add(new KillEffects());
      modules.add(new AutoBamboo());
      modules.add(new AutoWatercube());
      modules.add(new EgapFinder());
      modules.add(new ObsidianFarm());
      modules.add(new VillagerTrader());
      modules.add(new WaypointDeleter());
      modules.add(new ItemDropper());
      Commands cmds = Commands.get();
      cmds.add(new LogoutSpotsCommand());
      cmds.add(new HeadItemCommand());
      Hud hud = Hud.get();
      hud.register(ItemHud.INFO);
      hud.register(StatsHud.INFO);
   }

   public void onRegisterCategories() {
      Modules.registerCategory(CATEGORY);
   }

   public String getPackage() {
      return "venomhack";
   }
}
