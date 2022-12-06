package venomhack.modules.render;

import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.util.Pair;
import venomhack.Venomhack420;
import venomhack.events.SoundInstanceEvent;
import venomhack.modules.ModuleHelper;

public class SoundEsp extends ModuleHelper {
   private final Setting<Double> scale = this.setting("scale", "The scale of the nametags.", Double.valueOf(1.5));
   private final Setting<SettingColor> background = this.setting("background-color", "The color of the nametags backgrounds.", 0, 0, 0, 75);
   private final Setting<SettingColor> names = this.setting("primary-color", "The color of the nametags names.", 255, 255, 255, 255);
   private final Setting<Integer> renderTime = this.setting(
      "render-time", "How many ticks to keep rendering a sound event's nametag for.", Integer.valueOf(50), 1.0, 100.0
   );
   private final Setting<Boolean> culling = this.setting(
      "culling", "Only render a certain number of nametags until a certain distance.", Boolean.valueOf(false)
   );
   private final Setting<Integer> maxCullCount = this.setting(
      "culling-count", "Only render up to this many nametags.", Integer.valueOf(50), this.sgGeneral, this.culling::get, 1.0, 100.0
   );
   private final Setting<Boolean> debug = this.setting("debug-boxes", "Renders a box where the sound plays.", Boolean.valueOf(false));
   private final Map<Pair<SoundInstance, WeightedSoundSet>, Long> soundList = new HashMap<>();
   private final Map<Pair<SoundInstance, WeightedSoundSet>, Long> toremove = new HashMap<>();

   public SoundEsp() {
      super(Venomhack420.CATEGORY, "sound-esp", "Shows you where sounds are playing.");
   }

   @EventHandler
   private void onSound(SoundInstanceEvent event) {
      if (!this.culling.get() || this.soundList.size() <= this.maxCullCount.get()) {
         this.soundList.put(new Pair(event.soundInstance, event.weightedSoundSet), System.currentTimeMillis());
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (!this.soundList.isEmpty()) {
         this.soundList.forEach((pair, time) -> {
            if (System.currentTimeMillis() - time > (long)(this.renderTime.get() * 50)) {
               this.toremove.put(pair, time);
            }
         });
      }

      this.toremove.forEach(this.soundList::remove);
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      this.soundList.forEach((pair, time) -> {
         SoundInstance sound = (SoundInstance)pair.getLeft();
         WeightedSoundSet soundSet = (WeightedSoundSet)pair.getRight();
         if (soundSet.getSubtitle() != null) {
            Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            if (NametagUtils.to2D(soundPos, this.scale.get())) {
               TextRenderer renderer = TextRenderer.get();
               NametagUtils.begin(soundPos);
               double width = renderer.getWidth(soundSet.getSubtitle().getString(), true);
               double height = renderer.getHeight(true);
               Renderer2D.COLOR.begin();
               Renderer2D.COLOR.quad(-width / 2.0 - 1.0, -height / 2.0 - 1.0, width + 2.0, height + 2.0, (Color)this.background.get());
               Renderer2D.COLOR.render(null);
               TextRenderer.get().begin(1.0, false, true);
               TextRenderer.get().render(soundSet.getSubtitle().getString(), -width / 2.0, -height / 2.0, (Color)this.names.get());
               TextRenderer.get().end();
               NametagUtils.end();
            }
         }
      });
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.debug.get()) {
         this.soundList
            .forEach(
               (pair, time) -> {
                  SoundInstance sound = (SoundInstance)pair.getLeft();
                  event.renderer
                     .boxLines(
                        sound.getX() - 0.25,
                        sound.getY() - 0.25,
                        sound.getZ() - 0.25,
                        sound.getX() + 0.25,
                        sound.getY() + 0.25,
                        sound.getZ() + 0.25,
                        new Color(255, 241, 145, 255),
                        0
                     );
               }
            );
      }
   }
}
