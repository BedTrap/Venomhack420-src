package venomhack.modules.world.villager_trader;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class VillagerTrader extends ModuleHelper {
   private final SettingGroup sgDelay = this.group("Delay");
   private final Setting<Double> tradeRange = this.setting("trade-range", "The maximum range the villager can be traded with.", Double.valueOf(5.0), 0.0, 6.0);
   private final Setting<Double> wallsRange = this.setting(
      "walls-range", "The maximum range the villager can be traded with through walls.", Double.valueOf(5.0), 0.0, 6.0
   );
   private final Setting<Boolean> ignoreWalls = this.setting("ignore-walls", "Whether or not to trade through walls.", Boolean.valueOf(true));
   private final Setting<Boolean> tradeAura = this.setting("aura", "Automatically opens the guis of nearby villagers to trade.", Boolean.valueOf(true));
   private final Setting<List<Item>> trades = this.setting(
      "selected-trades",
      "The items you wish to auto trade for.",
      this.sgGeneral,
      false,
      Item -> true,
      null,
      null,
      null,
      new Item[]{Items.ENCHANTED_BOOK}
   );
   private final Setting<Object2BooleanMap<EntityType<?>>> entities = this.setting(
      "entities", "Entities to trade with.", this.sgGeneral, true, null, null, null, new EntityType[]{EntityType.VILLAGER, EntityType.WANDERING_TRADER}
   );
   private final Setting<Integer> tradeDelay = this.setting(
      "trade-delay", "How many ticks to wait between opening guis.", Integer.valueOf(5), this.sgDelay, 0.0, 50.0
   );
   private final Setting<Integer> retradeDelay = this.setting(
      "cool-down", "How long in seconds it takes to trade again.", Integer.valueOf(60), this.sgDelay, 0.0, 300.0
   );
   private final List<Entity> entityList = new ArrayList();
   private final List<Entity> alreadyTradedVillagers = new ArrayList();
   private int hitDelayTimer;
   private int tradeRefreshTimer;

   public VillagerTrader() {
      super(Venomhack420.CATEGORY, "villager-trader", "Automates villager trading.");
   }

   public ArrayList<Item> getWantedTradeItems() {
      return new ArrayList((Collection)this.trades.get());
   }

   public BetterGuiMerchant initVillagerTrader(MerchantScreenHandler handler, PlayerInventory inv, Text title) {
      ArrayList<Item> wantedTrades = this.getWantedTradeItems();
      return new BetterGuiMerchant(handler, inv, title, wantedTrades);
   }

   public void onDeactivate() {
      this.hitDelayTimer = 0;
      this.tradeRefreshTimer = 0;
      this.alreadyTradedVillagers.clear();
      this.entityList.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player.isAlive() && !this.mc.player.isDead() && PlayerUtils.getGameMode() != GameMode.SPECTATOR && this.tradeAura.get()
         )
       {
         Vec3d pos = this.mc.player.getPos();
         double range = this.tradeRange.get();
         this.entityList.clear();

         for(Entity entity : this.mc.world.getEntities()) {
            this.entityList.add(entity);
         }

         TargetUtils.getList(this.entityList, entityx -> {
            if (!entityx.equals(this.mc.player) && !entityx.equals(this.mc.cameraEntity)) {
               if ((!(entityx instanceof LivingEntity) || !((LivingEntity)entityx).isDead()) && entityx.isAlive()) {
                  if (entityx.getPos().distanceTo(pos) > range) {
                     return false;
                  } else if (!((Object2BooleanMap)this.entities.get()).getBoolean(entityx.getType())) {
                     return false;
                  } else if (this.alreadyTraded(entityx)) {
                     return false;
                  } else {
                     if (this.ignoreWalls.get()) {
                        if (entityx.getPos().distanceTo(pos) > this.wallsRange.get()) {
                           return false;
                        }
                     } else if (!PlayerUtils.canSeeEntity(entityx)) {
                        return false;
                     }

                     if ((double)entityx.getBlockPos().getY() > pos.getY() + 1.0) {
                        return false;
                     } else {
                        return !(entityx instanceof VillagerEntity) || !((VillagerEntity)entityx).isBaby();
                     }
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }, SortPriority.LowestDistance, 1);
         if (this.delayCheck() && this.entityList.size() > 0) {
            this.entityList.forEach(this::trade);
         }

         this.resetTradedVillagers();
      }
   }

   private boolean alreadyTraded(Entity target) {
      return this.alreadyTradedVillagers.contains(target);
   }

   private boolean delayCheck() {
      if (this.hitDelayTimer >= 0) {
         --this.hitDelayTimer;
         return false;
      } else {
         this.hitDelayTimer = this.tradeDelay.get();
         return true;
      }
   }

   private void resetTradedVillagers() {
      if (this.tradeRefreshTimer >= 0) {
         --this.tradeRefreshTimer;
      } else {
         this.alreadyTradedVillagers.clear();
         this.tradeRefreshTimer = this.retradeDelay.get() * 20;
      }
   }

   private void trade(Entity target) {
      this.mc.interactionManager.interactEntity(this.mc.player, target, Hand.MAIN_HAND);
      this.mc.player.swingHand(Hand.MAIN_HAND);
      this.alreadyTradedVillagers.add(target);
   }
}
