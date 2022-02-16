package com.http.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
  public static void print(String message) {
    String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
    System.out.println("[" + timeStamp + "]: " + message);
  }

  public static void printError(String message) {
    String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
    System.err.println("[" + timeStamp + "]: " + message);
  }
}
