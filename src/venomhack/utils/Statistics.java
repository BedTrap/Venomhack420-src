package venomhack.utils;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura;
import meteordevelopment.meteorclient.systems.modules.combat.AutoAnvil;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.client.network.OtherClientPlayerEntity;
import venomhack.events.PlayerDeathEvent;
import venomhack.events.PlayerListChangeEvent;
import venomhack.events.TotemPopEvent;
import venomhack.mixinInterface.IModule;
import venomhack.modules.combat.AutoAnchor;
import venomhack.modules.combat.AutoBed;
import venomhack.modules.combat.AutoCrystal;
import venomhack.modules.combat.AutoFunnyCrystal;
import venomhack.modules.combat.Surround;

public class Statistics {
   private static Statistics INSTANCE = null;
   private static final Class[] COMBAT_MODULES = new Class[]{
      AutoAnchor.class, AutoBed.class, AutoCrystal.class, AutoFunnyCrystal.class, AnchorAura.class, AutoAnvil.class, BedAura.class, CrystalAura.class
   };
   private final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap();
   private short kills;
   private short killStreak;
   private int deaths;
   private final Collection<PlayerListEntry> lastList = new ArrayList();
   private String lastMessage;
   private final List<PlayerEntity> targets = new ArrayList();
   private final List<Long> targetTimes = new ArrayList<>();
   public final Map<BlockPos, Long> pendingObsidian = new ConcurrentHashMap<>();
   public final Int2ObjectMap<Vec3d> accuratePos = new Int2ObjectOpenHashMap();

   private Statistics() {
   }

   public static Statistics get() {
      if (INSTANCE == null) {
         INSTANCE = new Statistics();
      }

      return INSTANCE;
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.totemPops.clear();
      this.targets.clear();
      this.targetTimes.clear();
      this.pendingObsidian.clear();
      this.kills = 0;
      this.killStreak = 0;
      this.deaths = 0;
      this.lastMessage = "";
      if (!MeteorClient.mc.isInSingleplayer()) {
         this.lastList.clear();
         this.lastList.addAll(MeteorClient.mc.player.networkHandler.getPlayerList());
      }
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      Packet entity = event.packet;
      if (entity instanceof EntityStatusS2CPacket packet) {
         if (packet.getStatus() != 35) {
            return;
         }

         Entity entityx = packet.getEntity(MeteorClient.mc.world);
         if (entityx == null) {
            return;
         }

         int pops = this.totemPops.getOrDefault(entityx.getUuid(), 0) + 1;
         this.totemPops.put(entityx.getUuid(), pops);
         MeteorClient.EVENT_BUS.post(TotemPopEvent.get(entityx, (short)pops, entityx instanceof PlayerEntity && this.targets.contains(entityx)));
      } else {
         entity = event.packet;
         if (entity instanceof BlockUpdateS2CPacket packet) {
            this.pendingObsidian.remove(packet.getPos());
         } else {
            entity = event.packet;
            if (entity instanceof EntityPositionS2CPacket packet) {
               this.accuratePos.put(packet.getId(), new Vec3d(packet.getX(), packet.getY(), packet.getZ()));
            }
         }
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.world != null) {
         int latency = 100;
         PlayerListEntry networkEntry = MeteorClient.mc.player.networkHandler.getPlayerListEntry(MeteorClient.mc.getSession().getUuid());
         if (networkEntry != null && networkEntry.getLatency() != 0) {
            latency = networkEntry.getLatency();
         }

         for(Entry<BlockPos, Long> entry : this.pendingObsidian.entrySet()) {
            if ((double)(System.currentTimeMillis() - entry.getValue()) > (double)latency * 1.2) {
               this.pendingObsidian.remove(entry.getKey());
            }
         }

         Surround surround = (Surround)Modules.get().get(Surround.class);
         if (surround.auto.get()) {
            surround.enableSurroundListener();
         }

         int max = this.targets.size();

         for(int i = 0; i < max; ++i) {
            int lookBackAmount = 5000;
            if (this.targetTimes.get(i) + (long)lookBackAmount < System.currentTimeMillis()) {
               this.remove(i);
               --max;
               --i;
            }
         }

         Modules mods = Modules.get();

         for(Class clas : COMBAT_MODULES) {
            if (mods.isActive(clas)) {
               PlayerEntity target = ((IModule)mods.get(clas)).getTarget();
               if (target != null) {
                  this.add(target);
               }
            }
         }

         for(AbstractClientPlayerEntity player : MeteorClient.mc.world.getPlayers()) {
            if (player.deathTime > 0 || player.getHealth() <= 0.0F) {
               boolean isTarget = this.targets.contains(player);
               if (player.equals(MeteorClient.mc.player)) {
                  this.setDead();
               } else if (isTarget) {
                  ++this.kills;
                  ++this.killStreak;
                  this.remove(player);
               }

               MeteorClient.EVENT_BUS.post(PlayerDeathEvent.get(player, (short)this.totemPops.removeInt(player.getUuid()), isTarget, false));
            }
         }

         if (!MeteorClient.mc.isInSingleplayer() && MeteorClient.mc.getNetworkHandler() != null) {
            Collection<PlayerListEntry> nowList = MeteorClient.mc.getNetworkHandler().getPlayerList();
            ArrayList<PlayerListEntry> tempList = new ArrayList(nowList);
            tempList.removeAll(this.lastList);
            tempList.forEach(player -> MeteorClient.EVENT_BUS.post(PlayerListChangeEvent.Join.get(player)));
            this.lastList.removeAll(nowList);
            this.lastList.forEach(player -> MeteorClient.EVENT_BUS.post(PlayerListChangeEvent.Leave.get(player)));
            this.lastList.clear();
            this.lastList.addAll(nowList);
         }
      }
   }

   @EventHandler
   private void onMessageRecieve(Receive event) {
      Packet msg = event.packet;
      if (msg instanceof GameMessageS2CPacket) {
         GameMessageS2CPacket packet = (GameMessageS2CPacket)msg;
         if (packet.comp_763() != null) {
            String msgx = packet.comp_763().getString();
            int byIndex = msgx.indexOf("by");
            if (byIndex != -1) {
               int nameIndex = msgx.indexOf(MeteorClient.mc.player.getEntityName());
               if (nameIndex >= byIndex) {
                  String victimsName = msgx.substring(0, msgx.indexOf(" "));

                  for(AbstractClientPlayerEntity player : MeteorClient.mc.world.getPlayers()) {
                     if (player.getEntityName().equals(victimsName) && this.targets.contains(player)) {
                        return;
                     }
                  }

                  for(Friend friend : Friends.get()) {
                     if (victimsName.equals(friend.name)) {
                        return;
                     }
                  }

                  PlayerEntity victim = null;

                  for(PlayerListEntry target : MeteorClient.mc.getNetworkHandler().getPlayerList()) {
                     if (target.getProfile().getName().equals(victimsName)) {
                        victim = new OtherClientPlayerEntity(MeteorClient.mc.world, new GameProfile(target.getProfile().getId(), victimsName), target.getPublicKeyData());
                        break;
                     }
                  }

                  if (victim != null) {
                     ++this.kills;
                     ++this.killStreak;
                     MeteorClient.EVENT_BUS.post(PlayerDeathEvent.get(victim, (short)this.totemPops.removeInt(victim.getUuid()), true, true));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (MeteorClient.mc.world != null) {
         if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            IPlayerInteractEntityC2SPacket packet = (IPlayerInteractEntityC2SPacket)event.packet;
            Entity var5 = packet.getEntity();
            if (var5 instanceof PlayerEntity target) {
               if (MeteorClient.mc.player == target) {
                  return;
               }

               if (!Friends.get().shouldAttack(target)) {
                  return;
               }

               this.add(target);
            }
         } else {
            Packet var6 = event.packet;
            if (var6 instanceof ChatMessageC2SPacket packet) {
               packet.comp_945 = packet.comp_945
                  .replace("{player}", this.getLastTarget() == null ? "Nigger" : this.getLastTarget().getEntityName())
                  .replace("{kills}", this.getKills() + "")
                  .replace("{ks}", this.getKillStreak() + "")
                  .replace("{ksSuffix}", TextUtils.getGrammar(this.getKillStreak()))
                  .replace("{deaths}", this.getDeaths() + "")
                  .replace("{server.ip}", MeteorClient.mc.getCurrentServerEntry() == null ? "Server" : MeteorClient.mc.getCurrentServerEntry().address)
                  .replace("{server.name}", MeteorClient.mc.getCurrentServerEntry() == null ? "Server" : MeteorClient.mc.getCurrentServerEntry().name)
                  .replace("{server.online}", MeteorClient.mc.getNetworkHandler() == null ? "420" : MeteorClient.mc.getNetworkHandler().getPlayerList().size() + "")
                  .replace(
                     "{ping}",
                     MeteorClient.mc.getNetworkHandler() != null && MeteorClient.mc.getNetworkHandler().getPlayerListEntry(MeteorClient.mc.player.getUuid()) != null
                        ? MeteorClient.mc.getNetworkHandler().getPlayerListEntry(MeteorClient.mc.player.getUuid()).getLatency() + ""
                        : "69"
                  )
                  .replace("{kd}", this.getKayDee() + "");
               this.lastMessage = packet.comp_945;
            }
         }
      }
   }

   public void remove(PlayerEntity target) {
      int index = this.targets.indexOf(target);
      if (index != -1) {
         this.remove(index);
      }
   }

   private void remove(int index) {
      this.targets.remove(index);
      this.targetTimes.remove(index);
   }

   public void add(PlayerEntity target) {
      if (this.targets.contains(target)) {
         this.targetTimes.set(this.targets.indexOf(target), System.currentTimeMillis());
      } else {
         this.targets.add(target);
         this.targetTimes.add(System.currentTimeMillis());
      }
   }

   public boolean isTarget(PlayerListEntry playerListEntry) {
      for(PlayerEntity player : this.targets) {
         if (player.getEntityName().equals(playerListEntry.getProfile().getName())) {
            return true;
         }
      }

      return false;
   }

   public String getLastMessage() {
      return this.lastMessage;
   }

   public void setDead() {
      ++this.deaths;
      this.killStreak = 0;
   }

   public int getTotemPops(PlayerEntity player) {
      return this.totemPops.getInt(player.getUuid());
   }

   public short getKills() {
      return this.kills;
   }

   public short getKillStreak() {
      return this.killStreak;
   }

   public float getKayDee() {
      return this.deaths == 0 ? (float)this.kills : (float)TextUtils.round((float)this.kills / (float)this.deaths, 1);
   }

   public PlayerEntity getLastTarget() {
      return this.targets.isEmpty() ? null : (PlayerEntity)this.targets.get(this.targets.size() - 1);
   }

   public int getDeaths() {
      return this.deaths;
   }

   public List<PlayerEntity> getTargets() {
      return this.targets;
   }
}
