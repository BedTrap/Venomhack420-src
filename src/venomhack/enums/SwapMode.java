package venomhack.enums;

public enum SwapMode {
   OLD("Old"),
   NEW("New");

   private final String title;

   private SwapMode(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
