package venomhack.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.util.math.MatrixStack;

public class GuideScreen {
   private String title = "Placeholders";
   private final List<String> messages = new LinkedList<>(
      Arrays.asList(
         "These are the default placeholders.",
         "These placeholders can't be modified but work everywhere.",
         "",
         "{player} displays the last player targeted.",
         "{ks} displays your current kill streak.",
         "{ksSuffix} displays the proper grammar for your current kill streak, e.g. 'nd' if your kill streak is 2 or 'st' if its 1.",
         "{server.ip} displays the ip address of the server you are currently playing on.",
         "{server.name} displays the name of the current server.",
         "{server.online} displays the amount of players online.",
         "{ping} displays your current ping.",
         "{deaths} displays the count of your deaths.",
         "{kd} displays your kills / deaths ratio.",
         "",
         "Auto EZ and notifier specific:",
         "{pops} for the amount of totem pops of the target.",
         "{totem} to get the singular or plural of totem.",
         ""
      )
   );

   public GuideScreen withContents(String... contents) {
      this.messages.clear();
      this.messages.addAll(Arrays.asList(contents));
      return this;
   }

   public GuideScreen title(String title) {
      this.title = title;
      return this;
   }

   public void show() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> MeteorClient.mc.setScreen(new GuideScreen.PromptScreen()));
      } else {
         MeteorClient.mc.setScreen(new GuideScreen.PromptScreen());
      }
   }

   private class PromptScreen extends WindowScreen {
      public PromptScreen() {
         super(GuiThemes.get(), GuideScreen.this.title);
      }

      public void initWidgets() {
         for(String line : GuideScreen.this.messages) {
            this.add(this.theme.label(line)).expandX();
         }

         this.add(this.theme.horizontalSeparator()).expandX();
         WHorizontalList list = (WHorizontalList)this.add(this.theme.horizontalList()).expandX().widget();
         WButton okButton = (WButton)list.add(this.theme.button("Ok")).expandX().widget();
         okButton.action = this::close;
      }

      public void renderBackground(MatrixStack matrices) {
         this.renderBackground(matrices, 0);
      }
   }
}
