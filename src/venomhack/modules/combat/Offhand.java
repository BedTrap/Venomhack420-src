package venomhack.modules.combat;

import com.google.common.util.concurrent.AtomicDouble;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NoInteract;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.util.hit.HitResult.class_240;
import venomhack.Venomhack420;
import venomhack.enums.GapMode;
import venomhack.mixinInterface.INoInteract;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.UtilsPlus;

public class Offhand extends ModuleHelper {
   private final SettingGroup sgOffhand = this.group("Offhand");
   private final SettingGroup sgSword = this.group("Sword Settings");
   private final Setting<Integer> health = this.setting("health", "The health to hold a totem at.", Integer.valueOf(13), this.sgGeneral, 0.0, 36.0);
   private final Setting<Integer> delay = this.setting("delay", "The ticks between slot movements.", Integer.valueOf(0), this.sgGeneral, 0.0, 10.0);
   private final Setting<Boolean> elytra = this.setting("elytra", "Will always hold a totem when flying with elytra.", Boolean.valueOf(true), this.sgGeneral);
   private final Setting<Boolean> hotbar = this.setting("hotbar", "Will take items from your hotbar aswell.", Boolean.valueOf(true), this.sgGeneral);
   public final Setting<Offhand.Mode> mode = this.setting(
      "mode", "Changes what item that will go into your offhand.", Offhand.Mode.CRYSTAL, this.sgOffhand, null, mode -> this.currentMode = mode
   );
   public final Setting<GapMode> gapMode = this.setting("gap-mode", "Defines which gaps to use.", GapMode.EGAP, this.sgOffhand);
   private final Setting<Boolean> click = this.setting(
      "right-click-gap", "Puts a gap in your offhand when pressing right click", Boolean.valueOf(true), this.sgOffhand
   );
   private final Setting<Boolean> crystalMine = this.setting(
      "crystal-on-mine", "Holds a crystal while you are mining.", Boolean.valueOf(false), this.sgOffhand
   );
   private final Setting<Boolean> holeMineCrystal = this.setting(
      "only-when-safe",
      "Will only hold a crystal while mining if you are surrounded or burrowed.",
      Boolean.valueOf(true),
      this.sgOffhand,
      this.crystalMine::get
   );
   private final Setting<Boolean> sword = this.setting(
      "sword-gap", "Changes the mode to gap if you are holding a sword in your main hand.", Boolean.valueOf(false), this.sgSword
   );
   private final Setting<Boolean> hole = this.setting("hole-only", "Only activates when in a hole or when burrowed.", Boolean.valueOf(false), this.sgSword);
   private final Setting<Boolean> shield = this.setting(
      "offhand-shield", "Changes the mode to Shield if you are holding a sword in your main hand.", Boolean.valueOf(false), this.sgSword
   );
   private final Setting<Boolean> strength = this.setting(
      "offhand-strength", "Changes the mode to a strength potion if you are holding a sword in your main hand.", Boolean.valueOf(false), this.sgSword
   );
   private final Setting<Boolean> shieldStr = this.setting(
      "shield-strength", "Only hold a shield when you have the Strength effect.", Boolean.valueOf(false), this.sgSword, this.shield::get
   );
   private final Setting<Integer> shieldHealth = this.setting(
      "shield-health", "Minimum health for you to hold a shield.", Integer.valueOf(30), this.sgSword, this.shield::get, 0.0, 36.0
   );
   private final Setting<Integer> strengthHealth = this.setting(
      "strength-health", "Minimum health for you to hold a strength potion.", Integer.valueOf(25), this.sgSword, this.strength::get, 0.0, 36.0
   );
   private static boolean isClicking = false;
   private boolean sentMessage;
   private boolean locked;
   private Offhand.Mode currentMode;
   private Item mItem;
   private Item oItem;
   private int ticks;
   private int previousSlot;

   public Offhand() {
      super(Venomhack420.CATEGORY, "offhand-vh", "Auto totem and offhand.");
   }

   public void onActivate() {
      this.previousSlot = -1;
      this.sentMessage = false;
      this.locked = false;
   }

   @EventHandler(
      priority = 201
   )
   private void onTick(Pre event) {
      FindItemResult totemResult = InvUtils.find(new Item[]{Items.TOTEM_OF_UNDYING});
      int totems = totemResult.count();
      if (totems <= 0) {
         this.locked = false;
      } else if (this.ticks >= this.delay.get()) {
         double playerHealth = (double)EntityUtils.getTotalHealth(this.mc.player);
         this.locked = playerHealth <= (double)((Integer)this.health.get()).intValue()
            || playerHealth - PlayerUtils.possibleHealthReductions(true, true) < 0.5
            || this.elytra.get()
               && this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
               && this.mc.player.isFallFlying();
         this.ticks = 0;
         if (this.locked) {
            if (this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               InvUtils.move().from(totemResult.slot()).toOffhand();
            }
         } else if (PlayerUtils.getDimension() != Dimension.Nether) {
            AtomicDouble anchorDamage = new AtomicDouble();
            BlockIterator.register(
               6,
               6,
               (blockPos, blockState) -> {
                  if (blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
                     Vec3d vec = new Vec3d(
                        (double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5
                     );
                     anchorDamage.set(Math.max((double)DamageCalcUtils.explosionDamage(this.mc.player, vec, 5), anchorDamage.get()));
                  }
               }
            );
            BlockIterator.after(() -> {
               if (playerHealth - anchorDamage.get() < 0.5) {
                  this.locked = true;
                  if (this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                     InvUtils.move().from(totemResult.slot()).toOffhand();
                  }
               }
            });
         }
      } else {
         ++this.ticks;
      }
   }

   @EventHandler
   private void onTick(Post event) {
      this.currentMode = (Offhand.Mode)this.mode.get();
      this.mItem = this.mc.player.getMainHandStack().getItem();
      this.oItem = this.mc.player.getOffHandStack().getItem();
      if (this.mc.currentScreen == null || this.mc.currentScreen instanceof InventoryScreen || this.mc.currentScreen instanceof WidgetScreen) {
         if (!this.locked) {
            if (this.mc.interactionManager.isBreakingBlock() && this.crystalMine.get() && (!this.holeMineCrystal.get() || UtilsPlus.isSafe(this.mc.player))) {
               this.currentMode = Offhand.Mode.CRYSTAL;
            }

            if ((this.mItem instanceof SwordItem || this.mItem instanceof AxeItem)
               && (!this.hole.get() || UtilsPlus.isSurrounded(this.mc.player, true, true) || UtilsPlus.isBurrowed(this.mc.player))) {
               float health = this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount();
               boolean str = this.mc.player.hasStatusEffect(StatusEffects.STRENGTH);
               int result = this.findSlot(this.getItem());
               int pots = InvUtils.find(
                     itemStack -> itemStack.getItem() == PotionUtil.setPotion(Items.POTION.getDefaultStack(), Potions.STRENGTH).getItem(),
                     this.hotbar.get() ? 0 : 9,
                     35
                  )
                  .slot();
               if (this.shield.get()
                  && (health >= (float)((Integer)this.shieldHealth.get()).intValue() || !this.strength.get() && !this.sword.get())
                  && (!this.shieldStr.get() || str || pots == -1)) {
                  this.currentMode = Offhand.Mode.SHIELD;
                  result = this.findSlot(this.getItem());
               }

               if (this.strength.get()
                  && !str
                  && (
                     (health < (float)((Integer)this.shieldHealth.get()).intValue() || !this.shield.get())
                           && (health >= (float)((Integer)this.strengthHealth.get()).intValue() || !this.sword.get())
                        || this.shieldStr.get()
                        || result == -1
                  )) {
                  this.currentMode = Offhand.Mode.POTION;
                  result = this.findSlot(this.getItem());
               }

               if (this.sword.get()
                  && (
                     (
                              health < (float)((Integer)this.shieldHealth.get()).intValue()
                                 || !this.shield.get()
                                 || !this.strength.get() && this.shield.get() && this.shieldStr.get() && !str
                           )
                           && (!this.strength.get() || health < (float)((Integer)this.strengthHealth.get()).intValue() || str)
                        || result == -1
                  )) {
                  this.currentMode = Offhand.Mode.GAP;
               }
            }

            if (this.oItem != this.getItem() && !isClicking) {
               int result = this.findSlot(this.getItem());
               if (result == -1 && this.oItem != this.getItem()) {
                  if (this.currentMode != this.mode.get()) {
                     this.currentMode = (Offhand.Mode)this.mode.get();
                     if (this.oItem != this.getItem()) {
                        result = this.findSlot(this.getItem());
                        if (result != -1) {
                           this.doMove(result);
                           return;
                        }
                     }
                  }

                  if (!this.sentMessage) {
                     this.warning(4242, "Chosen item not found.", new Object[0]);
                     this.sentMessage = true;
                  }

                  return;
               }

               this.doMove(result);
               this.sentMessage = false;
            }
         }
      }
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (!this.locked) {
         if (event.action == KeyAction.Press && event.button == 1) {
            if (this.mc.currentScreen == null && this.click.get()) {
               if (this.mItem instanceof ToolItem) {
                  if (!this.mc.player.isSneaking() && this.mc.crosshairTarget.getType() == class_240.BLOCK) {
                     BlockHitResult hit = (BlockHitResult)this.mc.crosshairTarget;
                     NoInteract cancel = (NoInteract)Modules.get().get(NoInteract.class);
                     if (BlockUtils2.clickableBlock(this.mc.world.getBlockState(hit.getBlockPos()), hit, Hand.MAIN_HAND)
                        && (!cancel.isActive() || ((INoInteract)cancel).shouldInteract(hit, Hand.MAIN_HAND))) {
                        return;
                     }
                  }

                  if (!this.strength.get()
                     || !(this.oItem instanceof PotionItem)
                     || !(PlayerUtils.getTotalHealth() >= (double)((Integer)this.strengthHealth.get()).intValue())
                     || this.hole.get()
                     || !UtilsPlus.isSafe(this.mc.player)) {
                     if (!this.shield.get()
                        || !(this.oItem instanceof ShieldItem)
                        || !(PlayerUtils.getTotalHealth() >= (double)((Integer)this.shieldHealth.get()).intValue())
                        || this.hole.get()
                        || !UtilsPlus.isSafe(this.mc.player)) {
                        this.currentMode = Offhand.Mode.GAP;
                        isClicking = true;
                        Item item = this.getItem();
                        if (this.oItem != item) {
                           int result = this.findSlot(item);
                           if (result == -1 && !this.sentMessage) {
                              this.warning(4242, "Can't find " + ((GapMode)this.gapMode.get()).title + "s!", new Object[0]);
                              this.sentMessage = true;
                           }

                           this.previousSlot = result;
                           this.doMove(result);
                           this.sentMessage = false;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = 200
   )
   private void onReceivePacket(Receive event) {
      Packet entity = event.packet;
      if (entity instanceof EntityStatusS2CPacket p) {
         if (p.getStatus() == 35) {
            Entity entityx = p.getEntity(this.mc.world);
            if (entityx != null && entityx.equals(this.mc.player)) {
               this.ticks = 0;
            }
         }
      }
   }

   @EventHandler
   private void onMouseButtonRelease(MouseButtonEvent event) {
      if (event.action == KeyAction.Release && event.button == 1) {
         if (this.previousSlot != -1) {
            InvUtils.move().fromOffhand().toHotbar(this.previousSlot);
            this.previousSlot = -1;
            isClicking = false;
         }
      }
   }

   private Item getItem() {
      return switch(this.currentMode) {
         case GAP -> {
            switch((GapMode)this.gapMode.get()) {
               case EGAP:
                  yield Items.ENCHANTED_GOLDEN_APPLE;
               case GAP:
                  yield Items.GOLDEN_APPLE;
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case EXP -> Items.EXPERIENCE_BOTTLE;
         case CRYSTAL -> Items.END_CRYSTAL;
         case SHIELD -> Items.SHIELD;
         case TOTEM -> Items.TOTEM_OF_UNDYING;
         case POTION -> PotionUtil.setPotion(Items.POTION.getDefaultStack(), Potions.STRENGTH).getItem();
      };
   }

   private void doMove(int slot) {
      InvUtils.move().from(slot).toOffhand();
   }

   private int findSlot(Item item) {
      for(int i = 9; i < this.mc.player.getInventory().size(); ++i) {
         if (this.mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return InvUtils.find(itemStack -> itemStack.getItem() == item, this.hotbar.get() ? 0 : 9, 35).slot();
   }

   public String getInfoString() {
      return ((Offhand.Mode)this.mode.get()).name();
   }

   public static enum Mode {
      GAP("Golden Apple"),
      EXP("Experience Bottle"),
      CRYSTAL("End Crystal"),
      SHIELD("Shield"),
      POTION("Potion"),
      TOTEM("Totem");

      private final String title;

      private Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
