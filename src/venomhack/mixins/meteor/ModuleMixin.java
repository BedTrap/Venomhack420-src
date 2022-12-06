package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.systems.modules.movement.ReverseStep;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import venomhack.mixinInterface.IBlink;
import venomhack.mixinInterface.ISpoofName;

@Mixin(
   value = {Module.class},
   remap = false
)
public abstract class ModuleMixin implements IBlink, ISpoofName {
   @Shadow
   @Final
   public String title;
   @Unique
   private Vec3d oldPos;
   @Unique
   private String spoofName = "";

   @Shadow
   public abstract boolean isActive();

   @Overwrite
   public void onDeactivate() {
      if (this instanceof ReverseStep) {
         ((Timer)Modules.get().get(Timer.class)).setOverride(1.0);
      }
   }

   @Overwrite
   public void onActivate() {
      if (this instanceof Blink) {
         this.oldPos = MeteorClient.mc.player.getPos();
      }
   }

   @Unique
   @Override
   public Vec3d getOldPos() {
      return this.oldPos;
   }

   @Redirect(
      method = {"info(Lnet/minecraft/text/Text;)V"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendMsg(Ljava/lang/String;Lnet/minecraft/text/Text;)V"
),
      remap = true
   )
   private void modifyInfo1(String prefix, Text message) {
      ChatUtils.sendMsg(this.spoofName.isEmpty() ? this.title : this.spoofName, message);
   }

   @Redirect(
      method = {"info(Ljava/lang/String;[Ljava/lang/Object;)V"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;info(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V"
)
   )
   private void modifyInfo2(String prefix, String message, Object[] args) {
      ChatUtils.info(this.spoofName.isEmpty() ? this.title : this.spoofName, message, args);
   }

   @Redirect(
      method = {"warning"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;warning(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V"
)
   )
   private void modifyWarning(String prefix, String message, Object[] args) {
      ChatUtils.warning(this.spoofName.isEmpty() ? this.title : this.spoofName, message, args);
   }

   @Redirect(
      method = {"error"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;error(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V"
)
   )
   private void modifyError(String prefix, String message, Object[] args) {
      ChatUtils.error(this.spoofName.isEmpty() ? this.title : this.spoofName, message, args);
   }

   @Redirect(
      method = {"sendToggledMsg"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendMsg(ILnet/minecraft/util/Formatting;Ljava/lang/String;[Ljava/lang/Object;)V"
),
      remap = true
   )
   private void sendToggledMsg(int id, Formatting color, String message, Object[] args) {
      ChatUtils.sendMsg(
         this.hashCode(),
         Formatting.GRAY,
         "Toggled (highlight)%s(default) %s(default).",
         new Object[]{this.spoofName.isEmpty() ? this.title : this.spoofName, this.isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off"}
      );
   }

   @Redirect(
      method = {"toTag"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/nbt/NbtCompound;putString(Ljava/lang/String;Ljava/lang/String;)V"
),
      remap = true
   )
   private void storeSpoofName(NbtCompound tag, String key, String value) {
      tag.putString(key, value);
      tag.putString("spoofName", this.spoofName);
   }

   @Inject(
      method = {"fromTag(Lnet/minecraft/nbt/NbtCompound;)Lmeteordevelopment/meteorclient/systems/modules/Module;"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/nbt/NbtCompound;get(Ljava/lang/String;)Lnet/minecraft/nbt/NbtElement;"
)},
      remap = true
   )
   private void loadSpoofName(NbtCompound tag, CallbackInfoReturnable<Module> cir) {
      if (tag.contains("spoofName")) {
         this.spoofName = tag.getString("spoofName");
      }
   }

   @Override
   public String getSpoofName() {
      return this.spoofName;
   }

   @Override
   public void setSpoofName(String spoofName) {
      this.spoofName = spoofName;
   }
}
