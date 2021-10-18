package com.listeners;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.utils.TimeService;

@WebListener
public class CtxListener implements ServletContextListener, ServletContextAttributeListener {
	
	public void attributeAdded(ServletContextAttributeEvent e) {
		
	}

	public void attributeRemoved(ServletContextAttributeEvent e) {
		
	}

	public void attributeReplaced(ServletContextAttributeEvent e) {
	
	}
	
	public void contextInitialized(ServletContextEvent e) {
		System.out.println("Tomcat context initialized");
	}
	
	public void contextDestroyed(ServletContextEvent e) {
		TimeService.destroy();
		System.out.println("Tomcat context destroyed");
	}
}
