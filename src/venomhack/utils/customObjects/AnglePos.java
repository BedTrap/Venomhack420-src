package venomhack.utils.customObjects;

import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityPose;

public record AnglePos(Vec3d vec, float yaw, float pitch, float headYaw, EntityPose pose) {
}
