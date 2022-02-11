package com.http.servlets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.http.helpers.ParameterStringBuilder;
import com.utils.TimeService;

/**
 * Servlet implementation class UploadServlet
 */
@MultipartConfig
@WebServlet(urlPatterns = { "/schedule/*" }, asyncSupported = true, loadOnStartup = 1)
public class ScheduleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String token = "";
	private static final String myToken = "@dn0h@3,0hr@";
	public static final String PATH_TO_YII = "/var/www/www-root/data/www/autorazborkaby.by/yii";
	public static String CONTEXT_HOSTNAME;
	//public static final String PATH_TO_YII = "C:/Apache/yii";
	
	private static RunnableScheduledFuture<?> cursTask = null;
	private static RunnableScheduledFuture<?> requestTask = null;
	
	private static final boolean RUN_TASKS_WHEN_STARTED = true;
	
	private static final int CURS_DEFAULT_RATE = 288; // Twelve hours
	private static final int REQUEST_DEFAULT_RATE = 24;
	private static final String COMMAND_DEFAULT_TOKEN = "8sthirhoyg1i933n";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ScheduleServlet() {
        super();
        System.out.println("Schedule servlet started");
        printResponse(sendApiRequest("index"));

				CONTEXT_HOSTNAME = this.getServletContext().getInitParameter("CONTEXT_HOSTNAME");
        token = COMMAND_DEFAULT_TOKEN;
        
      if (RUN_TASKS_WHEN_STARTED) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					printResponse(sendApiRequest("products"));
				}, CURS_DEFAULT_RATE, CURS_DEFAULT_RATE, TimeUnit.MINUTES);
				
				System.out.println("New Currency update schedule set");
			}
		
			if (RUN_TASKS_WHEN_STARTED) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					printResponse(sendApiRequest("deactivate"));
				}, REQUEST_DEFAULT_RATE, REQUEST_DEFAULT_RATE, TimeUnit.HOURS);
				
				System.out.println("New Request update schedule set");
			}
    }
    
    public void destroy() {
    	
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("setScheduleRates") == null) {
			System.err.println("Error: no setScheduleRates parameter");
			response.getWriter().write("error");
			return;
		}
		
		String auth = request.getParameter("token");
		
		if (auth == null || !myToken.equals(auth)) {
			System.err.println("Error: invalid token");
			response.getWriter().write("error token");
			return;
		}
		
		String json = request.getParameter("config");
		
		if (json == null) {
			System.err.println("Error: no config parameter");
			response.getWriter().write("error config");
			return;
		}
		
		try {
			JSONObject config = new JSONObject(json);
			
			int cureScheduleInt = config.optInt("curs_schedule");
			boolean cursSchedule = cureScheduleInt == 1;
			int requestScheduleInt = config.optInt("request_schedule");
			boolean requestSchedule = requestScheduleInt == 1;
			int cursRate = config.optInt("curs_schedule_rate", 12);
			int requestRate = config.optInt("request_schedule_rate", 12);
			token = config.optString("token", "");
			System.out.println("Proccessing scheduled task update request...");
			
			System.out.println("Printing new configuration:\n"+
				"\tRaw json: " + config.toString() + "\n" +
				"\tCurs updated scheduled: " + (cursSchedule ? "true" : "false") + "\n" +
				"\tCurs updated timeout: " + cursRate + "\n" +	
				"\tRequest updated scheduled: " + (requestSchedule ? "true" : "false") + "\n" +
				"\tRequest updated timeout: " + requestRate + "\n");
			
			if (cursTask != null && !cursTask.isCancelled() && !cursTask.isDone()) {
				if (cursTask.cancel(true)) {
					System.out.println("Current curs task cancelled");
				} else {
					System.out.println("Current curs task NOT cancelled");
				}
			}
			
			if (requestTask != null && !requestTask.isCancelled() && !requestTask.isDone()) {
				if (requestTask.cancel(true)) {
					System.out.println("Current request task cancelled");
				} else {
					System.out.println("Current request task NOT cancelled");
				}
			}
			
			if (cursSchedule) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					printResponse(sendApiRequest("products"));
				}, cursRate, cursRate, TimeUnit.HOURS);
				
				System.out.println("New Currency update schedule set");
			}
			
			if (requestSchedule) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					printResponse(sendApiRequest("deactivate"));
				}, requestRate, requestRate, TimeUnit.HOURS);
				
				System.out.println("New Request update schedule set");
			}
			
			response.getWriter().write("success");
		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().write("error config");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public static String getToken() {
		return token;
	}
	
	public static void runCommand(String cmd) {
		String[] env = {"PATH=/bin:/usr/bin/"};
		
		try {
			System.out.println("Running command: " + cmd);
			Process proc = Runtime.getRuntime().exec(cmd, env);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			System.out.println("Schedule servlet command [" + cmd + "] output:\n");
			
			String s = null;
			
			while ((s = stdInput.readLine()) != null) {
			    System.out.println(s);
			}

			System.out.println("Schedule servlet command [" + cmd + "] errors:\n");
			
			while ((s = stdError.readLine()) != null) {
			    System.out.println(s);
			}
			
			stdInput.close();
			stdError.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static HashMap<String, String> sendApiRequest(String action) {
		String url = "https://autorazborkaby.by/api/" + action + "/" + token + "/";
		return sendRequest(url);
	}

	public static HashMap<String, String> sendApiRequest(String action, HashMap<String, String> data) {
		String hostname = "https://autorazborkaby.by";

		if (ScheduleServlet.CONTEXT_HOSTNAME != null) {
			hostname = ScheduleServlet.CONTEXT_HOSTNAME;
		}

		String url = hostname + "/api/" + action + "/" + token + "/";
		return sendRequest(url, data);
	}

	private static HashMap<String, String> sendRequest(String url) {
		return sendRequest(url, null);
	}

	private static HashMap<String, String> sendRequest(String url, HashMap<String, String> postData) {
		return sendRequest(url, null, 0);
	}

	private static HashMap<String, String> sendRequest(String url, HashMap<String, String> postData, int currentTry) {
		HashMap<String, String> result = new HashMap<String, String>();
		result.put("status", "0");
		result.put("response", "");
		HttpURLConnection huc = null;
		BufferedReader in = null;

		try {
			URL u = new URL(url);
			huc = (HttpURLConnection) u.openConnection();

			if (postData != null) {
				huc.setRequestMethod("POST");
				DataOutputStream out = null;

				try {
					huc.setDoOutput(true);
					out = new DataOutputStream(huc.getOutputStream());
					out.writeBytes(ParameterStringBuilder.getParamsString(postData));
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					if (out != null) {
						out.flush();
						out.close();
					}
				}
			}

			huc.setConnectTimeout(5000);
			int responseCode = huc.getResponseCode();
			result.put("status", String.valueOf(responseCode));
			System.out.println("Request to url " + url + " status code is " + responseCode);
			
			if (responseCode == 200) {
				in = new BufferedReader(new InputStreamReader(huc.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}

				result.put("response", content.toString());
			}
		} catch (Exception hucEx2) {
			result.put("status", "500");
			result.put("response", hucEx2.getMessage());
			hucEx2.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {}
			}

			if (huc != null) {
				try {
					huc.disconnect();
				} catch (Exception ex) {}
			}
		}

		if (!result.get("status").equals("200") && currentTry < 1) {
			return sendRequest(url, postData, 1);
		}

		return result;
	}

	private static void printResponse(HashMap<String, String> response) {
		if (response.get("status").equals("200")) {
			System.out.println("Request processed successfully");
		} else {
			System.err.println("Error processing request [" + response.get("status") + "]: " + response.get("message"));
		}
	}
}
