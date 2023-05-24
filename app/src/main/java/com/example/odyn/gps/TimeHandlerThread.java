/*
    BSD 3-Clause License
    Copyright (c) Viacheslav Kushinir <kushnir@mat.umk.pl>, 2023

    See https://aleks-2.mat.umk.pl/pz2022/zesp10/#/project-info for see license text.
*/

package com.example.odyn.gps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Thread for updating current time in InfoDisplayThread and SRTWriter
 */
public class TimeHandlerThread extends Thread {
	Timer timer = new Timer();

	/**
	 * Method that updates current time every second
	 */

	@Override
	public void run() {
		TimerTask task = new TimerTask() {
			public void run() {
				Date currentTime = new Date();
				DataHolder dataHolder = DataHolder.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
				String formattedTime = dateFormat.format(currentTime);
				//Log.d("TimeHandlerThread", formattedTime);
				dataHolder.setTimer(formattedTime);
			}
		};
		timer.schedule(task, 0, 1000);
	}

	/**
	 * Method to stop the thread
	 */
	public void stopTimer() {
		timer.cancel();
		interrupt();
	}
}