package venomhack.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.misc.text.StringCharacterVisitor;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.Visible;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.chat.ChatControl;
import venomhack.utils.TextUtils;

@Mixin({ChatHud.class})
public class ChatHudMixin {
   @Shadow
   @Final
   private List<class_7590> visibleMessages;
   @Shadow
   private int scrolledLines;
   @Unique
   private static final Identifier METEOR_CHAT_ICON = new Identifier("meteor-client", "textures/icons/chat/meteor.png");
   @Unique
   private static final Identifier VENOMHACK_CHAT_ICON = new Identifier("venomhack", "chat/icon.png");
   @Unique
   private static final Identifier BARITONE_CHAT_ICON = new Identifier("meteor-client", "textures/icons/chat/baritone.png");

   @Shadow
   private static double getMessageOpacityMultiplier(int age) {
      throw new AssertionError();
   }

   @Shadow
   private boolean isChatFocused() {
      return MeteorClient.mc.currentScreen instanceof ChatScreen;
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void renderTail(MatrixStack matrices, int tickDelta, CallbackInfo ci) {
      if (((ChatControl)Modules.get().get(ChatControl.class)).isActive()) {
         if (((BetterChat)Modules.get().get(BetterChat.class)).displayPlayerHeads()) {
            if (MeteorClient.mc.options.getChatVisibility().getValue() != ChatVisibility.HIDDEN) {
               int maxLineCount = MeteorClient.mc.inGameHud.getChatHud().getVisibleLineCount();
               double d = MeteorClient.mc.options.getChatOpacity().getValue() * 0.9F + 0.1F;
               double g = 9.0 * (MeteorClient.mc.options.getChatLineSpacing().getValue() + 1.0);
               double h = -8.0 * (MeteorClient.mc.options.getChatLineSpacing().getValue() + 1.0)
                  + 4.0 * MeteorClient.mc.options.getChatLineSpacing().getValue()
                  + 8.0;
               matrices.push();
               matrices.translate(2.0, -0.1F, 10.0);
               RenderSystem.enableBlend();

               for(int m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < maxLineCount; ++m) {
                  class_7590 chatHudLine = (class_7590)this.visibleMessages.get(m + this.scrolledLines);
                  if (chatHudLine != null) {
                     int x = tickDelta - chatHudLine.comp_895();
                     if (x < 200 || this.isChatFocused()) {
                        double o = this.isChatFocused() ? 1.0 : getMessageOpacityMultiplier(x);
                        if (o * d > 0.01) {
                           double s = (double)(-m) * g;
                           StringCharacterVisitor visitor = new StringCharacterVisitor();
                           chatHudLine.comp_896().accept(visitor);
                           this.drawThis(matrices, visitor.result.toString(), (int)(s + h), (float)(o * d));
                        }
                     }
                  }
               }

               RenderSystem.disableBlend();
               matrices.pop();
            }
         }
      }
   }

   private void drawThis(MatrixStack matrices, String line, int y, float opacity) {
      ChatControl cc = (ChatControl)Modules.get().get(ChatControl.class);
      String timeStampRegex = "^\\s{0,2}("
         + TextUtils.addBackSlashes((String)cc.lBracketTimestamps.get())
         + "[0-9]{1,2}:[0-9]{1,2}(:[0-9]{1,2})?(\\s?(?:A|P)\\.?M\\.?)?"
         + TextUtils.addBackSlashes((String)cc.rBracketTimestamps.get())
         + "\\s)?";
      Pattern METEOR_PREFIX_REGEX = Pattern.compile(
         timeStampRegex
            + TextUtils.addBackSlashes((String)cc.lBracketMeteor.get())
            + (String)cc.meteorText.get()
            + TextUtils.addBackSlashes((String)cc.rBracketMeteor.get())
      );
      Pattern VENOMHACK_PREFIX_REGEX = Pattern.compile(
         timeStampRegex
            + TextUtils.addBackSlashes((String)cc.lBracket.get())
            + (String)cc.customPrefix.get()
            + TextUtils.addBackSlashes((String)cc.rBracket.get())
      );
      Pattern BARITONE_PREFIX_REGEX = Pattern.compile(timeStampRegex + "\\[Baritone]");
      boolean found = false;
      if (METEOR_PREFIX_REGEX.matcher(line).find()) {
         RenderSystem.setShaderTexture(0, METEOR_CHAT_ICON);
         found = true;
      } else if (BARITONE_PREFIX_REGEX.matcher(line).find()) {
         RenderSystem.setShaderTexture(0, BARITONE_CHAT_ICON);
         found = true;
      } else if (VENOMHACK_PREFIX_REGEX.matcher(line).find()) {
         RenderSystem.setShaderTexture(0, VENOMHACK_CHAT_ICON);
         found = true;
      }

      if (found) {
         matrices.push();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
         matrices.translate(0.0, (double)y, 0.0);
         matrices.scale(0.125F, 0.125F, 1.0F);
         DrawableHelper.drawTexture(matrices, 0, 0, 0.0F, 0.0F, 64, 64, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         matrices.pop();
      }
   }
}
