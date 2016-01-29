package iogenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import event.*;
import scheduler.*;
 
public class Main {
	
	/**
	 * Create and call the chain: Input file -> Driver -> Executor -> Output files 
	 * @param args: 
	 */
	public static void main (String[] args) { 
		
		try {
		
		/*** Print current time ***/
		Date dNow = new Date( );
	    SimpleDateFormat ft = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
	    System.out.println("Current Date: " + ft.format(dNow));
	    
	    /*** INPUT ***/
	    // Set default values
	    String path = "src\\iofiles\\";
		String inputfile ="stream.txt";
		String outputfile ="sequences.txt";
		File output_file = new File(path + outputfile);
		BufferedWriter output = new BufferedWriter(new FileWriter(output_file)); 
		
	    int lastsec = 57;
		int window_length = 20;
		int window_slide = 10;	
		boolean incremental = false;
		
		// Read input parameters
	    for (int i=0; i<args.length; i++){
			if (args[i].equals("-path")) 		path = args[++i];
			if (args[i].equals("-filename")) 	inputfile = args[++i];
			if (args[i].equals("-sec")) 		lastsec = Integer.parseInt(args[++i]);
			if (args[i].equals("-wl")) 			window_length = Integer.parseInt(args[++i]);
			if (args[i].equals("-wos")) 		window_slide = Integer.parseInt(args[++i]);
			if (args[i].equals("-inc")) 		incremental = (Integer.parseInt(args[++i])==1);
		}
	    String input = path + inputfile;
	    
	    // Print input parameters
	    System.out.println(	"Input file: " + input +
	    					"\nLast sec: " + lastsec +
	    					"\nWindow length: " + window_length + 
							"\nWindow slide: " + window_slide +
							"\nIncremental: " + incremental);

		/*** SHARED DATA STRUCTURES ***/		
		AtomicInteger driverProgress = new AtomicInteger(-1);	
		EventQueue eventqueue = new EventQueue(driverProgress);						
		CountDownLatch done = new CountDownLatch(1);
		long startOfSimulation = System.currentTimeMillis();	
		
		/*** EXECUTORS ***/
		int number_of_executors = 1;// Integer.parseInt(args[0]);
		//System.out.println("Number of executors: " + number_of_executors);
		ExecutorService executor = Executors.newFixedThreadPool(number_of_executors);
			
		/*** Create and start the event driver and the scheduler THREADS.
		 *   Driver reads from the file and writes into the event queue.
		 *   Scheduler reads from the event queue and submits event batches to the executor. ***/
		EventDriver driver = new EventDriver (input, lastsec, eventqueue, startOfSimulation, driverProgress);				
				
		Scheduler scheduler = new Scheduler (lastsec, eventqueue, executor, done, startOfSimulation, driverProgress, window_length, window_slide, incremental, output);		
		
		Thread prodThread = new Thread(driver);
		prodThread.setPriority(10);
		prodThread.start();
		
		Thread consThread = new Thread(scheduler);
		consThread.setPriority(10);
		consThread.start();		
				
		/*** Wait till all input events are processed and terminate the executor ***/
		done.await();		
		executor.shutdown();	
		output.close();
		System.out.println("Executor is done.\nMain is done.");
			
		} catch (InterruptedException e) { e.printStackTrace(); }
		  catch (IOException e1) { e1.printStackTrace(); }
	}	
}