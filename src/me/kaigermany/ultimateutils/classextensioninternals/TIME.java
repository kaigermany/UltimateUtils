package me.kaigermany.ultimateutils.classextensioninternals;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

public interface TIME {
	/**
	 * Converts long to Time-String.
	 * Example: 1245372962000 -> 'Thu Jun 18 20:56:02 EDT 2009'
	 * @param ms milliseconds since 1.1.1970
	 * @return Time-String
	 */
	default String time_MSToDateStr(long ms) {
		return new Date(ms).toString();
	}
	/**
	 * Converts Time-String to long.
	 * Example: 'Thu Jun 18 20:56:02 EDT 2009' -> 1245372962000
	 * @param timeStr Time-String
	 * @return milliseconds since 1.1.1970
	 */
	default long time_DateStrToMS(String timeStr) { 
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss zzz yyyy", Locale.ENGLISH);
		TemporalAccessor zdt = formatter.parse(timeStr, new ParsePosition(0));
		LocalTime t = LocalTime.from(zdt);
		Instant instant = t.atDate(LocalDate.from(zdt)).atZone(ZoneId.from(zdt)).toInstant();
		Date time = Date.from(instant);
		return time.getTime();
	}
}
