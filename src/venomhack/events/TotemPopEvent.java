package venomhack.events;

import net.minecraft.entity.Entity;

public class TotemPopEvent {
   private static final TotemPopEvent INSTANCE = new TotemPopEvent();
   private Entity entity;
   private short pops;
   private boolean isTarget;

   public static TotemPopEvent get(Entity entity, short pops, boolean isTarget) {
      INSTANCE.entity = entity;
      INSTANCE.pops = pops;
      INSTANCE.isTarget = isTarget;
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public short getPops() {
      return this.pops;
   }

   public boolean isTarget() {
      return this.isTarget;
   }
}
