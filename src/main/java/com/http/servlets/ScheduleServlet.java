package com.http.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

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
	//public static final String PATH_TO_YII = "C:/Apache/yii";
	
	private static RunnableScheduledFuture<?> cursTask = null;
	private static RunnableScheduledFuture<?> requestTask = null;
	
	private static final boolean RUN_TASKS_WHEN_STARTED = true;
	
	private static final int CURS_DEFAULT_RATE = 288; // Twelve hours
	private static final int REQUEST_DEFAULT_RATE = 24;
	private static final String COMMAND_DEFAULT_TOKEN = "Rh0yN@,R0v@yl@";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ScheduleServlet() {
        super();
        System.out.println("Schedule servlet started");
        runCommand("php " + PATH_TO_YII + " command/index");
        
        token = COMMAND_DEFAULT_TOKEN;
        
      if (RUN_TASKS_WHEN_STARTED) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					runCommand("php " + PATH_TO_YII + " command/products " + token);
				}, CURS_DEFAULT_RATE, CURS_DEFAULT_RATE, TimeUnit.MINUTES);
				
				System.out.println("New Currency update schedule set");
			}
		
			if (RUN_TASKS_WHEN_STARTED) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					runCommand("php " + PATH_TO_YII + " command/deactivate " + token);
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
			final String t = token;
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
					runCommand("php " + PATH_TO_YII + " command/products " + t);
				}, cursRate, cursRate, TimeUnit.HOURS);
				
				System.out.println("New Currency update schedule set");
			}
			
			if (requestSchedule) {
				cursTask = TimeService.scheduleTaskAtFixedRate(() -> {
					runCommand("php " + PATH_TO_YII + " command/deactivate " + t);
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
}
