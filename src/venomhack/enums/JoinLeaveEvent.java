package venomhack.enums;

public enum JoinLeaveEvent {
   JOIN("Join"),
   LEAVE("Leave"),
   BOTH("Both"),
   NONE("None");

   private final String title;

   private JoinLeaveEvent(String title) {
      this.title = title;
   }

   public boolean join() {
      return this == JOIN || this == BOTH;
   }

   public boolean leave() {
      return this == LEAVE || this == BOTH;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
