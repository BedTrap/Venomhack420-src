package venomhack.mixinInterface;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public interface INoInteract {
   boolean shouldInteract(BlockHitResult var1, Hand var2);
}
