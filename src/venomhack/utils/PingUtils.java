package venomhack.utils;

import java.util.Arrays;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.class_2800;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import venomhack.utils.customObjects.Timer;

public class PingUtils {
   public static SettingGroup sgPingUtils = Config.get().settings.createGroup("PingUtils");
   public static final Setting<Boolean> enabled = sgPingUtils.add(
      ((Builder)((Builder)((Builder)((Builder)new Builder().name("enable-calculations")).description("Enable/disable manual ping calculation"))
               .onChanged(value -> PingUtils.checkingPing = false))
            .defaultValue(true))
         .build()
   );
   private static final Setting<Integer> delay = sgPingUtils.add(
      ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder(
                        
                     )
                     .name("delay"))
                  .description("Delay between pings"))
               .visible(enabled::get))
            .defaultValue(2000))
         .min(0)
         .max(15000)
         .sliderRange(0, 15000)
         .build()
   );
   private static final Setting<PingUtils.Mode> mode = sgPingUtils.add(
      ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder(
                           
                        )
                        .name("type of ping"))
                     .description("Type of check, on MESSAGE with chat disabled delay does not matter"))
                  .visible(enabled::get))
               .defaultValue(PingUtils.Mode.STATISTICS))
            .onChanged(value -> PingUtils.checkingPing = false))
         .build()
   );
   private static final Setting<Boolean> updateEntry = sgPingUtils.add(
      ((Builder)((Builder)((Builder)((Builder)new Builder().name("update-entry")).description("Update ping in player list")).visible(enabled::get))
            .defaultValue(true))
         .build()
   );
   private static final Setting<Boolean> withTickrate = sgPingUtils.add(
      ((Builder)((Builder)((Builder)((Builder)new Builder().name("with-tps")).description("Account for TPS when calculating ping")).visible(enabled::get))
            .defaultValue(true))
         .build()
   );
   private static boolean checkingPing = false;
   private static int ping = 0;
   private static final Timer timer = new Timer();
   private static final int[] pastPingValues = new int[10];
   private static int nextIndex = 0;

   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(PingUtils.class);
   }

   @EventHandler
   private static void onWorldJoin(GameJoinedEvent event) {
      Arrays.fill(pastPingValues, -1);
      nextIndex = 0;
      ping = 0;
      timer.reset();
      checkingPing = false;
   }

   @EventHandler
   private static void onPacketReceive(Receive event) {
      if (Utils.canUpdate() && enabled.get()) {
         if (mode.get() == PingUtils.Mode.INTERACT) {
            Packet var2 = event.packet;
            if (var2 instanceof BlockUpdateS2CPacket packet && packet.getPos().equals(MeteorClient.mc.player.getBlockPos())) {
               updatePing();
               return;
            }
         }

         if (mode.get() == PingUtils.Mode.STATISTICS && event.packet instanceof StatisticsS2CPacket) {
            updatePing();
         }
      }
   }

   @EventHandler
   private static void onTick(Pre event) {
      if (Utils.canUpdate() && enabled.get()) {
         if (timer.passedMillis((long)((Integer)delay.get()).intValue()) && !checkingPing) {
            switch((PingUtils.Mode)mode.get()) {
               case STATISTICS:
                  requestStats();
                  break;
               case INTERACT:
                  sendInteract();
            }

            timer.reset();
            checkingPing = true;
         }
      }
   }

   private static void requestStats() {
      MeteorClient.mc.player.networkHandler.sendPacket(new ClientStatusC2SPacket(class_2800.REQUEST_STATS));
   }

   private static void sendInteract() {
      if (PlayerUtils.shouldPause(false, true, true)) {
         BlockPos pos = MeteorClient.mc.player.getBlockPos();
         BlockHitResult result = BlockUtils2.getPlaceResult(pos, true, false);
         boolean sneak = !MeteorClient.mc.player.isSneaking()
            && BlockUtils2.clickableBlock(MeteorClient.mc.world.getBlockState(pos), result, Hand.OFF_HAND);
         if (sneak) {
            MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.PRESS_SHIFT_KEY));
         }

         MeteorClient.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, result, 0));
         if (sneak) {
            MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.RELEASE_SHIFT_KEY));
         }
      }
   }

   private static void updatePlayerListPing() {
      if (MeteorClient.mc.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = MeteorClient.mc.getNetworkHandler().getPlayerListEntry(MeteorClient.mc.player.getUuid());
         if (playerListEntry != null) {
            playerListEntry.setLatency(ping);
         }
      }
   }

   private static void updatePing() {
      ping = (int)(withTickrate.get() ? (float)timer.millisPassed() * (TickRate.INSTANCE.getTickRate() / 20.0F) : (float)timer.millisPassed());
      pastPingValues[nextIndex] = ping;
      nextIndex = (nextIndex + 1) % pastPingValues.length;
      if (updateEntry.get()) {
         updatePlayerListPing();
      }

      checkingPing = false;
   }

   public static int getPing() {
      return ping;
   }

   public static int getAveragePing() {
      int numPings = 0;
      int pingSum = 0;

      for(int ping : pastPingValues) {
         if (ping != -1) {
            pingSum += ping;
            ++numPings;
         }
      }

      return numPings < 10 ? getPing() : pingSum / numPings;
   }

   public static enum Mode {
      INTERACT,
      STATISTICS;
   }
}
