package venomhack.enums;

public enum SpeedUnit {
   METERS_PER_SECOND("Meters per second", "m/s", 1.0),
   KILOMETERS_PER_HOUR("Kilometers per hour", "km/h", 3.6000059687997457),
   METERS_PER_TICK("Meters per tick", "m/t", 0.05),
   MILES_PER_HOUR("Miles per hour", "mi/h", 2.23694),
   FEET_PER_SECOND("Feet per second", "ft/s", 3.2808453346457),
   KNOTS("Knots", "kt", 1.9438477170141413),
   MACH("Mach", "Mach", 0.00291545);

   private final String title;
   public final String unit;
   public final double factor;

   private SpeedUnit(String title, String unit, double factor) {
      this.title = title;
      this.unit = unit;
      this.factor = factor;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
