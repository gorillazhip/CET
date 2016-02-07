package transaction;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import event.*;
import iogenerator.*;

public abstract class Transaction implements Runnable {
	
	ArrayList<Event> batch;		
	OutputFileGenerator output;
	public CountDownLatch transaction_number;	
	AtomicLong processingTime;
	AtomicInteger maxMemoryPerWindow;
	
	public Transaction (ArrayList<Event> b, OutputFileGenerator o, CountDownLatch tn, AtomicLong pT, AtomicInteger mMPW) {		
		batch = b;			
		output = o; 
		transaction_number = tn;
		processingTime = pT;
		maxMemoryPerWindow = mMPW;
	}	
}
