package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.UtilsPlus;

public class SelfTrap extends ModuleHelper {
   private final SettingGroup sgProtect = this.group("Protect");
   private final SettingGroup sgAttack = this.group("Attack Crystals");
   private final SettingGroup sgExtra = this.group("Extra");
   private final Setting<SelfTrap.Mode> mode = this.setting("mode", "The way you are trapped.", SelfTrap.Mode.FULL);
   private final Setting<Boolean> attack = this.setting(
      "attack-crystals", "Attempts to break crystals placed in your selftrap.", Boolean.valueOf(true), this.sgAttack
   );
   private final Setting<Boolean> onlyFunny = this.setting(
      "only-funny", "Only attacks funny crystals.", Boolean.valueOf(false), this.sgAttack, this.attack::get
   );
   private final Setting<Integer> attackDelay = this.setting(
      "attack-delay", "The delay between attacking crystals", Integer.valueOf(0), this.sgAttack, this.attack::get, 0.0, 20.0
   );
   private final Setting<Boolean> swing = this.setting(
      "swing", "Whether or not to swing client side when attacking.", Boolean.valueOf(true), this.sgAttack, this.attack::get
   );
   private final Setting<Boolean> antiBed = this.setting(
      "anti-bed", "Attempts to quickly destroy beds in your selftrap.", Boolean.valueOf(true), this.sgProtect
   );
   private final Setting<Integer> hpThreshold = this.setting(
      "anti-bed-threshold", "At what health to stop breaking beds.", Integer.valueOf(0), this.sgProtect, 0.0, 36.0
   );
   private final Setting<Boolean> antiFunny = this.setting("anti-funny", "Attempts to replace funny crystals.", Boolean.valueOf(true), this.sgProtect);
   private final Setting<Boolean> tpDisable = this.setting("teleport-disable", "Disables when you teleport", Boolean.valueOf(true), this.sgExtra);
   private final Setting<Boolean> onlyHole = this.setting("only-in-hole", "Only plaves while you're in a hole.", Boolean.valueOf(false), this.sgExtra);
   private final Setting<Boolean> toggleOnMove = this.setting("toggle-on-move", "Toggles when you move.", Boolean.valueOf(true), this.sgExtra);
   private final List<BlockPos> positions = new ArrayList();
   BlockPos playerPos;
   int crystalDelay;

   public SelfTrap() {
      super(Categories.Combat, "self-trap-vh", "Surrounds your head with blocks.");
   }

   @EventHandler
   private void onTick(Pre event) {
      --this.crystalDelay;
      if (this.mc.player.isFallFlying()) {
         this.toggleWithInfo("You are flying, disabling.", new Object[0]);
      } else if (this.onlyHole.get() && !UtilsPlus.isSurrounded(this.mc.player, false, true)) {
         this.toggleWithInfo("Not in hole, disabling.", new Object[0]);
      } else {
         if (this.antiBed.get() && this.mc.player.getHealth() > (float)((Integer)this.hpThreshold.get()).intValue()) {
            this.breakBed(this.playerPos.up());
         }

         this.attack();
         this.place();
         this.positions.clear();
      }
   }

   @EventHandler
   private void onSend(Sent event) {
      if (this.tpDisable.get()) {
         if (event.packet instanceof TeleportConfirmC2SPacket) {
            this.toggleWithInfo("You teleported, disabling.", new Object[0]);
         }
      }
   }

   @EventHandler
   private void onReceive(Receive event) {
      if (this.antiFunny.get()) {
         Packet var3 = event.packet;
         if (var3 instanceof BlockBreakingProgressS2CPacket packet) {
            if (this.mc.world.getEntityById(packet.getEntityId()) == this.mc.player) {
               return;
            }

            if (!packet.getPos().equals(this.playerPos.up(2))) {
               return;
            }

            this.add(packet.getPos().up());
         }
      }
   }

   private FindItemResult findBlock() {
      FindItemResult item = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
      if (!item.found()) {
         item = InvUtils.findInHotbar(UtilsPlus::isGoodForSurround);
      }

      return item;
   }

   private void add(BlockPos pos) {
      this.positions.add(pos);
   }

   private void getPositions() {
      BlockPos up = this.playerPos.up();
      switch((SelfTrap.Mode)this.mode.get()) {
         case TOP:
            this.add(up.up());
            break;
         case FULL:
            this.add(up.up());
            this.add(up.north());
            this.add(up.east());
            this.add(up.south());
            this.add(up.west());
      }
   }

   private void place() {
      if (this.toggleOnMove.get() && this.mc.player.getBlockPos() != this.playerPos) {
         this.toggleWithInfo("You moved, disabling.", new Object[0]);
      } else {
         this.getPositions();
         FindItemResult result = this.findBlock();

         for(BlockPos position : this.positions) {
            if (this.mc.world.getBlockState(position).getBlock().equals(Blocks.AIR)) {
               BlockUtils2.justPlace(result, BlockUtils2.getPlaceResult(position, true, false), this.swing.get(), false, 0);
            }
         }
      }
   }

   private void attack() {
      if (this.attack.get()) {
         for(Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal
               && this.mc.world.getBlockState(this.playerPos.up(2)).getBlock().equals(Blocks.OBSIDIAN)) {
               BlockPos crystalPos = crystal.getBlockPos();
               if (crystalPos.equals(this.playerPos.up(3))) {
                  this.hitCrystal(crystal);
               } else if (this.onlyFunny.get()) {
                  continue;
               }

               if (crystalPos.equals(this.playerPos.up().north())
                  || crystalPos.equals(this.playerPos.up().east())
                  || crystalPos.equals(this.playerPos.up().south())
                  || crystalPos.equals(this.playerPos.up().west())) {
                  this.hitCrystal(crystal);
               }
            }
         }
      }
   }

   private void hitCrystal(EndCrystalEntity crystal) {
      if (this.crystalDelay <= 0) {
         this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
         if (this.swing.get()) {
            this.mc.player.swingHand(Hand.MAIN_HAND);
         }

         this.crystalDelay = this.attackDelay.get();
      }
   }

   private void breakBed(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      if (state.getBlock() instanceof BedBlock) {
         this.mc
            .player
            .networkHandler
            .sendPacket(
               new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), BlockUtils2.getClosestDirection(pos, false), pos, false), 0)
            );
      }
   }

   public void onActivate() {
      this.playerPos = this.mc.player.getBlockPos();
   }

   public void onDeactivate() {
      this.positions.clear();
   }

   public static enum Mode {
      TOP,
      FULL;
   }
}
