package venomhack.enums;

public enum GapMode {
   GAP("Golden Apple"),
   EGAP("Enchanted Golden Apple");

   public final String title;

   private GapMode(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
