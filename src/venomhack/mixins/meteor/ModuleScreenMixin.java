package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.mixinInterface.ISpoofName;

@Mixin(
   value = {ModuleScreen.class},
   remap = false
)
public abstract class ModuleScreenMixin extends WidgetScreen {
   @Shadow
   @Final
   private Module module;
   @Unique
   private WSection section;

   public ModuleScreenMixin(GuiTheme theme, String title) {
      super(theme, title);
   }

   @Redirect(
      method = {"initWidgets"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/gui/widgets/containers/WSection;add(Lmeteordevelopment/meteorclient/gui/widgets/WWidget;)Lmeteordevelopment/meteorclient/gui/utils/Cell;",
   ordinal = 0
)
   )
   private <T extends WWidget> Cell<T> dawd(WSection instance, T widget) {
      this.section = instance;
      return this.section.add(this.theme.keybind(this.module.keybind));
   }

   @Inject(
      method = {"initWidgets"},
      at = {@At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/gui/screens/ModuleScreen;add(Lmeteordevelopment/meteorclient/gui/widgets/WWidget;)Lmeteordevelopment/meteorclient/gui/utils/Cell;",
   shift = Shift.BEFORE,
   ordinal = 5
)}
   )
   private void addNameSpoof(CallbackInfo ci) {
      WHorizontalList nameSpoof = (WHorizontalList)this.section.add(this.theme.horizontalList()).widget();
      ISpoofName spoofedModule = (ISpoofName)this.module;
      nameSpoof.add(this.theme.label("Name Spoof: "));
      WTextBox cfC = (WTextBox)nameSpoof.add(this.theme.textBox(spoofedModule.getSpoofName())).minWidth(400.0).expandX().widget();
      cfC.action = () -> spoofedModule.setSpoofName(cfC.get());
   }
}
