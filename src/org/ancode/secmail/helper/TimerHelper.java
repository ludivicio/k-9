package org.ancode.secmail.helper;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;

public class TimerHelper {
	private static Activity mActivity;
	
	private final static Timer timer = new Timer();
	private static TimerTask task;
	private static int delay;
	private static Tick mTick;
	private static boolean isTicking = false;

	private TimerHelper() {}
	
	private static class SingletonHolder {
		public static final TimerHelper INSTANCE = new TimerHelper();
	}
	
	public static TimerHelper getInstance(Activity activity) {
		mActivity = activity;

		return SingletonHolder.INSTANCE;
	}
	
	public static void start(Activity activity, int delayTime, TimerHelper.Tick tick) {
		if(activity == null) {
			return;
		}
		
		if(delayTime <= 0) {
			return;
		}
		
		if(tick == null) {
			return;
		}
		
		mActivity = activity;
		delay = delayTime;
		mTick = tick;
		
		mTick.onStart();
		
		task = new TimerTask() {
			@Override
			public void run() {
				
				mActivity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						isTicking = true;
						delay--;  
	                    mTick.onTick(delay);
	                    if(delay < 0){  
	                    	isTicking = false;
	                        timer.cancel();  
	                        mTick.onFinish();
	                    }  
					}
					
				});
			}
			
		};
		
		timer.schedule(task, 0, 1000);
	}
	
	public static boolean isTicking() {
		return isTicking;
	}
	
	public static void cancel() {
		timer.cancel();
		if(mTick != null) {
			isTicking = false;
			mTick.onCancel();
		}
	}
	
	public interface Tick {
		void onStart();
		void onTick(int delay);
		void onFinish();
		void onCancel();
	}
	
}
