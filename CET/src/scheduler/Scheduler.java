package scheduler;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import event.*;
import iogenerator.*;

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
	//boolean incremental;
	OutputFileGenerator output;
	
	public Scheduler (int last, EventQueue eq, ExecutorService exe, CountDownLatch d, long start, AtomicInteger dp, int wl, int ws, OutputFileGenerator o) {	
		
		lastsec = last;
		eventqueue = eq;
		executor = exe;
		transaction_number = new CountDownLatch(0);
		done = d;
		startOfSimulation = start;
		drProgress = dp;
		window_length = wl;
		window_slide = ws;
		//incremental = incr;
		output = o;
	}
	
	/**
	 * As long as not all events are processed, extract events from the event queue and execute them.
	 */	
	public void run() {	
		
		/*** Set local variables ***/
		int progress = Math.min(window_slide,lastsec);
		ArrayDeque<Window> windows = new ArrayDeque<Window>();
		Window first_window = new Window(0,window_length);
		windows.add(first_window);
		int new_window_creation = window_slide; 
		boolean last_iteration = false;
		boolean first_expired = false;
							
		/*** Get the permission to schedule current slide ***/
		while (eventqueue.getDriverProgress(progress)) {
			
			/*** Schedule the available events ***/
			Event event = eventqueue.contents.peek();
			while (event != null && event.sec <= progress) { 
					
				Event e = eventqueue.contents.poll();
				
				/*** Create new windows ***/
				if (e.sec >= new_window_creation && new_window_creation < lastsec) {
					int end = (new_window_creation+window_length > lastsec) ? lastsec : (new_window_creation+window_length); 
					Window new_window = new Window(new_window_creation, end);					
					windows.add(new_window);
					// System.out.println(new_window.toString());					
					new_window_creation += window_slide;
					transaction_number = new CountDownLatch((int)transaction_number.getCount()+1);
				}		
				/*** Fill windows with events ***/
				for (Window window : windows) {
					if (window.relevant(e)) {
						window.events.add(e); 
					} else {						
						first_expired = true;
					}
				}
				/*** Poll an expired window and submit it for execution ***/
				if (first_expired) {					
					Window window = windows.poll();
					System.out.println(window.toString());
					
					// Submit for execution
					if (output.isAvailable()) {
						try { output.file.append(window.toString() + "\n"); } catch (IOException e1) { e1.printStackTrace(); }
						output.setAvailable();
						transaction_number.countDown();
					}					
					first_expired = false;
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
		/*** Poll the last windows and submit them for execution ***/
		if (output.isAvailable()) {
			for (Window window : windows) {
				
				System.out.println(window.toString());
			
				// Submit for execution
				try { output.file.append(window.toString() + "\n"); } catch (IOException e) { e.printStackTrace(); }				
				transaction_number.countDown();				
			}
			output.setAvailable();
		}
		/*** Terminate ***/
		try { transaction_number.await(); } catch (InterruptedException e) { e.printStackTrace(); }
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