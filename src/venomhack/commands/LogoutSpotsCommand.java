package venomhack.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import venomhack.modules.render.LogoutSpotsRewrite;

public class LogoutSpotsCommand extends Command {
   private final LogoutSpotsRewrite logoutSpots = (LogoutSpotsRewrite)Modules.get().get(LogoutSpotsRewrite.class);

   public LogoutSpotsCommand() {
      super("logoutspots", "Allows certain interactions with the logoutspots rewrite.", new String[]{"logs"});
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("list").executes(context -> {
         if (this.logoutSpots.getLogs().isEmpty()) {
            this.info(Text.literal("No logout spots saved.").formatted(Formatting.GRAY));
         } else {
            this.info(Text.literal("All logout spots:").formatted(Formatting.GRAY));

            for(LogoutSpotsRewrite.Spot spot : this.logoutSpots.getLogs()) {
               PlayerEntity player = spot.player();
               MutableText text = Text.literal(player.getEntityName());
               text.append("'s logout spot is at ");
               text.append("X: " + player.getBlockX() + " Y: " + player.getBlockY() + " Z: " + player.getBlockZ());
               text.append(" in the " + spot.dimension() + ".");
               ChatUtils.sendMsg(text.formatted(Formatting.AQUA));
            }
         }

         return 1;
      }));
      builder.then(literal("overworld").executes(context -> {
         this.handleDimension(Dimension.Overworld);
         return 1;
      }));
      builder.then(literal("nether").executes(context -> {
         this.handleDimension(Dimension.Nether);
         return 1;
      }));
      builder.then(literal("end").executes(context -> {
         this.handleDimension(Dimension.End);
         return 1;
      }));
   }

   private void handleDimension(Dimension dim) {
      boolean hasntSent = true;

      for(LogoutSpotsRewrite.Spot spot : this.logoutSpots.getLogs()) {
         if (spot.dimension() == dim) {
            if (hasntSent) {
               this.info(Text.literal("All logout spots in the " + dim + ":").formatted(Formatting.GRAY));
               hasntSent = false;
            }

            PlayerEntity player = spot.player();
            MutableText text = Text.literal(player.getEntityName());
            text.append("'s logout spot is at ");
            text.append("X: " + player.getBlockX() + " Y: " + player.getBlockY() + " Z: " + player.getBlockZ() + ".");
            ChatUtils.sendMsg(text.formatted(Formatting.AQUA));
         }
      }

      if (hasntSent) {
         this.info(Text.literal("No logout spots in the " + dim + ".").formatted(Formatting.GRAY));
      }
   }
}
