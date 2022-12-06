package venomhack.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.events.SoundInstanceEvent;

@Mixin({SoundSystem.class})
public class SoundSystemMixin {
   @Inject(
      method = {"play(Lnet/minecraft/client/sound/SoundInstance;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSoundPlayed(SoundInstance sound, CallbackInfo ci) {
      SoundInstanceEvent siEvent = SoundInstanceEvent.get(sound, sound.getSoundSet(MinecraftClient.getInstance().getSoundManager()));
      MeteorClient.EVENT_BUS.post(siEvent);
      if (siEvent.isCancelled()) {
         ci.cancel();
      }
   }
}
