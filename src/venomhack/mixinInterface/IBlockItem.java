package venomhack.mixinInterface;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BlockState;

public interface IBlockItem {
   boolean canBePlaced(ItemPlacementContext var1, BlockState var2);

   BlockState getThePlacementState(ItemPlacementContext var1);
}
