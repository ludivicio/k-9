package org.ancode.secmail.mail.crypto.v2;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AsyncHttpTools {

	private static final int N = 5;
	private static final String KEY = "result";
	private static final Executor worker = Executors.newFixedThreadPool(N);
	private static final Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			TaskListener listener = (TaskListener) msg.obj;
			Bundle bundle = msg.getData();
			PostResultV2 result = (PostResultV2) bundle.get(KEY);
			if (listener != null) {
				listener.callBack(result);
			}
		};
	};

	private AsyncHttpTools() {

	}

	public static void execute(final TaskListener listener) {
		try {
			worker.execute(new Runnable() {

				@Override
				public void run() {
					if (listener == null)
						return;
					PostResultV2 pr = (PostResultV2) listener.executeTask();
					Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putSerializable(KEY, pr);
					msg.setData(bundle);
					msg.obj = listener;
					handler.sendMessage(msg);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public interface TaskListener {
		public PostResultV2 executeTask();

		public void callBack(PostResultV2 result);
	}

	public static void destory() {
		if (worker != null && worker instanceof ExecutorService) {
			ExecutorService service = (ExecutorService) worker;
			if (!service.isShutdown()) {
				service.shutdown();
			}
		}
	}
}
