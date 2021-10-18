package com.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

public class Utils {
	private static final DateFormat df0 = new SimpleDateFormat("yyyy-MM-dd");
	//private static final DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static final DateTimeFormatter df2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private static final List<Character> cyrillic = Arrays.asList(new Character[]{'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ь', 'ы', 'ъ', 'э', 'ю', 'я', ' '});
	private static final List<String> latin = Arrays.asList(new String[]{"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "j", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "c", "ch", "sh", "shj", "", "ji", "j", "e", "yu", "ya", "_"});
	
	static {
		df0.setLenient(true);
	}
	
	public static String encodeURL(String url) throws UnsupportedEncodingException {
		if (url == null) return null;
		url = url.trim();
		if (url.isEmpty()) return url;
		return URLEncoder.encode(url, "UTF-8").replaceAll("%2F", "/");
	}
	
	public static String getChecksum(Object object) throws IOException {
		ByteArrayOutputStream baos = null;
	    ObjectOutputStream oos = null;
	    
	    try {
	        baos = new ByteArrayOutputStream();
	        oos = new ObjectOutputStream(baos);
	        oos.writeObject(object);
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] thedigest = md.digest(baos.toByteArray());
	        return DatatypeConverter.printHexBinary(thedigest);
	    } catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} finally {
	        if (oos != null) oos.close();
	        if (baos != null) baos.close();
	    }
	}
	
	public static Cookie getCookie(String name, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		
		if (cookies != null) {
			int l = cookies.length;
			for (int i = 0; i < l; i++) if (name.equals(cookies[i].getName())) return cookies[i];
		}
		
		return null;
	}
	
	public static String getCookieValue(String name, HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		
		if (cookies != null) {
			int l = cookies.length;
			for (int i = 0; i < l; i++) if (name.equals(cookies[i].getName())) return cookies[i].getValue();
		}
		
		return null;
	}
	
	public static Long getDateFormatted(Date d) {
		if (d != null) return d.getTime();
		return null;
	}
	
	public static String getDateFormatted(LocalDateTime d) {
		if (d != null) return df2.format(d);
		return null;
	}
	
	public static String getDateFormattedByLocalTimezone(Date d) {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		calendar.setTime(d);
		DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");    
		formatter.setTimeZone(TimeZone.getTimeZone("GMT+03"));  
		return formatter.format(calendar.getTime());
	}
	
	public static String getDateFormattedSimply(Date d) {
		if (d != null) return df0.format(d);
		return null;
	}
	
	public static Map<String, List<String>> getQueryParams(String url) {
	    try {
	        Map<String, List<String>> params = new HashMap<String, List<String>>();
	        String[] urlParts = url.split("\\?");
	        
	        if (urlParts.length > 1) {
	            String query = urlParts[1];
	            
	            for (String param : query.split("&")) {
	                String[] pair = param.split("=");
	                String key = URLDecoder.decode(pair[0], "UTF-8");
	                String value = "";
	                if (pair.length > 1) value = URLDecoder.decode(pair[1], "UTF-8");

	                List<String> values = params.get(key);
	                
	                if (values == null) {
	                    values = new ArrayList<String>();
	                    params.put(key, values);
	                }
	                
	                values.add(value);
	            }
	        }

	        return params;
	    } catch (UnsupportedEncodingException ex) {
	        return null;
	    }
	}
	
	public static int genRandomInt(int max) {
		return new Random().nextInt(max + 1);
	}
	
	public static int getRandomNumberInRange(int min, int max) {
		return new Random().ints(min, (max + 1)).findFirst().getAsInt();
	}
	
	public static String genRandomString(int length) {
		char[] chars = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		
		for (int i = 0; i < length; ++i) {
			char c = chars[random.nextInt(chars.length)];
			if (random.nextBoolean()) c = Character.toUpperCase(c);
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public static boolean isCorrectEmail(String s) {
		if (s == null) return false;
		
		return Pattern.compile("^[\\w!#$%&\'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&\'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$")
			.matcher(s)
			.matches();
	}
	
	public static boolean isNumber(String string) {
	    if (string == null || string.trim().isEmpty()) {
	        return false;
	    }
	    
	    int i = 0;
	    
	    if (string.charAt(0) == '-') {
	        if (string.length() > 1) {
	            i++;
	        } else {
	            return false;
	        }
	    }
	    
	    for (; i < string.length(); i++) {
	        if (!Character.isDigit(string.charAt(i))) {
	            return false;
	        }
	    }
	    
	    return true;
	}
	
	public static boolean isParameterPresent(String param) {
		return (param != null && !param.trim().isEmpty());
	}
	
	public static Date parseDate(String d) {
		LocalDate ld = LocalDate.parse(d);
		return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}
	
	public static Date parseSQLDate(String d) throws ParseException {
		if (isNumber(d)) return new Date(Long.parseLong(d));
		return df0.parse(d);
	}
	
	public static String transliterate(String s) {
		int l = s.length();
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < l; i++) {
			char c = s.charAt(i);
			c = Character.toLowerCase(c);
			int j = cyrillic.indexOf(Character.valueOf(c));
			
			if (j > 0) {
				String lat = latin.get(j);
				sb.append(lat);
			} else {
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
}
