package com.boot.jx.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;

import com.boot.utils.ArgUtil;

public final class CommonUtils {

	
public static String monthNameByTimestamp(long timestamp) {
        String monthYear = null;
        // Convert the Unix timestamp to LocalDateTime
        LocalDateTime dateTime1 = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
      // Get the month name
        String month = dateTime1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Get the year
        int year = dateTime1.getYear();
        monthYear =month+" "+year;
        return monthYear;
	}
	
	
	public static long startTStampForaMonth(long givenTimestamp) {
		
	 // Convert the given timestamp to LocalDateTime
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(givenTimestamp), ZoneId.systemDefault());
    
    // Get the start of the month (1st day of that month at 00:00:00)
    LocalDateTime startOfMonth = dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);//.withNano(0);
    
    // Get the end of the month (Last day of that month at 23:59:59)
    LocalDateTime endOfMonth = dateTime.withDayOfMonth(dateTime.toLocalDate().lengthOfMonth())
                                       .withHour(23).withMinute(59).withSecond(59);
    //.withNano(999999999);
    
    // Convert to timestamps (milliseconds since epoch)
    long startTimestamp = startOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
    long endTimestamp = endOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
    
    
    return startTimestamp; 
	}
	
	
	
	public static long startTStampForaMonthV1(long givenTimestamp) {
        // Convert given timestamp (seconds) to milliseconds
        givenTimestamp = givenTimestamp * 1000;

        // Convert the given timestamp to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(givenTimestamp), ZoneId.systemDefault());

        // Get the start of the month (1st day of that month at 00:00:00)
        LocalDateTime startOfMonth = dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Convert to timestamp (seconds since epoch)
        return startOfMonth.toInstant(ZoneOffset.UTC).getEpochSecond();
    }
	
	
	public static long endTStampForaMonth(long givenTimestamp) {
		// Convert the given timestamp to LocalDateTime
	    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(givenTimestamp), ZoneId.systemDefault());
	    
	    // Get the start of the month (1st day of that month at 00:00:00)
	    LocalDateTime startOfMonth = dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);//.withNano(0);
	    
	    // Get the end of the month (Last day of that month at 23:59:59)
	    LocalDateTime endOfMonth = dateTime.withDayOfMonth(dateTime.toLocalDate().lengthOfMonth())
	                                       .withHour(23).withMinute(59).withSecond(59);
	    //.withNano(999999999);
	    
	    // Convert to timestamps (milliseconds since epoch)
	    long startTimestamp = startOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
	    long endTimestamp = endOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
	   
	    return endTimestamp; 
		}
	
	 public static long endTStampForaMonthV1(long givenTimestamp) {
	        // Convert given timestamp (seconds) to milliseconds
	        givenTimestamp = givenTimestamp * 1000;

	        // Convert the given timestamp to LocalDateTime
	        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(givenTimestamp), ZoneId.systemDefault());

	        // Get the end of the month (Last day of that month at 23:59:59)
	        LocalDateTime endOfMonth = dateTime.withDayOfMonth(dateTime.toLocalDate().lengthOfMonth())
	                                           .withHour(23).withMinute(59).withSecond(59);

	        // Convert to timestamp (seconds since epoch)
	        return endOfMonth.toInstant(ZoneOffset.UTC).getEpochSecond();
	    }
	 
	 
	 public static Date getDate(String value) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				 // Parse the string to a Date object
		        Date date = sdf.parse(value);
		        return date;
			}catch(Exception e) {
				e.printStackTrace();
			}
			return new Date();
		}
	 
	 public static String getTodayDtAsStr() {
		   String formattedDate = null;
			try {
				 LocalDate today = LocalDate.now();
			     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			     formattedDate = today.format(formatter);
			    return formattedDate;
			}catch(Exception e) {
				e.printStackTrace();
			}
			return formattedDate;
		}
	 
	 
	  
	 
	 public static long getDateWithTS(String dateString) {
		 long timestamp =0;
		 
		try {
		 	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	        // Parse the date
	        LocalDate localDate = LocalDate.parse(dateString, formatter);
	        // Convert to Date object and get timestamp
	         timestamp = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime();
	        return timestamp;
		}catch(Exception e) {
			if(ArgUtil.is(dateString)) {
				timestamp =  Long.parseLong(dateString);
			}
		}
		return timestamp;
	 }
	 
	 
	 public static int getYearFromTimestamp(long timestamp) {
	        String monthYear = null;
	        // Convert the Unix timestamp to LocalDateTime
	        LocalDateTime dateTime1 = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
	      // Get the month name
	        String month = dateTime1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

	        // Get the year
	        int year = dateTime1.getYear();
	       return year;
		}
	 
	 
	 public static int getMonthFromTimestamp(long timestamp) {
	        String monthYear = null;
	        // Convert the Unix timestamp to LocalDateTime
	        LocalDateTime dateTime1 = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
	      // Get the month name
	        String monthStr = dateTime1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

	        // Get the year
	        int month = dateTime1.getMonthValue();
	       return month;
		}


	public static void main(String[] args)
	{
	
		String dt ="05/23/1984";
		long ts = 1733682600L;
		System.out.println("Date :"+getYearFromTimestamp(ts)+"\t ts :"+getMonthFromTimestamp(ts));
	}
}
