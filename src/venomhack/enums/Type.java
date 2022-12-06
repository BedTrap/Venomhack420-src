package venomhack.enums;

public enum Type {
   PLACE("Place"),
   BREAK("Break"),
   BOTH("Both"),
   NONE("None");

   private final String title;

   private Type(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }

   public boolean placeTrue() {
      return this == PLACE || this == BOTH;
   }

   public boolean breakTrue() {
      return this == BREAK || this == BOTH;
   }
}
