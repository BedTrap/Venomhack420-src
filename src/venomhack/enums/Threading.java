package venomhack.enums;

public enum Threading {
   THREADED("Threaded"),
   NONE("No threading");

   private final String title;

   private Threading(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
