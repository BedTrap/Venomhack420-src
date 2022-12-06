package venomhack.modules.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import venomhack.Venomhack420;
import venomhack.events.PlayerListChangeEvent;
import venomhack.modules.ModuleHelper;
import venomhack.utils.RandUtils;

public class LogoutSpotsRewrite extends ModuleHelper {
   private final SettingGroup sgRender = this.group("Render");
   private final Setting<Boolean> msg = this.setting("message", "Sends a chat message when a logged person logs back in.", Boolean.valueOf(true));
   private final Setting<Boolean> coords = this.setting(
      "coords-in-message",
      "Defines whether the log back in message should contain the coordinates of the logout spot.",
      Boolean.valueOf(true),
      this.sgGeneral,
      this.msg::get
   );
   private final Setting<SettingColor> nameColor = this.setting("name-color", "The color for the name of the nametag.", 255, 255, 255, this.sgRender);
   private final Setting<SettingColor> sideColor = this.setting("side-color", "The side color.", 255, 215, 0, 70, this.sgRender);
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color.", 255, 215, 0, this.sgRender);
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "The shape mode", ShapeMode.Both, this.sgRender);
   private final Setting<Double> scale = this.setting(
      "nametag-scale", "The size of the nametag that renders the name and health of the logged player.", Double.valueOf(1.0), this.sgRender, 0.0, 3.0
   );
   private final List<PlayerEntity> lastPlayers = new ArrayList();
   private final List<LogoutSpotsRewrite.Spot> spots = new ArrayList<>();

   public LogoutSpotsRewrite() {
      super(Venomhack420.CATEGORY, "logoutspots-rewrite", "Keeps track of where players log out.");
   }

   @EventHandler
   private void onTick(Post event) {
      this.lastPlayers.clear();

      for(Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof PlayerEntity player && player != this.mc.player) {
            this.lastPlayers.add(player);
         }
      }
   }

   @EventHandler
   private void onLogout(PlayerListChangeEvent.Leave event) {
      for(PlayerEntity player : this.lastPlayers) {
         if (event.getPlayer().getProfile().getId().equals(player.getUuid())) {
            this.spots.removeIf(spot -> spot.player.getUuid().equals(player.getUuid()));
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
            player.prevBodyYaw = player.bodyYaw;
            player.prevHeadYaw = player.getHeadYaw();
            player.prevPitch = player.getPitch();
            player.lastLimbDistance = player.limbDistance;
            this.spots.add(new LogoutSpotsRewrite.Spot(player, PlayerUtils.getDimension()));
            return;
         }
      }
   }

   @EventHandler
   private void onLogin(PlayerListChangeEvent.Join event) {
      for(LogoutSpotsRewrite.Spot spot : this.spots) {
         PlayerEntity player = spot.player;
         if (player.getUuid().equals(event.getPlayer().getProfile().getId())) {
            this.spots.remove(spot);
            if (this.msg.get()) {
               StringBuilder message = new StringBuilder(player.getEntityName() + " logged back in ");
               if (this.coords.get()) {
                  message.append("at X: ")
                     .append(player.getBlockX())
                     .append(" Y: ")
                     .append(player.getBlockY())
                     .append(" Z: ")
                     .append(player.getBlockZ())
                     .append(" ");
               }

               message.append("in the ").append(spot.dimension).append(" removing their logout spot.");
               this.info(message.toString(), new Object[0]);
               return;
            }
         }
      }
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.lastPlayers.clear();
      this.spots.clear();
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      this.spots
         .forEach(
            spot -> {
               if (spot.dimension == PlayerUtils.getDimension()
                  && RandUtils.horizontalDistance(spot.player.getPos(), this.mc.player.getPos()) < (double)this.mc.gameRenderer.getViewDistance()) {
                  WireframeEntityRenderer.render(
                     event, spot.player, 1.0, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get()
                  );
               }
            }
         );
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      this.spots
         .forEach(
            spot -> {
               Vec3 vec = new Vec3();
               vec.set(spot.player, (double)event.tickDelta);
               vec.add(0.0, (double)spot.player.getEyeHeight(spot.player.getPose()) + 0.5, 0.0);
               if (spot.dimension == PlayerUtils.getDimension()
                  && RandUtils.horizontalDistance(spot.player.getPos(), this.mc.player.getPos()) < (double)this.mc.gameRenderer.getViewDistance()
                  && NametagUtils.to2D(vec, this.scale.get())) {
                  PlayerEntity player = spot.player;
                  TextRenderer renderer = TextRenderer.get();
                  NametagUtils.begin(vec);
                  double totalWidth = renderer.getWidth(player.getEntityName() + "420");
                  double height = renderer.getHeight(true);
                  float absorption = player.getAbsorptionAmount();
                  int health = Math.round(player.getHealth() + absorption);
                  double healthPercentage = (double)((float)health / (player.getMaxHealth() + absorption));
                  Color healthColor;
                  if (healthPercentage <= 0.333) {
                     healthColor = new Color(255, 25, 25);
                  } else if (healthPercentage <= 0.666) {
                     healthColor = new Color(255, 105, 25);
                  } else {
                     healthColor = new Color(25, 252, 25);
                  }
      
                  Renderer2D.COLOR.begin();
                  Renderer2D.COLOR.quad(-totalWidth * 0.5 - 1.0, -height - 1.0, totalWidth + 2.0, height + 2.0, new Color(0, 0, 0, 75));
                  Renderer2D.COLOR.render(null);
                  renderer.beginBig();
                  renderer.render(player.getEntityName(), totalWidth * -0.5, -height, PlayerUtils.getPlayerColor(player, (Color)this.nameColor.get()), true);
                  renderer.render(" " + health, totalWidth * -0.5 + renderer.getWidth(player.getEntityName()), -height, healthColor, true);
                  renderer.end();
                  NametagUtils.end();
               }
            }
         );
   }

   public void onActivate() {
      this.lastPlayers.clear();
      this.spots.clear();
   }

   public List<LogoutSpotsRewrite.Spot> getLogs() {
      return this.spots;
   }

   public boolean remove(LogoutSpotsRewrite.Spot spot) {
      return this.spots.remove(spot);
   }

   public static record Spot(PlayerEntity player, Dimension dimension) {
   }
}
