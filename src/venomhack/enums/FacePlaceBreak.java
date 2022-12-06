package venomhack.enums;

public enum FacePlaceBreak {
   DEFAULT("Default Speed"),
   CUSTOM("Custom Speed");

   private final String title;

   private FacePlaceBreak(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
