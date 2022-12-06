package venomhack.modules.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.ChatHudAccessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.Visible;
import venomhack.Venomhack420;
import venomhack.enums.Fonts;
import venomhack.enums.TimeFormat;
import venomhack.gui.screens.GuideScreen;
import venomhack.modules.ModuleHelper;
import venomhack.utils.ColorUtils;

public class ChatControl extends ModuleHelper {
   private final SettingGroup sgPrefixControl = this.settings.createGroup("Prefix Control");
   private final SettingGroup sgRainbow = this.settings.createGroup("Rainbow settings");
   private final SettingGroup sgTimestamps = this.settings.createGroup("Timestamps");
   private final SettingGroup sgPlaceholders = this.settings.createGroup("Placeholders");
   private final SettingGroup sgFancy = this.settings.createGroup("Fancy Chat");
   private final SettingGroup sgPrefix = this.settings.createGroup("Prefix & Suffix");
   private final Setting<Boolean> clear = this.setting(
      "clear-chat", "Makes the chat background transparent.", Boolean.valueOf(false), this.sgGeneral, this::handleClear
   );
   private final Setting<Double> width = this.setting(
      "custom-width", "Customize your chats width beyond minecrafts default limits.", Double.valueOf(1.0), this.sgGeneral, this::handleWide, 2.0
   );
   private final Setting<Boolean> selfHighlight = this.setting(
      "highlight-self", "Highlights your name in a special color. Compatible with name protect.", Boolean.valueOf(true), this.sgGeneral
   );
   private final Setting<SettingColor> sHighlightColor = this.setting(
      "self-highlight-color", "Color to highlight your name with.", 120, 0, 255, this.sgGeneral, this.selfHighlight::get
   );
   private final Setting<Boolean> friendHighlight = this.setting("highlight-friends", "Colors your friends names.", Boolean.valueOf(false), this.sgGeneral);
   private final Setting<Boolean> rainbow = this.setting("rainbow-prefix", "Enables a proper rainbow prefix.", Boolean.valueOf(true), this.sgRainbow);
   private final Setting<Boolean> synchro = this.setting("synchronized", "Synchronizes the words.", Boolean.valueOf(true), this.sgRainbow, this.rainbow::get);
   private final Setting<Double> rainbowSpeed = this.setting(
      "rainbow-speed", "Rainbow speed for the prefix.", 0.0035, this.sgRainbow, this.rainbow::get, 0.0, 0.02, 4
   );
   private final Setting<Double> rainbowLineSpread = this.setting(
      "rainbow-line-spread", "Rainbow spread for the prefix per line.", Double.valueOf(0.05), this.sgRainbow, this.rainbow::get, 0.2
   );
   private final Setting<Double> rainbowWordSpread = this.setting(
      "rainbow-word-spread", "Rainbow spread for the prefix inside word.", Double.valueOf(0.02), this.sgRainbow, this.rainbow::get, 0.1
   );
   private final Setting<Integer> maxMessages = this.setting(
      "max-messages", "How many lines of chat to scan for rainbow.", Integer.valueOf(50), this.sgRainbow, this.rainbow::get, 100.0
   );
   private final Setting<Boolean> meteor = this.setting(
      "apply-to-meteor", "Enables the rainbow also for the meteor prefix.", Boolean.valueOf(false), this.sgRainbow, this.rainbow::get
   );
   private final Setting<Boolean> rainbowTimestamps = this.setting(
      "apply-to-timestamps", "Enables the rainbow also for the timestamps.", Boolean.valueOf(false), this.sgRainbow, this.rainbow::get
   );
   private final Setting<Boolean> otherAddons = this.setting(
      "apply-to-other-addons", "Enables the rainbow also for other addons.", Boolean.valueOf(false), this.sgRainbow, this.rainbow::get
   );
   private final Setting<List<String>> addons = this.setting(
      "",
      "What other addons prefixes to apply the rainbow effect to.",
      this.sgRainbow,
      () -> this.rainbow.get() && this.otherAddons.get(),
      new String[]{"[Ion]", "[Banana+]"}
   );
   public final Setting<String> customPrefix = this.setting(
      "custom-prefix", "The prefix for modules added by Venomhack420.", "Venomhack", this.sgPrefixControl, this::changePrefix
   );
   private final Setting<SettingColor> customPrefixColor = this.setting(
      "prefix-color", "Color of the prefix text.", 255, 0, 0, this.sgPrefixControl, () -> !this.rainbow.get(), this::changePrefix
   );
   public final Setting<String> lBracket = this.setting("left-bracket", "Left bracket for the prefix.", "[", this.sgPrefixControl, this::changePrefix);
   public final Setting<String> rBracket = this.setting("right-bracket", "Right bracket for the prefix.", "]", this.sgPrefixControl, this::changePrefix);
   private final Setting<SettingColor> prefixBracketsColor = this.setting(
      "brackets-color", "Color of the brackets.", 170, 170, 170, this.sgPrefixControl, () -> !this.rainbow.get(), this::changePrefix
   );
   private final Setting<Boolean> overwriteMeteor = this.setting(
      "always-vh-prefix", "Whether to use the custom prefix also for meteor modules.", Boolean.valueOf(false), this.sgPrefixControl, this::changePrefix
   );
   public final Setting<String> meteorText = this.setting(
      "meteor-text", "The text for the meteor prefix.", "Meteor", this.sgPrefixControl, () -> !this.overwriteMeteor.get(), s -> this.changeMeteorPrefix()
   );
   private final Setting<SettingColor> meteorColor = this.setting(
      "meteor-color",
      "Color of the meteor prefix text.",
      145,
      61,
      226,
      this.sgPrefixControl,
      () -> !this.overwriteMeteor.get() && (!this.meteor.get() || !this.rainbow.get()),
      s -> this.changeMeteorPrefix()
   );
   public final Setting<String> lBracketMeteor = this.setting(
      "meteor-left-bracket",
      "Left bracket for the meteor prefix.",
      "[",
      this.sgPrefixControl,
      () -> !this.overwriteMeteor.get(),
      s -> this.changeMeteorPrefix()
   );
   public final Setting<String> rBracketMeteor = this.setting(
      "meteor-right-bracket",
      "Right bracket for the meteor prefix.",
      "]",
      this.sgPrefixControl,
      () -> !this.overwriteMeteor.get(),
      s -> this.changeMeteorPrefix()
   );
   private final Setting<SettingColor> meteorPrefixBracketsColor = this.setting(
      "meteor-brackets-color",
      "Color of the meteor brackets.",
      170,
      170,
      170,
      this.sgPrefixControl,
      () -> !this.overwriteMeteor.get() && (!this.meteor.get() || !this.rainbow.get()),
      s -> this.changeMeteorPrefix()
   );
   private final Setting<Boolean> timestamps = this.setting(
      "timestamps", "Adds client side time stamps to the beginning of chat messages.", Boolean.valueOf(false), this.sgTimestamps
   );
   private final Setting<TimeFormat> format = this.setting(
      "format", "What time format to use.", TimeFormat.TWENTY_FOUR_HOUR, this.sgTimestamps, this.timestamps::get
   );
   private final Setting<Boolean> amPm = this.setting(
      "include-am-pm",
      "Whether to add the 'AM' or 'PM' suffix when using 12h format.",
      Boolean.valueOf(true),
      this.sgTimestamps,
      () -> this.timestamps.get() && this.format.get() == TimeFormat.TWELVE_HOUR
   );
   private final Setting<Boolean> timestampsSeconds = this.setting(
      "include-seconds", "Whether to add seconds to the timestamps or not.", Boolean.valueOf(false), this.sgTimestamps, this.timestamps::get
   );
   private final Setting<SettingColor> timestampsColor = this.setting(
      "timestamp-color",
      "Color of the timestamps.",
      170,
      170,
      170,
      this.sgTimestamps,
      () -> this.timestamps.get() && (!this.rainbow.get() || !this.rainbowTimestamps.get())
   );
   public final Setting<String> lBracketTimestamps = this.setting(
      "left-bracket", "Left bracket for the timestamps.", "<", this.sgTimestamps, this.timestamps::get
   );
   public final Setting<String> rBracketTimestamps = this.setting(
      "right-bracket", "Right bracket for the timestamps.", ">", this.sgTimestamps, this.timestamps::get
   );
   private final Setting<SettingColor> timestampsBracketsColor = this.setting(
      "brackets-color",
      "Color of the timestamps' brackets.",
      170,
      170,
      170,
      this.sgTimestamps,
      () -> this.timestamps.get() && (!this.rainbow.get() || !this.rainbowTimestamps.get())
   );
   private final Setting<List<String>> placeholders = this.setting(
      "placeholders",
      "Add placeholders that get replaced when sending messages. Seperate placeholder and message with a space.",
      this.sgPlaceholders,
      null,
      new String[]{":skull: ☠", ":lit: \ud83d\udd25", ":discord: https://discord.gg/VqRd4MJkbY", "shit sheeeeeeiiiittttt"}
   );
   private final Setting<Boolean> fancy = this.setting("fancy-chat", "Makes your messages ғᴀɴᴄʏ!", Boolean.valueOf(false), this.sgFancy);
   private final Setting<Fonts> font = this.setting("font", "Which font to use", Fonts.FANCY, this.sgFancy, this.fancy::get);
   private final Setting<Boolean> prefix = this.setting("prefix", "Adds a prefix to your chat messages.", Boolean.valueOf(false), this.sgPrefix);
   private final Setting<Boolean> prefixRandom = this.setting(
      "random", "Uses a random number as your prefix.", Boolean.valueOf(false), this.sgPrefix, this.prefix::get
   );
   private final Setting<String> prefixText = this.setting(
      "text", "The text to add as your prefix.", "> ", this.sgPrefix, () -> !this.prefixRandom.get() && this.prefix.get()
   );
   private final Setting<Fonts> prefixFont = this.setting(
      "prefix-font", "Set a font for your prefix.", Fonts.DEFAULT, this.sgPrefix, () -> !this.prefixRandom.get() && this.prefix.get()
   );
   private final Setting<Boolean> suffix = this.setting("suffix", "Adds a suffix to your chat messages.", Boolean.valueOf(false), this.sgPrefix);
   private final Setting<Boolean> suffixRandom = this.setting(
      "random", "Uses a random number as your suffix.", Boolean.valueOf(false), this.sgPrefix, this.suffix::get
   );
   private final Setting<String> suffixText = this.setting(
      "text", "The text to add as your suffix.", " | Venomhack", this.sgPrefix, () -> !this.suffixRandom.get() && this.suffix.get()
   );
   private final Setting<Fonts> suffixFont = this.setting(
      "suffix-font", "Set a font for your suffix.", Fonts.GREEKISH, this.sgPrefix, () -> !this.suffixRandom.get() && this.suffix.get()
   );
   private final Color rgb = new Color(255, 255, 255);
   private double rainbowHue1;
   private double rainbowHue2;

   public ChatControl() {
      super(Venomhack420.CATEGORY, "chat-control", "Grants you full control over your chat experience.");
   }

   @EventHandler
   private synchronized void onRender(Render2DEvent event) {
      if (this.rainbow.get()) {
         this.rainbowHue1 += this.rainbowSpeed.get() * 0.45992073;
         if (this.rainbowHue1 > 1.0) {
            --this.rainbowHue1;
         } else if (this.rainbowHue1 < -1.0) {
            ++this.rainbowHue1;
         }

         this.rainbowHue2 = this.rainbowHue1;
         List<class_7590> visible = ((ChatHudAccessor)this.mc.inGameHud.getChatHud()).getVisibleMessages();

         for(int index = Math.min(this.maxMessages.get(), visible.size() - 1); index > -1; --index) {
            class_7590 line = (class_7590)visible.get(index);
            OrderedText content = line.comp_896();
            if (content == null) {
               return;
            }

            MutableText parsed = Text.literal("");
            int totalChars = 0;
            content.accept((i, style, codePoint) -> {
               parsed.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
               return true;
            });
            if (this.timestamps.get() && this.rainbowTimestamps.get()) {
               int stampIndexBegin = parsed.getString().indexOf((String)this.lBracketTimestamps.get())
                  - (parsed.getString().length() - parsed.getSiblings().size());
               if (((String)this.lBracketTimestamps.get()).isEmpty() && parsed.getString().indexOf("  ") == 0) {
                  stampIndexBegin = 2;
               }

               int min = Math.min(stampIndexBegin + this.timestampText().getString().length() - 1, parsed.getString().length() - 1);
               if (stampIndexBegin > -1
                  && parsed.getString().substring(stampIndexBegin, min).startsWith((String)this.lBracketTimestamps.get())
                  && parsed.getString().substring(stampIndexBegin, min).endsWith(this.getTimeStampsRight())) {
                  String time = parsed.getString().substring(stampIndexBegin, min);
                  if (time.startsWith((String)this.lBracketTimestamps.get()) && time.endsWith((String)this.rBracketTimestamps.get())) {
                     parsed.getSiblings().subList(stampIndexBegin, min).clear();
                     parsed.getSiblings().add(stampIndexBegin, this.applyRgb(time));
                     totalChars = time.length();
                  }
               }
            }

            String vPrefix = (String)this.lBracket.get() + (String)this.customPrefix.get() + (String)this.rBracket.get();
            int vIndex = parsed.getString().indexOf(vPrefix) - (parsed.getString().length() - parsed.getSiblings().size());
            if (vIndex > -1) {
               parsed.getSiblings().subList(vIndex, Math.min(vIndex + vPrefix.length(), parsed.getString().length() - 1)).clear();
               parsed.getSiblings().add(vIndex, this.applyRgb(vPrefix));
               totalChars += vPrefix.length();
            }

            if (this.meteor.get()) {
               String mPrefix = (String)this.lBracketMeteor.get() + (String)this.meteorText.get() + (String)this.rBracketMeteor.get();
               int mIndex = parsed.getString().indexOf(mPrefix) - (parsed.getString().length() - parsed.getSiblings().size());
               if (mIndex > -1) {
                  parsed.getSiblings().subList(mIndex, Math.min(mIndex + mPrefix.length(), parsed.getString().length() - 1)).clear();
                  parsed.getSiblings().add(mIndex, this.applyRgb(mPrefix));
                  totalChars += mPrefix.length();
               }
            }

            if (this.otherAddons.get()) {
               for(String addon : (List)this.addons.get()) {
                  int addonIndex = parsed.getString().indexOf(addon) - (parsed.getString().length() - parsed.getSiblings().size());
                  if (addonIndex > -1) {
                     parsed.getSiblings().subList(addonIndex, Math.min(addonIndex + addon.length(), parsed.getString().length() - 1)).clear();
                     parsed.getSiblings().add(addonIndex, this.applyRgb(addon));
                     totalChars += addon.length();
                  }
               }
            }

            this.rainbowHue2 -= this.rainbowLineSpread.get();
            if (this.synchro.get()) {
               this.rainbowHue2 += this.rainbowWordSpread.get() * (double)totalChars;
            }

            try {
               ((ChatHudAccessor)this.mc.inGameHud.getChatHud())
                  .getVisibleMessages()
                  .set(index, new class_7590(line.comp_895(), parsed.asOrderedText(), null, true));
            } catch (Exception var13) {
            }
         }
      }
   }

   @EventHandler(
      priority = 199
   )
   private void onMessageRecieve(ReceiveMessageEvent event) {
      Text message = event.getMessage();
      MutableText parsed = Text.literal("");
      message.asOrderedText().accept((i, style, codePoint) -> {
         parsed.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
         return true;
      });
      if (this.timestamps.get()) {
         parsed.getSiblings().add(0, this.timestampText());
         OrderedText temp = parsed.asOrderedText();
         parsed.getSiblings().clear();
         temp.accept((i, style, codePoint) -> {
            parsed.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
         });
      }

      if (this.selfHighlight.get()) {
         String selfName = ((NameProtect)Modules.get().get(NameProtect.class)).getName(this.mc.player.getEntityName());
         int nameIndex = parsed.getString().indexOf(selfName);
         if (nameIndex > -1) {
            parsed.getSiblings().subList(nameIndex, nameIndex + selfName.length()).clear();
            parsed.getSiblings().add(nameIndex, ColorUtils.coloredText(selfName, (Color)this.sHighlightColor.get()));
         }
      }

      if (this.friendHighlight.get()) {
         for(Friend friend : Friends.get()) {
            if (!friend.name.equals(this.mc.player.getEntityName()) || !this.selfHighlight.get()) {
               int nameIndex = parsed.getString().indexOf(friend.name) - (parsed.getString().length() - parsed.getSiblings().size());
               if (nameIndex > -1) {
                  parsed.getSiblings().subList(nameIndex, nameIndex + friend.name.length()).clear();
                  parsed.getSiblings().add(nameIndex, ColorUtils.coloredText(friend.name, (Color)Config.get().friendColor.get()));
                  break;
               }
            }
         }
      }

      event.setMessage(parsed);
   }

   @EventHandler(
      priority = 201
   )
   private void onMessageSend(SendMessageEvent event) {
      StringBuilder builder = new StringBuilder();
      if (this.prefix.get()) {
         builder.append(this.getAffix((String)this.prefixText.get(), (Fonts)this.prefixFont.get(), this.prefixRandom.get())).append(" ");
      }

      String message = event.message;

      for(String placeholder : (List)this.placeholders.get()) {
         String[] array = placeholder.split(" ", 2);
         if (array.length >= 1) {
            message = message.replace(array[0], array[1]);
         }
      }

      if (this.fancy.get()) {
         message = ((Fonts)this.font.get()).apply(message);
      }

      builder.append(message);
      if (this.suffix.get()) {
         builder.append(" ").append(this.getAffix((String)this.suffixText.get(), (Fonts)this.suffixFont.get(), this.suffixRandom.get()));
      }

      event.message = builder.toString();
   }

   private MutableText applyRgb(String text) {
      MutableText prefix = Text.literal("");

      for(int i = 0; i < text.length(); ++i) {
         int c = java.awt.Color.HSBtoRGB((float)this.rainbowHue2, 1.0F, 1.0F);
         this.rgb.r = Color.toRGBAR(c);
         this.rgb.g = Color.toRGBAG(c);
         this.rgb.b = Color.toRGBAB(c);
         prefix.append(ColorUtils.coloredText(text.substring(i, i + 1), this.rgb));
         this.rainbowHue2 -= this.rainbowWordSpread.get();
      }

      return prefix;
   }

   private String getTimeStampsRight() {
      if (((String)this.rBracketTimestamps.get()).isEmpty()) {
         return this.format.get() == TimeFormat.TWENTY_FOUR_HOUR ? "" : "M";
      } else {
         return (String)this.rBracketTimestamps.get();
      }
   }

   public MutableText timestampText() {
      StringBuilder timeText = new StringBuilder();
      if (this.format.get() == TimeFormat.TWENTY_FOUR_HOUR) {
         timeText.append("HH:mm");
      } else {
         timeText.append("hh:mm");
      }

      if (this.timestampsSeconds.get()) {
         timeText.append(":ss");
      }

      if (this.format.get() == TimeFormat.TWELVE_HOUR && this.amPm.get()) {
         timeText.append(" aa");
      }

      return ColorUtils.coloredText((String)this.lBracketTimestamps.get(), (Color)this.timestampsBracketsColor.get())
         .append(ColorUtils.coloredText(new SimpleDateFormat(timeText.toString()).format(new Date()), (Color)this.timestampsColor.get()))
         .append(ColorUtils.coloredText((String)this.rBracketTimestamps.get(), (Color)this.timestampsBracketsColor.get()))
         .append(" ");
   }

   private String getAffix(String text, Fonts font, boolean random) {
      return random ? String.format("(%03d) ", Utils.random(0, 1000)) : font.apply(text);
   }

   private boolean handleClear(boolean bool) {
      if (this.clear.get()) {
         this.mc.options.getTextBackgroundOpacity().setValue(0.0);
      } else {
         this.mc.options.getTextBackgroundOpacity().setValue(0.5);
      }

      return true;
   }

   private boolean handleWide(double doub) {
      this.mc.options.getChatWidth().setValue((Double)this.width.get());
      return true;
   }

   private void changePrefix(SettingColor color) {
      this.changePrefix("");
   }

   private void changePrefix(boolean bool) {
      this.changePrefix("");
   }

   private void changePrefix(String string) {
      ChatUtils.registerCustomPrefix("venomhack", this::prefix);
      if (this.overwriteMeteor.get()) {
         ChatUtils.registerCustomPrefix("meteordevelopment", this::prefix);
      } else {
         ChatUtils.registerCustomPrefix("meteordevelopment", this::customMeteorPrefix);
      }
   }

   private void changeMeteorPrefix() {
      ChatUtils.registerCustomPrefix("meteordevelopment", this::customMeteorPrefix);
   }

   private MutableText prefix() {
      MutableText prefix = ColorUtils.coloredText((String)this.lBracket.get(), (Color)this.prefixBracketsColor.get());
      prefix.append(ColorUtils.coloredText((String)this.customPrefix.get(), (Color)this.customPrefixColor.get()));
      prefix.append(ColorUtils.coloredText((String)this.rBracket.get(), (Color)this.prefixBracketsColor.get()));
      return prefix.append(" ");
   }

   private MutableText customMeteorPrefix() {
      MutableText prefix = ColorUtils.coloredText((String)this.lBracketMeteor.get(), (Color)this.meteorPrefixBracketsColor.get());
      prefix.append(ColorUtils.coloredText((String)this.meteorText.get(), (Color)this.meteorColor.get()));
      prefix.append(ColorUtils.coloredText((String)this.rBracketMeteor.get(), (Color)this.meteorPrefixBracketsColor.get()));
      return prefix.append(" ");
   }

   private MutableText defaultPrefix() {
      MutableText prefix = Text.literal("[").formatted(Formatting.GRAY);
      prefix.append(ColorUtils.coloredText("Meteor", MeteorClient.ADDON.color));
      prefix.append(Text.literal("]").formatted(Formatting.GRAY));
      return prefix.append(" ");
   }

   public void onActivate() {
      this.changePrefix(true);
      this.handleClear(true);
      this.handleWide(1.0);
   }

   public void onDeactivate() {
      ChatUtils.registerCustomPrefix("venomhack", this::defaultPrefix);
      ChatUtils.registerCustomPrefix("meteordevelopment", this::defaultPrefix);
      if (this.mc.options.getTextBackgroundOpacity().getValue() == 0.0) {
         this.mc.options.getTextBackgroundOpacity().setValue(0.5);
      }

      if (this.mc.options.getChatWidth().getValue() > 1.0) {
         this.mc.options.getChatWidth().setValue(1.0);
      }
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton placeholders = (WButton)list.add(theme.button("Placeholders")).expandX().widget();
      placeholders.action = () -> new GuideScreen().show();
      return list;
   }
}
