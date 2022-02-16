package com.listeners;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import com.http.helpers.Logger;
import com.utils.TimeService;

@WebListener
public class CtxListener implements ServletContextListener, ServletContextAttributeListener {
	public static String CONTEXT_HOSTNAME = null;

	
	public void attributeAdded(ServletContextAttributeEvent e) {
		
	}

	public void attributeRemoved(ServletContextAttributeEvent e) {
		
	}

	public void attributeReplaced(ServletContextAttributeEvent e) {
	
	}
	
	public void contextInitialized(ServletContextEvent e) {
		Logger.print("Tomcat context initialized");

		ServletContext ctx = e.getServletContext();
		CONTEXT_HOSTNAME = ctx.getInitParameter("CONTEXT_HOSTNAME");

		Logger.print("CONTEXT_HOSTNAME set to " + CONTEXT_HOSTNAME);
	}
	
	public void contextDestroyed(ServletContextEvent e) {
		TimeService.destroy();
		Logger.print("Tomcat context destroyed");
	}
}
