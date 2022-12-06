package venomhack.modules;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.PotionSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.settings.StatusEffectAmplifierMapSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MyPotion;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Formatting;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import venomhack.mixinInterface.ISpoofName;

public class ModuleHelper extends Module {
   protected final SettingGroup sgGeneral = this.settings.getDefaultGroup();

   public String title() {
      String spoofName = ((ISpoofName)this).getSpoofName();
      return spoofName.isEmpty() ? this.title : spoofName;
   }

   public Setting<Boolean> setting(String name, String description, boolean defaultValue, String group) {
      return this.groupFromString(group)
         .add(((Builder)((Builder)((Builder)new Builder().name(name)).description(description)).defaultValue(defaultValue)).build());
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue) {
      return this.setting(name, description, defaultValue, this.sgGeneral);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, IVisible visible) {
      return this.setting(name, description, defaultValue, this.sgGeneral, visible, null, null);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, Consumer<T> onChanged) {
      return this.setting(name, description, defaultValue, this.sgGeneral, null, onChanged);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, IVisible visible, Consumer<T> onChanged) {
      return this.setting(name, description, defaultValue, this.sgGeneral, visible, onChanged, null);
   }

   public <T> Setting<T> setting(
      String name, String description, T defaultValue, IVisible visible, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated
   ) {
      return this.setting(name, description, defaultValue, this.sgGeneral, visible, onChanged, onModuleActivated);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group) {
      return this.setting(name, description, defaultValue, group, null, null);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible) {
      return this.setting(name, description, defaultValue, group, visible, null);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, Consumer<T> onChanged) {
      return this.setting(name, description, defaultValue, group, null, onChanged);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, Consumer<T> onChanged) {
      return this.setting(name, description, defaultValue, group, visible, onChanged, null);
   }

   public <T> Setting<T> setting(
      String name, String description, T defaultValue, SettingGroup group, IVisible visible, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated
   ) {
      if (defaultValue instanceof Enum e8um) {
         return e8um instanceof MyPotion
            ? group.add(new PotionSetting(name, description, (MyPotion)defaultValue, onChanged, onModuleActivated, visible))
            : group.add(new EnumSetting(name, description, (Enum)defaultValue, onChanged, onModuleActivated, visible));
      } else if (defaultValue instanceof Boolean) {
         return group.add(
            ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)new Builder().name(name)).description(description))
                           .defaultValue((Boolean)defaultValue))
                        .visible(visible))
                     .onChanged(onChanged))
                  .onModuleActivated(onModuleActivated))
               .build()
         );
      } else if (defaultValue instanceof SettingColor) {
         return group.add(new ColorSetting(name, description, (SettingColor)defaultValue, onChanged, onModuleActivated, visible));
      } else if (defaultValue instanceof Keybind) {
         return group.add(new KeybindSetting(name, description, (Keybind)defaultValue, onChanged, onModuleActivated, visible, null));
      } else if (defaultValue instanceof String) {
         return group.add(
            new StringSetting(name, description, (String)defaultValue, onChanged, onModuleActivated, visible, StarscriptTextBoxRenderer.class, null, false)
         );
      } else if (defaultValue instanceof BlockPos) {
         return group.add(new BlockPosSetting(name, description, (BlockPos)defaultValue, onChanged, onModuleActivated, visible));
      } else if (defaultValue instanceof Object2IntMap) {
         return group.add(new StatusEffectAmplifierMapSetting(name, description, (Object2IntMap)defaultValue, onChanged, onModuleActivated, visible));
      } else if (defaultValue instanceof Integer) {
         return group.add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder(
                                    
                                 )
                                 .name(name))
                              .description(description))
                           .defaultValue((Integer)defaultValue))
                        .visible(visible))
                     .onChanged(onChanged))
                  .onModuleActivated(onModuleActivated))
               .build()
         );
      } else {
         return defaultValue instanceof Double
            ? group.add(
               ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                              
                           )
                           .name(name))
                        .description(description))
                     .defaultValue((Double)defaultValue))
                  .build()
            )
            : null;
      }
   }

   @SafeVarargs
   public final <T> Setting<List<T>> setting(String name, String description, SettingGroup group, IVisible visible, T... defaultValue) {
      return this.setting(name, description, group, visible, null, defaultValue);
   }

   @SafeVarargs
   public final <T> Setting<List<T>> setting(
      String name, String description, SettingGroup group, IVisible visible, Consumer<List<T>> onChanged, T... defaultValue
   ) {
      return this.setting(name, description, group, visible, onChanged, null, defaultValue);
   }

   public final <T> Setting<List<T>> setting(
      String name,
      String description,
      SettingGroup group,
      IVisible visible,
      Consumer<List<T>> onChanged,
      Consumer<Setting<List<T>>> onModuleActivated,
      T... defaultValue
   ) {
      if (defaultValue[0] instanceof String) {
         return group.add(
            new StringListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible, StarscriptTextBoxRenderer.class, null)
         );
      } else if (defaultValue[0] instanceof Enchantment) {
         return group.add(new EnchantmentListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible));
      } else if (defaultValue[0] instanceof Module) {
         return group.add(new ModuleListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible));
      } else if (defaultValue[0] instanceof Block) {
         return group.add(new BlockListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, Block -> true, visible));
      } else {
         return defaultValue[0] instanceof SoundEvent
            ? group.add(new SoundEventListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible))
            : null;
      }
   }

   public Setting<Integer> setting(String name, String description, int defaultValue, SettingGroup group, int sliderMax) {
      return group.add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder(
                        
                     )
                     .name(name))
                  .description(description))
               .defaultValue(defaultValue))
            .sliderMax(sliderMax)
            .build()
      );
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, double sliderMin, double sliderMax, int min, int max) {
      return this.setting(name, description, defaultValue, group, null, null, null, sliderMin, sliderMax, min, max, 3);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMax) {
      return this.setting(name, description, defaultValue, group, visible, null, null, 0.0, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, Consumer<T> onChanged, double sliderMax) {
      return this.setting(name, description, defaultValue, group, null, onChanged, null, 0.0, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, double sliderMin, double sliderMax) {
      return this.setting(name, description, defaultValue, this.sgGeneral, null, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
   }

   public <T> Setting<T> setting(
      String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax, int min, int max
   ) {
      return this.setting(name, description, defaultValue, group, visible, null, null, sliderMin, sliderMax, min, max, 3);
   }

   public Setting<Double> setting(
      String name, String description, double defaultValue, SettingGroup group, double sliderMin, double sliderMax, int min, int max, int decimalPlaces
   ) {
      return group.add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                     
                  )
                  .name(name))
               .description(description))
            .defaultValue(defaultValue)
            .sliderRange(sliderMin, sliderMax)
            .range((double)min, (double)max)
            .decimalPlaces(decimalPlaces)
            .build()
      );
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, double sliderMin, double sliderMax) {
      return this.setting(name, description, defaultValue, group, null, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
   }

   public <T> Setting<T> setting(String name, String description, T defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax) {
      return this.setting(name, description, defaultValue, group, visible, null, null, sliderMin, sliderMax, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
   }

   public Setting<Double> setting(
      String name, String description, double defaultValue, SettingGroup group, IVisible visible, double sliderMin, double sliderMax, int decimalPlaces
   ) {
      return group.add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                        
                     )
                     .name(name))
                  .description(description))
               .defaultValue(defaultValue)
               .visible(visible))
            .sliderRange(sliderMin, sliderMax)
            .decimalPlaces(decimalPlaces)
            .build()
      );
   }

   public Setting<Double> setting(
      String name, String description, double defaultValue, SettingGroup group, double sliderMin, double sliderMax, int decimalPlaces
   ) {
      return group.add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                     
                  )
                  .name(name))
               .description(description))
            .defaultValue(defaultValue)
            .sliderRange(sliderMin, sliderMax)
            .decimalPlaces(decimalPlaces)
            .build()
      );
   }

   public Setting<Double> setting(String name, String description, double defaultValue, SettingGroup group, int decimalPlaces) {
      return group.add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                     
                  )
                  .name(name))
               .description(description))
            .defaultValue(defaultValue)
            .decimalPlaces(decimalPlaces)
            .build()
      );
   }

   public <T> Setting<T> setting(
      String name,
      String description,
      T defaultValue,
      SettingGroup group,
      IVisible visible,
      Consumer<T> onChanged,
      Consumer<Setting<T>> onModuleActivated,
      double sliderMin,
      double sliderMax,
      int min,
      int max,
      int decimalPlaces
   ) {
      return defaultValue instanceof Integer
         ? group.add(
            ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder(
                                    
                                 )
                                 .name(name))
                              .description(description))
                           .defaultValue((Integer)defaultValue))
                        .visible(visible))
                     .onChanged(onChanged))
                  .onModuleActivated(onModuleActivated))
               .sliderRange((int)sliderMin, (int)sliderMax)
               .range(min, max)
               .build()
         )
         : group.add(
            ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                                    
                                 )
                                 .name(name))
                              .description(description))
                           .defaultValue((Double)defaultValue))
                        .visible(visible))
                     .onChanged(onChanged))
                  .onModuleActivated(onModuleActivated))
               .sliderRange(sliderMin, sliderMax)
               .range((double)min, (double)max)
               .decimalPlaces(decimalPlaces)
               .build()
         );
   }

   public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha) {
      return this.setting(name, description, red, green, blue, alpha, false, this.sgGeneral, null);
   }

   public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, SettingGroup group) {
      return this.setting(name, description, red, green, blue, 255, false, group, null);
   }

   public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, SettingGroup group) {
      return this.setting(name, description, red, green, blue, alpha, false, group, null);
   }

   public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, SettingGroup group, IVisible visible) {
      return this.setting(name, description, red, green, blue, 255, false, group, visible, null, null);
   }

   public Setting<SettingColor> setting(String name, String description, int red, int green, int blue, int alpha, SettingGroup group, IVisible visible) {
      return this.setting(name, description, red, green, blue, alpha, false, group, visible, null, null);
   }

   public Setting<SettingColor> setting(
      String name, String description, int red, int green, int blue, int alpha, boolean rainbow, SettingGroup group, IVisible visible
   ) {
      return this.setting(name, description, red, green, blue, alpha, rainbow, group, visible, null, null);
   }

   public Setting<SettingColor> setting(
      String name, String description, int red, int green, int blue, SettingGroup group, IVisible visible, Consumer<SettingColor> onChanged
   ) {
      return this.setting(name, description, red, green, blue, 255, false, group, visible, onChanged, null);
   }

   public Setting<SettingColor> setting(
      String name, String description, int red, int green, int blue, int alpha, SettingGroup group, IVisible visible, Consumer<SettingColor> onChanged
   ) {
      return this.setting(name, description, red, green, blue, alpha, false, group, visible, onChanged, null);
   }

   public Setting<SettingColor> setting(
      String name,
      String description,
      int red,
      int green,
      int blue,
      int alpha,
      boolean rainbow,
      SettingGroup group,
      IVisible visible,
      Consumer<SettingColor> onChanged,
      Consumer<Setting<SettingColor>> onModuleActivated
   ) {
      return group.add(new ColorSetting(name, description, new SettingColor(red, green, blue, alpha, rainbow), onChanged, onModuleActivated, visible));
   }

   public Setting<Object2BooleanMap<EntityType<?>>> setting(
      String name,
      String description,
      SettingGroup group,
      boolean onlyAttackable,
      IVisible visible,
      Consumer<Object2BooleanMap<EntityType<?>>> onChanged,
      Consumer<Setting<Object2BooleanMap<EntityType<?>>>> onModuleActivated,
      EntityType<?>... defaultValue
   ) {
      return group.add(new EntityTypeListSetting(name, description, Utils.asO2BMap(defaultValue), onChanged, onModuleActivated, visible, onlyAttackable));
   }

   public Setting<List<Item>> setting(
      String name,
      String description,
      SettingGroup group,
      boolean bypassFilterWhenSavingAndLoading,
      Predicate<Item> filter,
      IVisible visible,
      Consumer<List<Item>> onChanged,
      Consumer<Setting<List<Item>>> onModuleActivated,
      Item... defaultValue
   ) {
      return group.add(
         new ItemListSetting(name, description, Arrays.asList(defaultValue), onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading)
      );
   }

   public SettingGroup groupFromString(String group) {
      SettingGroup settingGroup = this.settings.getGroup(group);
      return settingGroup == null ? this.settings.createGroup(group) : settingGroup;
   }

   public SettingGroup group(String name) {
      return this.settings.createGroup(name);
   }

   public void toggleWithInfo(String message, Object... args) {
      this.info(message, args);
      this.toggle();
   }

   public void toggleWithInfo(int id, String message, Object... args) {
      this.info(id, message, args);
      this.toggle();
   }

   public void toggleWithInfo(int id, Text message) {
      this.info(id, message);
      this.toggle();
   }

   public void toggleWithwarning(String message, Object... args) {
      this.warning(message, args);
      this.toggle();
   }

   public void toggleWithwarning(int id, String message, Object... args) {
      this.warning(id, message, args);
      this.toggle();
   }

   public void toggleWithError(Text message) {
      ChatUtils.sendMsg(0, this.title(), Formatting.RED, message);
      this.toggle();
   }

   public void toggleWithError(int id, String message, Object... args) {
      this.error(id, message, args);
      this.toggle();
   }

   public void toggleWithError(int id, MutableText message) {
      this.error(id, message);
      this.toggle();
   }

   public void info(int id, String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.GRAY, message, args);
   }

   public void info(int id, Text message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, message);
   }

   public void warning(MutableText message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(0, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.YELLOW));
   }

   public void warning(int id, String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.YELLOW, message, args);
   }

   public void error(int id, String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, Formatting.RED, message, args);
   }

   public void error(MutableText message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(0, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.RED));
   }

   public void error(int id, MutableText message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(id, this.title(), Formatting.LIGHT_PURPLE, message.formatted(Formatting.RED));
   }

   public void info(MutableText message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(this.title(), message.formatted(Formatting.GRAY));
   }

   public void info(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.info(this.title(), message, args);
   }

   public void warning(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.warning(this.title(), message, args);
   }

   public void error(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.error(this.title(), message, args);
   }

   public ModuleHelper(Category category, String name, String description) {
      super(category, name, description);
   }
}
