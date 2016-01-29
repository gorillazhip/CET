package scheduler;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import event.*;
import transaction.*;

public class Scheduler implements Runnable {
	
	int lastsec;
	final EventQueue eventqueue;	
	ExecutorService executor;
	CountDownLatch transaction_number;
	CountDownLatch done;
	long startOfSimulation;
	AtomicInteger drProgress;
	int window_length;
	int window_slide;
	boolean incremental;
	BufferedWriter output;
	
	public Scheduler (int last, EventQueue eq, ExecutorService exe, CountDownLatch d, long start, AtomicInteger dp, int wl, int ws, boolean incr, BufferedWriter o) {	
		
		lastsec = last;
		eventqueue = eq;
		executor = exe;
		done = d;
		startOfSimulation = start;
		drProgress = dp;
		window_length = wl;
		window_slide = ws;
		incremental = incr;
		output = o;
	}
	
	/**
	 * As long as not all events are processed, extract events from the event queue and execute them.
	 */	
	public void run() {	
		
		/*** Set local variables ***/
		int progress = Math.min(window_slide,lastsec);
		ArrayList<Window> windows = new ArrayList<Window>();
		Window first_window = new Window(0,window_length);
		windows.add(first_window);
		int new_window_creation = window_slide; 
		boolean last_iteration = false;
							
		/*** Get the permission to schedule current second ***/
		while (eventqueue.getDriverProgress(progress)) {
			
			/*** Schedule the available events ***/
			Event event = eventqueue.contents.peek();
			while (event != null && event.sec <= progress) { 
					
				Event e = eventqueue.contents.poll();
				if (e.sec >= new_window_creation && new_window_creation < lastsec) {
					int end = (new_window_creation+window_length > lastsec) ? lastsec : (new_window_creation+window_length); 
					Window new_window = new Window(new_window_creation, end);					
					windows.add(new_window);
					//System.out.println(new_window.toString());					
					new_window_creation += window_slide;					
				}				
				for (Window window : windows) {
					if (window.relevant(e)) window.events.add(e); 
				}			
				event = eventqueue.contents.peek();
			}		 
			/*** Update progress ***/
			if (last_iteration) {
				break;
			} else {
				if (progress+window_slide>lastsec) {
					progress = lastsec;
					last_iteration = true;
				} else {
					progress += window_slide;
				}
			}
									
		}
		/*** Show the contents of a window ***/
		/*for (Window window : windows) {
			System.out.println(window.toString());						
		}*/
		/*** Terminate ***/
		done.countDown();
		System.out.println("Scheduler is done.");
	}	
	
	/*public void run() {	
		
		try {		
		*//*** Set local variables ***//*
		int window_end = Math.min(window_length,lastsec);
		int progress = (incremental) ? 0 : window_end;		
		HashSet<TreeSet<Event>> results = new HashSet<TreeSet<Event>>();
		transaction_number = new CountDownLatch(0);
							
		*//*** Get the permission to schedule current second ***//*
		while (progress <= lastsec && eventqueue.getDriverProgress(progress)) {
			
			ArrayList<Event> batch = new ArrayList<Event>();
			if (incremental) progress = Math.min(eventqueue.driverProgress.get(),window_end);
										
			*//*** Schedule the available events ***//*
			Event event = eventqueue.contents.peek();
			while (event != null && event.sec <= progress) { 
					
				Event e = eventqueue.contents.poll();
				//System.out.println(e.toString());
				batch.add(e);
				event = eventqueue.contents.peek();
			}
			*//*** Create a transaction and submit it for execution ***//*
			if (!batch.isEmpty()) {
				transaction_number.await();
				transaction_number = new CountDownLatch(1);
				BaseLine transaction = new BaseLine(batch,startOfSimulation,transaction_number,results,output);				
				executor.execute(transaction);
			}
			*//*** If the stream is over, terminate ***//*
			if (progress == lastsec) {
				transaction_number.await();
				done.countDown();	
				break;
			} else {
				*//*** If window is over, clear the results and update the window end ***//*
				if (progress == window_end) {
					transaction_number.await();
					
					results.clear();
					window_end += window_length;				 
					if (window_end >= lastsec) window_end = lastsec;
				}
				*//*** Update progress ***//*
				progress = (incremental) ? progress+1 : window_end;				
			}			
		}
		} catch (InterruptedException e) { e.printStackTrace(); }
		System.out.println("Scheduler is done.");
	}*/	
}