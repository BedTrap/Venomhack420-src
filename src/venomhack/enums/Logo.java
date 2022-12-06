package venomhack.enums;

import net.minecraft.util.Identifier;

public enum Logo {
   BLACK("Black", 70, 123, new Identifier("venomhack", "venomlogo.png")),
   WHITE("White", 70, 123, new Identifier("venomhack", "venomlogo_white.png")),
   SIMPLE("Simple", 39, 65, new Identifier("venomhack", "icon.png")),
   SHEIT("Sheit", 122, 128, new Identifier("venomhack", "sheeeeeeiiiittttt.png")),
   METEOR("Meteor", 64, 64, new Identifier("meteor-client", "textures/meteor.png")),
   METEOR_RGB("Meteor RGB", 64, 64, new Identifier("venomhack", "meteor.png"));

   private final String title;
   public final int width;
   public final int height;
   public final Identifier identifier;

   private Logo(String title, int width, int height, Identifier identifier) {
      this.title = title;
      this.width = width;
      this.height = height;
      this.identifier = identifier;
   }

   public boolean blank() {
      return this == SIMPLE || this == METEOR_RGB;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
