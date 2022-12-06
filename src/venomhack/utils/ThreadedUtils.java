package venomhack.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import venomhack.modules.combat.AutoAnchor;

public class ThreadedUtils {
   public static ExecutorService aaExecutor;
   public static ScheduledExecutorService antiCityExecutor;

   public static void init() {
      aaExecutor = Executors.newSingleThreadExecutor();
      antiCityExecutor = Executors.newSingleThreadScheduledExecutor();
   }

   public static class AaCalcs implements Runnable {
      private static final AutoAnchor aar = (AutoAnchor)Modules.get().get(AutoAnchor.class);
      private final ThreadedUtils.CalcType type;
      public static int placeDelayLeft = 0;
      public static int breakDelayLeft = 0;

      public AaCalcs(ThreadedUtils.CalcType type) {
         this.type = type;
      }

      @Override
      public void run() {
         switch(this.type) {
            case Place:
               --placeDelayLeft;
               if (placeDelayLeft <= 0) {
                  aar.doPlaceThreaded();
               }
            case Break:
               --breakDelayLeft;
               if (breakDelayLeft <= 0) {
                  aar.doBreak();
               }
         }
      }

      public static void resetDelays(LivingEntity targ) {
         if (UtilsPlus.isSurrounded(targ, true, true) && aar.holeDelays.get()) {
            placeDelayLeft = aar.holePlaceDelay.get();
            breakDelayLeft = aar.holeBreakDelay.get();
         } else {
            placeDelayLeft = aar.placeDelay.get();
            breakDelayLeft = aar.breakDelay.get();
         }
      }
   }

   public static enum CalcType {
      Place,
      Break;
   }
}
