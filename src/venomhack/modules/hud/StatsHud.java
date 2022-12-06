package venomhack.modules.hud;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import venomhack.Venomhack420;

public class StatsHud extends HudElement {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> kills = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("kill-counter")).description("Displays how many kills you got in this session.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> killstreak = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("killstreak")).description("Displays how many kills you got in a row.")).defaultValue(true)).build()
      );
   private final Setting<Boolean> kayDee = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("k/d")).description("Displays your current kills / deaths ratio.")).defaultValue(true)).build());
   private final Setting<Boolean> deaths = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("death-counter")).description("Displays how many times you died in this session."))
               .defaultValue(true))
            .build()
      );
   public static final HudElementInfo<StatsHud> INFO = new HudElementInfo(
      Venomhack420.HUD_GROUP, "statistics", "Displays certain pvp related statistics.", StatsHud::new
   );

   public StatsHud() {
      super(INFO);
   }

   public void tick(HudRenderer renderer) {
      double width = renderer.textWidth("Statistics");
      double height = renderer.textHeight();
      if (MeteorClient.mc.world == null) {
         this.box.setSize(width, height);
      } else {
         if (this.kills.get()) {
            width = Math.max(width, renderer.textWidth("Kills: " + Venomhack420.STATS.getKills()));
            height += renderer.textHeight() + 2.0;
         }

         if (this.killstreak.get()) {
            width = Math.max(width, renderer.textWidth("Killstreak: " + Venomhack420.STATS.getKillStreak()));
            height += renderer.textHeight() + 2.0;
         }

         if (this.kayDee.get()) {
            width = Math.max(width, renderer.textWidth("k/d: " + Venomhack420.STATS.getKayDee()));
            height += renderer.textHeight() + 2.0;
         }

         if (this.deaths.get()) {
            width = Math.max(width, renderer.textWidth("Deaths: " + Venomhack420.STATS.getDeaths()));
         }

         this.box.setSize(width, height);
      }
   }

   public void render(HudRenderer renderer) {
      double x = (double)this.box.x;
      double y = (double)this.box.y;
      if (MeteorClient.mc.world != null && (this.kills.get() || this.killstreak.get() || this.kayDee.get() || this.deaths.get())) {
         if (this.kills.get()) {
            renderer.text("Kills:", x, y, Color.WHITE, true);
            renderer.text(" " + Venomhack420.STATS.getKills(), x + renderer.textWidth("Kills:"), y, Color.WHITE, true);
            y += renderer.textHeight() + 2.0;
         }

         if (this.killstreak.get()) {
            renderer.text("Killstreak:", x, y, Color.WHITE, true);
            renderer.text(" " + Math.round((float)Venomhack420.STATS.getKillStreak()), x + renderer.textWidth("Killstreak:"), y, Color.WHITE, true);
            y += renderer.textHeight() + 2.0;
         }

         if (this.kayDee.get()) {
            renderer.text("K/D:", x, y, Color.WHITE, true);
            renderer.text(" " + Venomhack420.STATS.getKayDee(), x + renderer.textWidth("K/D:"), y, Color.WHITE, true);
            y += renderer.textHeight() + 2.0;
         }

         if (this.deaths.get()) {
            renderer.text("Deaths:", x, y, Color.WHITE, true);
            renderer.text(" " + Venomhack420.STATS.getDeaths(), x + renderer.textWidth("Deaths:"), y, Color.WHITE, true);
         }
      } else {
         renderer.text("Statistics", x, y, Color.WHITE, true);
      }
   }
}
