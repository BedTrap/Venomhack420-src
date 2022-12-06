package venomhack.enums;

public enum SurroundBreak {
   OFF("Off"),
   ALWAYS("Always"),
   KEYBIND("On Keybind"),
   PICKAXE("On Pickaxe");

   private final String title;

   private SurroundBreak(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
