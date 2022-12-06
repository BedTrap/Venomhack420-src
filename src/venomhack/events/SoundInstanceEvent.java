package venomhack.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;

public class SoundInstanceEvent extends Cancellable {
   private static final SoundInstanceEvent INSTANCE = new SoundInstanceEvent();
   public SoundInstance soundInstance;
   public WeightedSoundSet weightedSoundSet;

   public static SoundInstanceEvent get(SoundInstance si, WeightedSoundSet wss) {
      INSTANCE.setCancelled(false);
      INSTANCE.soundInstance = si;
      INSTANCE.weightedSoundSet = wss;
      return INSTANCE;
   }
}
