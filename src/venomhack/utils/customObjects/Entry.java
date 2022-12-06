package venomhack.utils.customObjects;

import java.util.UUID;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Entry {
   public Vec3d vec3d;
   public final PlayerEntity player;
   public final UUID uuid;
   public final int health;
   public final int maxHealth;
   public final String healthText;
   public final Dimension dimension;

   public Entry(PlayerEntity entity, Dimension dimension) {
      this.player = entity;
      this.dimension = dimension;
      this.vec3d = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
      this.uuid = entity.getUuid();
      this.health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
      this.maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());
      this.healthText = " " + this.health;
   }
}
