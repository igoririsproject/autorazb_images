package com.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TimeService {
	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
	        .setNameFormat("Images-%d")
	        .setDaemon(true)
	        .build();
	
	private static ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, threadFactory));
	
	private static ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(4);
	
	static {
		ses.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		ses.setRemoveOnCancelPolicy(true);
	}
	
	public static long computeNextDelay(int targetHour, int targetMin, int targetSec) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.systemDefault();
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNextTarget = zonedNow.withHour(targetHour).withMinute(targetMin).withSecond(targetSec);
        if (zonedNow.compareTo(zonedNextTarget) > 0) zonedNextTarget = zonedNextTarget.plusDays(1);
        Duration duration = Duration.between(zonedNow, zonedNextTarget);
        return duration.getSeconds();
    }
	
	public static void destroy() {
		ses.shutdown();
		
		try {
		    if (!ses.awaitTermination(1, TimeUnit.MINUTES)) {
		    	System.out.println("Attempting to stop TimeService scheduled executor now after 800ms...");
		    	ses.shutdownNow();
		    } else {
		    	System.out.println("TimeService scheduled executor stopped successfully");
		    }
		} catch (InterruptedException e) {
			System.out.println("Attempting to stop TimeService scheduled executor now because of InterruptedException...");
			ses.shutdownNow();
		}
		
		executorService.shutdown();
		
		try {
		    if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
		    	System.out.println("Attempting to stop TimeService executor now after 800ms...");
		    	executorService.shutdownNow();
		    } else {
		    	System.out.println("TimeService executor stopped successfully");
		    }
		} catch (InterruptedException e) {
			System.out.println("Attempting to stop TimeService executor now because of InterruptedException...");
			executorService.shutdownNow();
		}
	}
	
	public static ListeningExecutorService getExecutor() {
		return executorService;
	}

	public static RunnableScheduledFuture<?> scheduleTask(Runnable task, long delay, TimeUnit timeunit) {
		RunnableScheduledFuture<?> rsf = (RunnableScheduledFuture<?>) ses.schedule(task, delay, timeunit);
		Date d = new Date();
		d.setTime(d.getTime()+rsf.getDelay(TimeUnit.MILLISECONDS));
		return rsf;
	}
	
	public static RunnableScheduledFuture<?> scheduleTaskAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeunit) {
		return (RunnableScheduledFuture<?>) ses.scheduleWithFixedDelay(task, initialDelay, period, timeunit);
	}
	
	public static <T> ListenableFuture<T> submit(Callable<T> task) {
		return executorService.submit(task);
	}
	
	public static ListenableFuture<?> submit(Runnable task) {
		return executorService.submit(task);
	}
}
