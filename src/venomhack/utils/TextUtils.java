package venomhack.utils;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import venomhack.Venomhack420;

public class TextUtils extends Utils {
   public static String getGrammar(int number) {
      String digit = Integer.toString(number);
      int length = digit.length();
      if (length > 1 && digit.charAt(length - 2) == '1') {
         return "th";
      } else {
         return switch(digit.charAt(length - 1)) {
            case '1' -> "st";
            case '2' -> "nd";
            case '3' -> "rd";
            default -> "th";
         };
      }
   }

   public static double round(float value, int precision) {
      double scale = Math.pow(10.0, (double)precision);
      return (double)Math.round((double)value * scale) / scale;
   }

   public static double round(double value, int precision) {
      double scale = Math.pow(10.0, (double)precision);
      return (double)Math.round(value * scale) / scale;
   }

   public static String addBackSlashes(String string) {
      return string.isEmpty() ? "" : "\\" + string;
   }

   public static String getNewMessage(List<String> messages) {
      String msg = messages.get(Utils.random(0, messages.size()));
      return msg.equals(Venomhack420.STATS.getLastMessage()) && messages.size() > 1 ? getNewMessage(messages) : msg;
   }

   public static void sendNewMessage(List<String> messages) {
      sendNewMessage(getNewMessage(messages));
   }

   public static void sendNewMessage(String message) {
      if (message.startsWith("/")) {
         MeteorClient.mc.player.sendCommand(message.substring(1), null);
      } else {
         MeteorClient.mc.player.sendChatMessage(message, null);
      }
   }
}
