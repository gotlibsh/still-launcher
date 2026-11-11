# Still itself uses no reflection, so the default rules cover it.

# Switch icons are guessed from Google Home device type class names
# (e.g. "RoomAirConditionerDevice", see SwitchLabels), so those names must survive shrinking.
-keepnames class com.google.home.**
