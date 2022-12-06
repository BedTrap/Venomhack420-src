package venomhack.modules.combat;

import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import venomhack.Venomhack420;
import venomhack.enums.Origin;
import venomhack.modules.ModuleHelper;
import venomhack.modules.movement.speed.Speed;
import venomhack.utils.AntiCheatHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.PingUtils;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RandUtils;
import venomhack.utils.Statistics;
import venomhack.utils.ThreadedUtils;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.RenderBlock;
import venomhack.utils.customObjects.Timer;

public class Surround extends ModuleHelper {
   private final SettingGroup sgAntiCheat = this.group("Anti Cheat");
   private final SettingGroup sgAttack = this.group("Attack Crystals");
   private final SettingGroup sgModules = this.group("Automation");
   private final SettingGroup sgRender = this.group("Rendering");
   private final Setting<Boolean> onlyGround = this.setting(
      "only-on-ground", "Won't attempt to place while you're not standing on ground.", Boolean.valueOf(false)
   );
   private final Setting<Boolean> center = this.setting("center", "Will move you to inside the hole so you can surround.", Boolean.valueOf(true));
   private final Setting<Boolean> hardSnap = this.setting(
      "hard-center", "Will align you at the exact center of the hole.", Boolean.valueOf(false), this.center::get
   );
   private final Setting<Boolean> antiSurroundBreak = this.setting(
      "anti-surround-break", "Places blocks around the city block that is being mined.", Boolean.valueOf(false)
   );
   private final Setting<Boolean> antiPhase = this.setting(
      "anti-phase", "Places blocks around players that are standing in your surround", Boolean.valueOf(true)
   );
   private final Setting<Double> placeRange = this.setting(
      "place-range", "How far you are able to place at max.", Double.valueOf(5.0), this.sgAntiCheat, 0.0, 6.0
   );
   private final Setting<Boolean> strictDirections = this.setting(
      "strict-directions", "Places only on visible sides.", Boolean.valueOf(false), this.sgAntiCheat
   );
   private final Setting<Boolean> rotate = this.setting("rotate", "Rotate to where you are placing.", Boolean.valueOf(false), this.sgAntiCheat);
   private final Setting<Boolean> airPlace = this.setting(
      "air-place", "Places blocks midair, will try to find support blocks when off.", Boolean.valueOf(true), this.sgAntiCheat
   );
   private final Setting<Integer> bpt = this.setting(
      "blocks-per-tick", "How many blocks to place per tick max.", Integer.valueOf(5), this.sgAntiCheat, 1.0, 5.0
   );
   private final Setting<Integer> delay = this.setting(
      "place-delay", "Delay between placing in ms", Integer.valueOf(25), this.sgAntiCheat, 25.0, 250.0, 25, Integer.MAX_VALUE
   );
   private final Setting<Boolean> attackCrystals = this.setting(
      "attack-crystals", "Whether to attack crystals that are in the way.", Boolean.valueOf(true), this.sgAttack
   );
   private final Setting<Integer> attackSwapPenalty = this.setting(
      "swap-penalty",
      "For how long to wait in ms after switching to obsidian until attacking.",
      Integer.valueOf(0),
      this.sgAttack,
      this.attackCrystals::get,
      0.0,
      500.0
   );
   private final Setting<Integer> attackMinAge = this.setting(
      "min-age", "How many ticks the cystal has to be alive for until u can attack it.", Integer.valueOf(0), this.sgAttack, this.attackCrystals::get, 0.0, 5.0
   );
   private final Setting<Double> attackRange = this.setting(
      "attack-range", "Maximum attack range.", Double.valueOf(4.0), this.sgAttack, this.attackCrystals::get, 0.0, 6.0
   );
   private final Setting<Integer> attackDelay = this.setting(
      "attack-delay", "How many ticks to wait between attacks.", Integer.valueOf(1), this.sgAttack, this.attackCrystals::get, 1.0, 5.0, 1, Integer.MAX_VALUE
   );
   private final Setting<Boolean> attackRotate = this.setting(
      "attack-rotate", "Whether to face the crystal you are attacking.", Boolean.valueOf(false), this.sgAttack, this.attackCrystals::get
   );
   private final Setting<Boolean> toggle = this.setting("toggle-on-y-change", "Will toggle off when you move upwards.", Boolean.valueOf(true), this.sgModules);
   public final Setting<Boolean> auto = this.setting(
      "auto-surround", "Automatically turns on surround when in an obsidian hole.", Boolean.valueOf(false), this.sgModules
   );
   private final Setting<Boolean> toggleStep = this.setting(
      "toggle-step", "Toggles off step when activating surround.", Boolean.valueOf(false), this.sgModules
   );
   private final Setting<Boolean> toggleSpeed = this.setting(
      "toggle-speed", "Toggles off vh speed when activating surround.", Boolean.valueOf(false), this.sgModules
   );
   private final Setting<Boolean> toggleBack = this.setting(
      "toggle-back", "Toggles on speed and/or step when turning off surround.", Boolean.valueOf(false), this.sgModules
   );
   private final Setting<Boolean> swing = this.setting("swing", "Renders your swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Renders the block where it is placing a crystal.", Boolean.valueOf(false), this.sgRender);
   private final Setting<Integer> renderTime = this.setting(
      "render-time", "Ticks to render the block for.", Integer.valueOf(8), this.sgRender, this.render::get
   );
   private final Setting<Boolean> fade = this.setting(
      "fade", "Will reduce the opacity of the rendered block over time.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the shapes are rendered.", ShapeMode.Both, this.sgRender, this.render::get);
   private final Setting<SettingColor> sideColor = this.setting("side-color", "The side color.", 0, 0, 255, 10, this.sgRender, this.render::get);
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color.", 0, 0, 255, 200, this.sgRender, this.render::get);
   private BlockPos playerPos;
   private boolean hasCentered;
   private boolean isOpen;
   private final List<BlockPos> extras = new CopyOnWriteArrayList();
   private final List<RenderBlock> renderBlocks = Collections.synchronizedList(new ArrayList<>());
   private int attackDelayLeft;
   private int blocksPlaced;
   private final Timer swapPenaltyTimer = new Timer();
   private final Timer placeTimer = new Timer();
   private final ConcurrentHashMap<Integer, Long> toRemoveWithTime = new ConcurrentHashMap<>();
   private static final Direction[] ORDERED_DIRECTIONS_ARRAY = new Direction[]{
      Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP
   };
   private final Surround.StaticListener staticListener = new Surround.StaticListener();

   public Surround() {
      super(Venomhack420.CATEGORY, "surround-vh", "Surrounds you in blocks to prevent you from taking lots of damage.");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.toggle.get()
         && this.playerPos != null
         && (
               this.mc.world.getBlockState(this.mc.player.getBlockPos()).isOf(Blocks.ENDER_CHEST)
                  ? Math.ceil(this.mc.player.getY())
                  : (double)this.mc.player.getBlockY()
            )
            > (double)this.playerPos.getY()
         && !((Blink)Modules.get().get(Blink.class)).isActive()) {
         this.toggle();
      } else {
         this.blocksPlaced = 0;
         if (!this.onlyGround.get() || this.mc.player.isOnGround()) {
            if (this.center.get() && !this.hasCentered) {
               BlockUtils2.centerPlayer(this.hardSnap.get());
               this.hasCentered = true;
            }

            this.playerPos = new BlockPos(
               (double)this.mc.player.getBlockX(),
               this.mc.world.getBlockState(this.mc.player.getBlockPos()).isOf(Blocks.ENDER_CHEST)
                  ? Math.ceil(this.mc.player.getY())
                  : (double)this.mc.player.getBlockY(),
               (double)this.mc.player.getBlockZ()
            );
            if (!this.toggle.get()
               || this.playerPos == null
               || (
                     this.mc.world.getBlockState(this.mc.player.getBlockPos()).isOf(Blocks.ENDER_CHEST)
                        ? Math.ceil(this.mc.player.getY())
                        : (double)this.mc.player.getBlockY()
                  )
                  <= (double)this.playerPos.getY()
               || ((Blink)Modules.get().get(Blink.class)).isActive()) {
               this.place();
            }

            this.isOpen = !UtilsPlus.isSurrounded(this.mc.player, true, false);
         }
      }
   }

   @EventHandler
   public void onPacketReceive(Receive event) {
      if (this.playerPos != null && this.mc.player != null && this.mc.world != null) {
         synchronized(this.toRemoveWithTime) {
            for(Integer key : this.toRemoveWithTime.keySet()) {
               if (System.currentTimeMillis() - 100L > this.toRemoveWithTime.getOrDefault(key, Long.MAX_VALUE)) {
                  this.toRemoveWithTime.remove(key);
               }
            }
         }

         if (!this.toggle.get()
            || this.playerPos == null
            || (
                  this.mc.world.getBlockState(this.mc.player.getBlockPos()).isOf(Blocks.ENDER_CHEST)
                     ? Math.ceil(this.mc.player.getY())
                     : (double)this.mc.player.getBlockY()
               )
               <= (double)this.playerPos.getY()
            || ((Blink)Modules.get().get(Blink.class)).isActive()) {
            this.place();
         }

         Packet noCrystal = event.packet;
         if (noCrystal instanceof BlockBreakingProgressS2CPacket packet) {
            if (this.mc.world.getEntityById(packet.getEntityId()) == this.mc.player) {
               return;
            }

            if (this.mc.world.getBlockState(packet.getPos()).getBlock().getHardness() < 0.0F) {
               return;
            }

            if (!this.getPlacePositions(false).contains(packet.getPos())) {
               return;
            }

            ObjectIterator var22 = ((WorldRendererAccessor)this.mc.worldRenderer).getBlockBreakingInfos().values().iterator();

            while(var22.hasNext()) {
               BlockBreakingInfo value = (BlockBreakingInfo)var22.next();
               BlockPos valuePos = value.getPos();
               BlockPos packetPos = packet.getPos();
               if (valuePos.getX() == packetPos.getX()
                  || valuePos.getY() == packetPos.getY()
                  || valuePos.getZ() == packetPos.getZ()) {
                  int msPassed = (this.mc.worldRenderer.ticks - value.getLastUpdateTick()) * 50;
                  if (msPassed != 0) {
                     int ping = PingUtils.enabled.get() ? PingUtils.getPing() : PlayerUtils.getPing();
                     int pingOffset = Math.floorDiv(ping, msPassed);
                     if (value.getStage() == 8 - pingOffset) {
                        FindItemResult result = this.findBlock();

                        for(EndCrystalEntity crystal : this.mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(valuePos), Entity::isAlive)) {
                           if (crystal.isAlive()) {
                              this.attackCrystal(crystal, Hand.MAIN_HAND);
                           }
                        }

                        int delay = pingOffset > 0 ? ping + msPassed : msPassed - ping;
                        ThreadedUtils.antiCityExecutor.schedule(() -> {
                           if (result.found()) {
                              BlockUtils2.justPlace(result, BlockUtils2.getPlaceResult(valuePos, true, false), this.swing.get(), false, 0);
                           }
                        }, (long)delay, TimeUnit.MILLISECONDS);
                     }
                  }
               }
            }

            if (!this.antiSurroundBreak.get()) {
               return;
            }

            synchronized(this.extras) {
               this.extras.add(packet.getPos().north());
               this.extras.add(packet.getPos().west());
               this.extras.add(packet.getPos().south());
               this.extras.add(packet.getPos().east());
            }
         } else {
            noCrystal = event.packet;
            if (noCrystal instanceof BlockUpdateS2CPacket packet) {
               if (!this.placeTimer.passedMillis((long)((Integer)this.delay.get()).intValue())) {
                  return;
               }

               if (!packet.getState().getMaterial().isReplaceable() && packet.getState().getBlock().getHardness() != 0.0F) {
                  return;
               }

               FindItemResult result = this.findBlock();
               if (!result.found()) {
                  return;
               }

               if (!this.getPlacePositions(false).contains(packet.getPos())) {
                  return;
               }

               this.isOpen = true;
               this.doPlace(packet.getState(), result, packet.getPos(), this.attackRotate.get());
            } else {
               noCrystal = event.packet;
               if (noCrystal instanceof EntitiesDestroyS2CPacket packet) {
                  if (!this.isOpen) {
                     return;
                  }

                  if (this.onlyGround.get() && !this.mc.player.isOnGround()) {
                     return;
                  }

                  boolean noCrystal = true;

                  try {
                     IntListIterator var28 = packet.getEntityIds().iterator();

                     while(var28.hasNext()) {
                        Integer id = (Integer)var28.next();
                        Entity var34 = this.mc.world.getEntityById(id);
                        if (var34 instanceof EndCrystalEntity entity && !(entity.distanceTo(this.mc.player) > 9.0F)) {
                           noCrystal = false;
                           break;
                        }
                     }
                  } catch (Exception var17) {
                  }

                  if (noCrystal) {
                     return;
                  }

                  FindItemResult result = this.findBlock();
                  if (!result.found()) {
                     return;
                  }

                  for(BlockPos pos : new ArrayList(this.getPlacePositions(true))) {
                     if (this.doPlace(this.mc.world.getBlockState(pos), result, pos, this.attackRotate.get()) && this.blocksPlaced >= this.bpt.get()) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketSend(Sent event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
         this.swapPenaltyTimer.reset();
      }
   }

   @EventHandler
   private void onPostTick(Post event) {
      synchronized(this.extras) {
         this.extras.clear();
      }

      synchronized(this.renderBlocks) {
         RenderBlock.tick(this.renderBlocks);
      }

      --this.attackDelayLeft;
   }

   private boolean doPlace(BlockState state, FindItemResult result, BlockPos pos, boolean attackRotate) {
      if (BlockUtils2.invalidPos(pos)) {
         return false;
      } else {
         boolean isBed = state.getBlock() instanceof BedBlock && this.mc.world.getRegistryKey() != World.OVERWORLD;
         if (!isBed && !state.getMaterial().isReplaceable() && state.getBlock().getHardness() != 0.0F) {
            return false;
         } else {
            BlockHitResult hitResult = BlockUtils2.getPlaceResult(pos, this.airPlace.get(), this.strictDirections.get());
            if (hitResult == null) {
               return false;
            } else {
               VoxelShape placeShape = this.getPlaceState(result).getCollisionShape(this.mc.world, pos, ShapeContext.absent());
               Entity crystal = null;

               try {
                  label135: {
                     Iterator var9 = this.mc
                        .world
                        .getOtherEntities(null, placeShape.isEmpty() ? new Box(pos) : placeShape.getBoundingBox().offset(pos), Entity::canHit)
                        .iterator();

                     while(true) {
                        if (!var9.hasNext()) {
                           break label135;
                        }

                        Entity entity = (Entity)var9.next();
                        if (!(entity instanceof EndCrystalEntity)) {
                           break;
                        }

                        if (crystal == null) {
                           synchronized(this.toRemoveWithTime) {
                              if (this.toRemoveWithTime.containsKey(entity.getId())) {
                                 continue;
                              }
                           }

                           if (!this.attackCrystals.get()
                              || this.attackDelayLeft > 0
                              || !this.swapPenaltyTimer.passedMillis((long)((Integer)this.attackSwapPenalty.get()).intValue())
                              || !((double)this.mc.player.distanceTo(entity) <= this.attackRange.get())
                              || entity.age < this.attackMinAge.get()) {
                              break;
                           }

                           crystal = entity;
                        }
                     }

                     return false;
                  }
               } catch (ConcurrentModificationException var18) {
               }

               if (crystal != null) {
                  synchronized(this.toRemoveWithTime) {
                     this.toRemoveWithTime.put(crystal.getId(), System.currentTimeMillis());
                  }

                  this.attackDelayLeft = this.attackDelay.get();
                  if (attackRotate) {
                     Rotations.rotate(Rotations.getYaw(crystal), Rotations.getPitch(crystal), 101, () -> this.doPlace(state, result, pos, false));
                     return true;
                  }

                  this.attackCrystal(crystal, RandUtils.hand(result));
               }

               if (isBed) {
                  this.mc
                     .player
                     .networkHandler
                     .sendPacket(
                        new PlayerInteractBlockC2SPacket(
                           Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), BlockUtils2.getClosestDirection(pos, false), pos, false), 0
                        )
                     );
               } else if (state.getBlock().getHardness() == 0.0F && !state.isAir()) {
                  UtilsPlus.mine(pos, false, false);
               }

               BlockUtils2.justPlace(result, hitResult, this.swing.get(), this.rotate.get(), 100);
               if (this.getPlaceState(result).getBlock() == Blocks.OBSIDIAN) {
                  Statistics.get().pendingObsidian.putIfAbsent(pos, System.currentTimeMillis());
               }

               this.placeTimer.reset();
               ++this.blocksPlaced;
               synchronized(this.renderBlocks) {
                  RenderBlock.addRenderBlock(this.renderBlocks, pos, this.renderTime.get());
                  return true;
               }
            }
         }
      }
   }

   private void add(Set<BlockPos> set, BlockPos pos) {
      if (this.airPlace.get()) {
         set.add(pos);
      } else if (!BlockUtils2.invalidPos(pos)) {
         if (!BlockUtils2.outOfPlaceRange(pos, Origin.NCP, this.placeRange.get())) {
            BlockState state = this.mc.world.getBlockState(pos);
            if (state.getMaterial().isReplaceable() || state.getBlock().getHardness() == 0.0F) {
               BlockState placeState = this.getPlaceState(this.findBlock());
               if (this.findNeighbour(set, pos, 0, placeState, (int)Math.floor(PlayerUtils2.eyePos(this.mc.player).y))) {
                  set.add(pos);
               }
            }
         }
      }
   }

   private boolean findNeighbour(Set<BlockPos> set, BlockPos pos, int iteration, BlockState placeState, int playerEyeY) {
      for(Direction direction : Direction.values()) {
         BlockPos neighbour = pos.offset(direction);
         if ((
               !this.strictDirections.get()
                  || AntiCheatHelper.isInteractableStrict(
                     this.mc.player.getBlockX(), playerEyeY, this.mc.player.getBlockZ(), neighbour, direction.getOpposite()
                  )
            )
            && (
               set.contains(neighbour)
                  || Statistics.get().pendingObsidian.containsKey(neighbour)
                  || !this.mc.world.getBlockState(neighbour).getMaterial().isReplaceable()
            )) {
            set.add(pos);
            return true;
         }
      }

      if ((double)(iteration + 1) > Math.ceil(this.placeRange.get()) * 2.0) {
         return false;
      } else {
         for(Direction direction : ORDERED_DIRECTIONS_ARRAY) {
            BlockPos neighbour = pos.offset(direction);
            if (BlockUtils2.invalidPos(neighbour)) {
               return false;
            }

            if (BlockUtils2.outOfPlaceRange(neighbour, Origin.NCP, this.placeRange.get())) {
               return false;
            }

            if (this.strictDirections.get() && placeState.isFullCube(this.mc.world, neighbour)) {
               Set<Direction> strictDirs = AntiCheatHelper.getInteractableDirections(
                  this.mc.player.getBlockX() - neighbour.getX(),
                  playerEyeY - neighbour.getY(),
                  this.mc.player.getBlockZ() - neighbour.getZ(),
                  true
               );
               Direction oppositeDirection = direction.getOpposite();
               if (!strictDirs.contains(oppositeDirection)
                  || AntiCheatHelper.isDirectionBlocked(neighbour, strictDirs, oppositeDirection, true)
                  || set.contains(pos)) {
                  continue;
               }
            }

            if (this.attackCrystals.get()) {
               VoxelShape placeShape = placeState.getCollisionShape(this.mc.world, neighbour);
               if (!this.mc
                  .world
                  .getOtherEntities(
                     null,
                     placeShape.isEmpty() ? new Box(neighbour) : placeShape.getBoundingBox().offset(neighbour),
                     entity -> entity.canHit() && !(entity instanceof EndCrystalEntity)
                  )
                  .isEmpty()) {
                  continue;
               }
            } else if (!this.mc.world.canPlace(placeState, neighbour, ShapeContext.absent())) {
               continue;
            }

            if (this.findNeighbour(set, neighbour, ++iteration, placeState, playerEyeY)) {
               return false;
            }
         }

         return false;
      }
   }

   public Set<BlockPos> getPlacePositions(boolean withExtra) {
      HashSet<BlockPos> positions = new HashSet(12);
      if (this.playerPos == null) {
         this.playerPos = this.mc.player.getBlockPos();
      }

      try {
         for(BlockPos city : BlockUtils2.getCity(this.mc.player, true, true)) {
            if (!this.mc.world.getBlockState(city).isOf(Blocks.BEDROCK)) {
               if (city.getY() == this.playerPos.getY()) {
                  VoxelShape shape = this.mc.world.getBlockState(city).getCollisionShape(this.mc.world, city);
                  boolean shouldSkipThisPos = false;

                  for(Entity entity : this.mc
                     .world
                     .getOtherEntities(null, shape.isEmpty() ? new Box(city) : shape.getBoundingBox().offset(city), Entity::canHit)) {
                     if ((!this.attackCrystals.get() || !(entity instanceof EndCrystalEntity))
                        && entity instanceof PlayerEntity player
                        && UtilsPlus.smartVelocity(player).length() == 0.0) {
                        if (this.antiPhase.get()) {
                           for(BlockPos blockPos : BlockUtils2.getCity(player, false, true)) {
                              this.add(positions, new BlockPos(blockPos.getX(), city.getY(), blockPos.getZ()));
                           }
                        }

                        shouldSkipThisPos = true;
                     }
                  }

                  if (shouldSkipThisPos) {
                     continue;
                  }
               }

               this.add(positions, city);
            }
         }
      } catch (ConcurrentModificationException var14) {
      }

      if (withExtra) {
         synchronized(this.extras) {
            for(BlockPos block : this.extras) {
               this.add(positions, block);
            }
         }
      }

      return positions;
   }

   private void place() {
      if (this.placeTimer.passedMillis((long)((Integer)this.delay.get()).intValue())) {
         if (!this.onlyGround.get() || this.mc.player.isOnGround()) {
            FindItemResult result = this.findBlock();
            if (result.found()) {
               ArrayList<BlockPos> blockList = new ArrayList(this.getPlacePositions(true));
               blockList.sort(Comparator.comparingDouble(o -> o.getSquaredDistance(this.mc.player.getPos())));

               for(BlockPos pos : blockList) {
                  if (this.doPlace(this.mc.world.getBlockState(pos), result, pos, this.attackRotate.get()) && this.blocksPlaced >= this.bpt.get()) {
                     break;
                  }
               }
            }
         }
      }
   }

   private BlockState getPlaceState(FindItemResult result) {
      Item item = PlayerUtils2.getItemFromResult(result);
      return item instanceof BlockItem blockItem ? blockItem.getBlock().getDefaultState() : Blocks.AIR.getDefaultState();
   }

   private void attackCrystal(Entity entity, Hand hand) {
      this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, this.mc.player.isSneaking()));
      RandUtils.swing(this.swing.get(), hand);
   }

   private FindItemResult findBlock() {
      FindItemResult result = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
      if (!result.found()) {
         result = InvUtils.findInHotbar(UtilsPlus::isGoodForSurround);
      }

      return result;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get() && !this.renderBlocks.isEmpty()) {
         synchronized(this.renderBlocks) {
            for(RenderBlock block : this.renderBlocks) {
               block.render(event, (SettingColor)this.sideColor.get(), (SettingColor)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), this.fade.get());
            }
         }
      }
   }

   public void onActivate() {
      this.hasCentered = false;
      this.playerPos = null;
      this.attackDelayLeft = 0;
      this.renderBlocks.clear();
      this.swapPenaltyTimer.setMs((long)((Integer)this.attackSwapPenalty.get()).intValue());
      this.placeTimer.setMs((long)((Integer)this.delay.get()).intValue());
      synchronized(this.toRemoveWithTime) {
         this.toRemoveWithTime.clear();
      }

      synchronized(this.renderBlocks) {
         this.renderBlocks.clear();
      }

      Module step = Modules.get().get(Step.class);
      Module speed = Modules.get().get(Speed.class);
      if (this.toggleStep.get() && step.isActive()) {
         step.toggle();
      }

      if (this.toggleSpeed.get() && speed.isActive()) {
         speed.toggle();
      }
   }

   public void onDeactivate() {
      if (this.toggleBack.get()) {
         Module step = Modules.get().get(Step.class);
         Module speed = Modules.get().get(Speed.class);
         if (this.toggleStep.get() && !step.isActive()) {
            step.toggle();
         }

         if (this.toggleSpeed.get() && !speed.isActive()) {
            speed.toggle();
         }
      }
   }

   public void enableSurroundListener() {
      MeteorClient.EVENT_BUS.subscribe(this.staticListener);
   }

   private class StaticListener {
      @EventHandler
      private void surroundListener(meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Post event) {
         if (Surround.this.auto.get()
            && !Modules.get().isActive(Surround.class)
            && (!Surround.this.onlyGround.get() || Surround.this.mc.player.isOnGround())
            && UtilsPlus.obbySurrounded(Surround.this.mc.player)) {
            ((Surround)Modules.get().get(Surround.class)).toggle();
         }
      }
   }

   public static enum SurroundMode {
      NORMAL("Normal"),
      CANCER("Cancer");

      private final String title;

      private SurroundMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
