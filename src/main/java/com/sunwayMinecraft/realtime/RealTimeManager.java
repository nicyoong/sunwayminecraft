package com.sunwayMinecraft.realtime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The RealTimeManager class provides utilities for obtaining the current date and time formatted
 * according to a specified {@link ZoneId}. By default, this manager uses the Asia/Singapore time
 * zone as its “local” zone and also exposes UTC.
 *
 * <p>The class maintains two immutable {@link DateTimeFormatter} instances for time (“HH:mm:ss”)
 * and date (“yyyy-MM-dd”) formatting, and two constant {@link ZoneId} values for UTC and
 * Asia/Singapore. It is thread-safe: all fields are effectively immutable and can be shared across
 * threads without additional synchronization.
 *
 * <p>Key functionality includes:
 *
 * <ul>
 *   <li>{@link #getFormattedTime(ZoneId)}: Returns the current time in the given zone, formatted as
 *       HH:mm:ss.
 *   <li>{@link #getFormattedDate(ZoneId)}: Returns the current date in the given zone, formatted as
 *       yyyy-MM-dd.
 *   <li>{@link #getLocalZone()}: Exposes the default local zone (Asia/Singapore).
 *   <li>{@link #getUTCZone()}: Exposes the UTC zone.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * RealTimeManager rtm = new RealTimeManager();
 * String utcNow = rtm.getFormattedTime(rtm.getUTCZone());
 * String localDate = rtm.getFormattedDate(rtm.getLocalZone());
 * }</pre>
 */
public class RealTimeManager {
  private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
  private static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");
  private DateTimeFormatter timeFormatter;
  private DateTimeFormatter dateFormatter;
  private ZoneId localZone;

  public RealTimeManager() {
    localZone = SINGAPORE_ZONE;
    timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  }

  public String getFormattedTime(ZoneId zone) {
    return ZonedDateTime.now(zone).format(timeFormatter);
  }

  public String getFormattedDate(ZoneId zone) {
    return ZonedDateTime.now(zone).format(dateFormatter);
  }

  public ZoneId getLocalZone() {
    return localZone;
  }

  public ZoneId getUTCZone() {
    return UTC_ZONE;
  }
}
