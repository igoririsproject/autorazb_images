package com.http.helpers;

import nu.pattern.OpenCV;

public class OpenCVHelper {
  public static void initialize() {
    try {
			OpenCV.loadShared();
			Logger.print("OpenCV Loaded");
		} catch (Throwable e) {
			Logger.printError(e.getMessage());
			e.printStackTrace();
		}
  }
}
