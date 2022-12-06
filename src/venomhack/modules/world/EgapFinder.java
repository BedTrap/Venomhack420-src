package venomhack.modules.world;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint.Builder;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.TextUtils;

public class EgapFinder extends ModuleHelper {
   private final SettingGroup sgAllChunksMode = this.group("All Chunks Mode");
   private final Setting<EgapFinder.FinderMode> finderMode = this.setting(
      "finder-mode", "The egap finder mode. (AllChunks mode loads all chunks in the specified radius).", EgapFinder.FinderMode.MANUAL_MODE
   );
   private final Setting<Boolean> addWaypoint = this.setting("add-Waypoints", "Adds waypoints to chests that have egaps.", Boolean.valueOf(true));
   private final Setting<String> voxel = this.setting("voxelmap-waypoints", "Creates waypoints for voxelmap with this ip.", "phoenixanarchy.com");
   private final Setting<Boolean> chatNotif = this.setting("chat-info", "Send chat messages to update you on progress.", Boolean.valueOf(true));
   private final Setting<Boolean> playSound = this.setting("play-sound", "Plays a sound when you find an egap.", Boolean.valueOf(false));
   private final Setting<Boolean> pause = this.setting("pause", "Pauses all automated egap finder activity.", Boolean.valueOf(false));
   private final Setting<Integer> chunkCacheLimit = this.setting(
      "chunk-cache-limit", "The # of cached chunks at which the egap finder will pause until the count is below it.", Integer.valueOf(10000), 1.0, 50000.0
   );
   private final Setting<Integer> delayAfterFullCache = this.setting(
      "delay-after-full-cache", "How many ticks to wait after the cache limit is reached before resuming.", Integer.valueOf(100), 0.0, 200.0
   );
   private final Setting<Boolean> randomColor = this.setting("random-color", "Whether or not to randomise the waypoint's color.", Boolean.valueOf(true));
   private final Setting<Integer> radius = this.setting(
      "radius",
      "The radius of the square area to search for egaps.",
      Integer.valueOf(1600),
      this.sgAllChunksMode,
      () -> this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE,
      16.0,
      32000.0
   );
   private final Setting<Integer> chunkLoadDelay = this.setting(
      "chunk-load-delay",
      "How much time to wait before loading the next chunk, in ticks.",
      Integer.valueOf(30),
      this.sgAllChunksMode,
      () -> this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE,
      0.0,
      60.0
   );
   private final Setting<Integer> loadedChunksSize = this.setting(
      "loaded-chunks-size",
      "How many chunks to have loaded at any one time.",
      Integer.valueOf(40),
      this.sgAllChunksMode,
      () -> this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE,
      1.0,
      400.0
   );
   private final Setting<Integer> chunksPerTick = this.setting(
      "chunks-per-tick",
      "How many chunks to load each tick.",
      Integer.valueOf(5),
      this.sgAllChunksMode,
      () -> this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE,
      1.0,
      100.0
   );
   private final ArrayList<BlockPos> coordList = new ArrayList();
   private final ArrayList<BlockPos> chunkQueue = new ArrayList();
   private World world;
   private ServerWorld serverWorld;
   private PlayerEntity playerEntity;
   private int chunkDelay;
   private int currentChunkNum;
   private int waitTimer;
   private int egapsFoundCount;
   private boolean finishedQueue;
   private ExecutorService executor;

   public EgapFinder() {
      super(Venomhack420.CATEGORY, "egap-finder", "Logs coords (in chat) of chests that contain egaps, only works in singleplayer. ");
   }

   public void onActivate() {
      try {
         this.executor = Executors.newSingleThreadExecutor();
         this.coordList.clear();
         this.egapsFoundCount = 0;
         this.waitTimer = 0;
         if (this.mc.getServer() == null || this.mc.world == null) {
            return;
         }

         this.world = this.mc.getServer().getWorld(this.mc.world.getRegistryKey());
         this.playerEntity = this.mc
            .getServer()
            .getWorld(this.mc.world.getRegistryKey())
            .getPlayerByUuid(MinecraftClient.getInstance().player.getUuid());
         this.serverWorld = this.mc.getServer().getWorld(this.mc.world.getRegistryKey());
         if (this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE) {
            TextUtils.sendNewMessage("/gamerule sendCommandFeedback false");
            this.chunkDelay = this.chunkLoadDelay.get();
            this.currentChunkNum = 0;
            this.finishedQueue = false;
            this.chunkQueue.clear();

            for(int currentChunk = 0; (double)currentChunk < Math.floor((double)((Integer)this.radius.get()).intValue() / 16.0) + 1.0; ++currentChunk) {
               for(int l = -currentChunk; l < currentChunk + 1; ++l) {
                  for(int h = -currentChunk; h < currentChunk + 1; ++h) {
                     if (Math.abs(h) == Math.abs(currentChunk) || Math.abs(l) == Math.abs(currentChunk)) {
                        this.chunkQueue
                           .add(
                              new BlockPos(
                                 this.mc.player.getBlockPos().getX() + 16 * l, 50, this.mc.player.getBlockPos().getZ() + 16 * h
                              )
                           );
                     }
                  }
               }
            }

            this.finishedQueue = true;
            this.info(
               this.chunkQueue.size() + " chunks, area of side length " + 2.0 * Math.floor((double)((float)((Integer)this.radius.get()).intValue() / 16.0F)),
               new Object[0]
            );
         }
      } catch (Exception var4) {
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      try {
         --this.waitTimer;
         if (this.waitTimer <= 0) {
            this.waitTimer = 0;
            if (this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE && this.finishedQueue && !this.shouldPause()) {
               this.executor.submit(this::doChunkLoading);
            }

            if (this.finderMode.get() == EgapFinder.FinderMode.ALLCHUNKS_MODE && this.serverWorld.getChunkManager().getLoadedChunkCount() > this.chunkCacheLimit.get()) {
               TextUtils.sendNewMessage("/forceload remove all");
               this.waitTimer = this.delayAfterFullCache.get();
            }
         }
      } catch (Exception var3) {
      }
   }

   public void doChunkLoading() {
      --this.chunkDelay;
      if (this.chunkDelay <= 0) {
         for(int i = 0; i < this.chunksPerTick.get(); ++i) {
            if (this.currentChunkNum + this.chunksPerTick.get() > this.chunkQueue.size() - 1) {
               this.info("Done", new Object[0]);
               TextUtils.sendNewMessage("/forceload remove all");
               TextUtils.sendNewMessage("/gamerule sendCommandFeedback false");
               this.toggle();
               break;
            }

            BlockPos chunkPos = new BlockPos(
               ((BlockPos)this.chunkQueue.get(this.currentChunkNum + i)).getX(),
               50,
               ((BlockPos)this.chunkQueue.get(this.currentChunkNum + i)).getZ()
            );
            TextUtils.sendNewMessage("/forceload add " + chunkPos.getX() + " " + chunkPos.getZ());
            double percentage = (double)Math.round((double)(this.currentChunkNum + i) * 100.0 / (double)this.chunkQueue.size() * 100.0) / 100.0;
            double chunkspertick = (double)((Integer)this.chunksPerTick.get()).intValue() / (double)((Integer)this.chunkLoadDelay.get()).intValue();
            double estimatedTicksToCompletion = (double)(this.chunkQueue.size() - (this.currentChunkNum + i)) / chunkspertick;
            double estimatedSeconds = estimatedTicksToCompletion / 20.0;
            if (this.chatNotif.get()) {
               this.mc
                  .player
                  .sendMessage(
                     Text.of(
                        "§4§lchecked chunk number "
                           + (this.currentChunkNum + i)
                           + " [ "
                           + ((BlockPos)this.chunkQueue.get(this.currentChunkNum + i)).getX()
                           + " "
                           + ((BlockPos)this.chunkQueue.get(this.currentChunkNum + i)).getZ()
                           + " ] { "
                           + Math.floorDiv(chunkPos.getX(), 16)
                           + " "
                           + Math.floorDiv(chunkPos.getZ(), 16)
                           + " } "
                           + percentage
                           + " percent checked. Estimated time until completion: "
                           + Math.round(estimatedSeconds / 60.0 / 60.0)
                           + " H "
                           + Math.round(estimatedSeconds / 60.0 * 100.0) / 100L
                           + " min"
                     ),
                     true
                  );
            }

            for(BlockEntity blockEntity : this.world.getWorldChunk((BlockPos)this.chunkQueue.get(this.currentChunkNum + i)).getBlockEntities().values()) {
               if (blockEntity instanceof ChestBlockEntity) {
                  this.checkChestForEgaps(
                     blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()
                  );
               }
            }
         }

         if (this.serverWorld.getForcedChunks().size() >= this.loadedChunksSize.get()) {
            TextUtils.sendNewMessage("/forceload remove all");
         }

         this.currentChunkNum += this.chunksPerTick.get();
         this.chunkDelay = this.chunkLoadDelay.get();
      }
   }

   private boolean shouldPause() {
      return this.pause.get() || this.serverWorld.getChunkManager().getLoadedChunkCount() > this.chunkCacheLimit.get();
   }

   private void checkChestForEgaps(int x, int y, int z) {
      BlockPos chestBlockPos = new BlockPos(x, y, z);
      if (this.finderMode.get() != EgapFinder.FinderMode.ALLCHUNKS_MODE) {
         this.world.getBlockState(chestBlockPos);
      }

      ChestBlockEntity chest = (ChestBlockEntity)this.world.getWorldChunk(chestBlockPos).getBlockEntity(chestBlockPos);
      if (chest != null) {
         chest.checkLootInteraction(this.playerEntity);

         for(int i = 0; i < 27; ++i) {
            ItemStack stack = getInventory(this.world, chestBlockPos).getStack(i);
            if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE && !this.coordList.contains(chestBlockPos)) {
               this.coordList.add(chestBlockPos);
               ++this.egapsFoundCount;
               if (this.chatNotif.get()) {
                  this.warning(
                     "Egap "
                        + this.egapsFoundCount
                        + " at "
                        + chestBlockPos.getX()
                        + " "
                        + chestBlockPos.getY()
                        + " "
                        + chestBlockPos.getZ(),
                     new Object[0]
                  );
               }

               Color waypointColor = new Color(255, 215, 0);
               if (this.randomColor.get()) {
                  waypointColor = Color.fromHsv(ThreadLocalRandom.current().nextDouble() * 360.0, 1.0, 1.0);
               }

               Dimension dim = PlayerUtils.getDimension();
               if (this.addWaypoint.get()) {
                  Waypoint waypoint = new Builder().name("Egap " + this.egapsFoundCount).pos(chestBlockPos).dimension(dim).build();
                  Waypoints.get().add(waypoint);
               }

               int gapX = chestBlockPos.getX();
               int gapZ = chestBlockPos.getZ();
               if (dim == Dimension.Nether) {
                  gapX *= 8;
                  gapZ *= 8;
               }

               writeToFile(
                  "name:Egap "
                     + this.egapsFoundCount
                     + ",x:"
                     + gapX
                     + ",z:"
                     + gapZ
                     + ",y:"
                     + chestBlockPos.getY()
                     + ",enabled:true,red:"
                     + (float)waypointColor.a / 255.0F
                     + ",green:"
                     + (float)waypointColor.g / 255.0F
                     + ",blue:"
                     + (float)waypointColor.b / 255.0F
                     + ",suffix:,world:,dimensions:"
                     + (dim == Dimension.End ? "end#" : "overworld#the_nether#"),
                  (String)this.voxel.get()
               );
               if (this.playSound.get()) {
                  this.mc.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 2.0F, 1.0F);
               }
            }
         }
      }
   }

   @EventHandler
   private void onChunkData(ChunkDataEvent event) {
      try {
         if (this.finderMode.get() == EgapFinder.FinderMode.MANUAL_MODE) {
            for(BlockEntity blockEntity : event.chunk.getBlockEntities().values()) {
               BlockEntity tileEntity = this.mc.world.getWorldChunk(blockEntity.getPos()).getBlockEntity(blockEntity.getPos());
               if (tileEntity instanceof ChestBlockEntity) {
                  this.checkChestForEgaps(
                     tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ()
                  );
               }
            }
         }
      } catch (Exception var5) {
      }
   }

   @Nullable
   public static Inventory getInventory(World world, BlockPos pos) {
      ChunkPos cPos = world.getChunk(pos).getPos();
      if (!world.isChunkLoaded(cPos.x, cPos.z)) {
         return null;
      } else {
         BlockEntity var4 = world.getWorldChunk(pos).getBlockEntity(pos);
         return var4 instanceof Inventory ? (Inventory)var4 : null;
      }
   }

   protected static void writeToFile(String coords, String server) {
      try (
         FileWriter fw = new FileWriter("voxelmap\\" + server + ".points", true);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter out = new PrintWriter(bw);
      ) {
         out.println(coords);
      } catch (IOException var13) {
      }
   }

   public String getInfoString() {
      return this.egapsFoundCount + " Egaps";
   }

   public static enum FinderMode {
      MANUAL_MODE("Manual"),
      ALLCHUNKS_MODE("All Chunks");

      private final String title;

      private FinderMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
