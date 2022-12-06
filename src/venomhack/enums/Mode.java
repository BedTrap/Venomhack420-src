package venomhack.enums;

public enum Mode {
   PACKET("Packet"),
   BYPASS("Bypass"),
   INSTANT("Instant mine");

   private final String title;

   private Mode(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
