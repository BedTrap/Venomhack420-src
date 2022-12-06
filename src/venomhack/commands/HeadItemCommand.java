package venomhack.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.command.CommandSource;
import venomhack.utils.RandUtils;

public class HeadItemCommand extends Command {
   public HeadItemCommand() {
      super("headitem", "Allows you to put any item in your head slot.", new String[]{"cum"});
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         RandUtils.clickSlotPacket(mc.player.getInventory().selectedSlot + 36, 39, SlotActionType.SWAP);
         return 1;
      });
   }
}
