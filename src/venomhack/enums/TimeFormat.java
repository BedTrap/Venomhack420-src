package venomhack.enums;

public enum TimeFormat {
   TWENTY_FOUR_HOUR("24 hour"),
   TWELVE_HOUR("12 hour");

   private final String title;

   private TimeFormat(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
