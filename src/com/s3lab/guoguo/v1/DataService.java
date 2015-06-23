package com.s3lab.guoguo.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class DataService extends Service {

	static final int L = 1024 * 128;
	static final int CAPACITY_F = 1024 * 128;
	static final int CAPACITY_B = CAPACITY_F * 4;
	static Context context;
	static float[] preProcessContainer = new float[CAPACITY_F];
	static ByteBuffer bb = ByteBuffer.allocate(CAPACITY_B);
	static byte[] out = new byte[CAPACITY_B];
	static LinkedBlockingDeque<Float> queue = new LinkedBlockingDeque<Float>();
	static MyHandlerThread fromnativeThread = null;
	static MyHandler fromnativeHandler = null;
	static float[] block = new float[3];
	static int readIndex = 0, writeIndex = 0;
	static int currentIndex = 0;
	MyHandlerThread locationUpdateThread;
	MyHandler locationUpdateHandler;

	JedisPoolConfig conf;
	JedisPool pool;

	static Jedis uploaderJedis;
	Jedis locationUpdateJedis;
	Jedis MsgReceiverJedis;

	static final String redisIP = "10.137.35.33";
	static String out_str;

	static String userName;

	// static String channelName_up;
	static String channelName_down;
	static String listName;
	static String notificationChannel = "notification";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		context = this;
		bb.order(ByteOrder.LITTLE_ENDIAN);
		// // boolean inited = init();
		// // Log.v("dataservice", "initialized = " + inited);
		// //
		// // createEngine();
		// // boolean created = createAudioRecorder();
		// Log.v("dataservice", "Audio Recorder is " + created);
		// clear();

		fromnativeThread = new MyHandlerThread("fromnativeThread");
		fromnativeThread.start();

		fromnativeHandler = new MyHandler(fromnativeThread.getLooper());
		Log.v("dataservice", "fromnativeThread ready");

		locationUpdateThread = new MyHandlerThread("locationUpdateThread");
		locationUpdateThread.start();

		locationUpdateHandler = new MyHandler(locationUpdateThread.getLooper());
		Log.v("dataservice", "locationUpdateThread ready");

		conf = new JedisPoolConfig();
		conf.setTestOnBorrow(true);
		conf.setMaxWait(10000);
		pool = new JedisPool(conf, redisIP);
		Log.v("dataservice", "jedisPool ready");

		fromnativeHandler.post(startUploader);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			Bundle data = intent.getExtras();
			userName = data.getString("userName");
			// channelName_up = userName+"_channel_up";
			channelName_down = userName + "_channel_down";
			listName = userName + "_list";
			Log.v("onstart", listName);
			fromnativeHandler.post(transmissionSignal);
			fromnativeHandler.post(recordStart);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		pool.returnResource(uploaderJedis);
		Log.v("dataservice", "jedis returned");
		stopProcess();
		Log.v("dataservice", "stop");
	}

	static boolean hh = true;

	public static void callback(final float[] input) {
		System.out.println(queue.size());
		for (float value : input) {
			queue.add(value);
		}
		if (queue.size() > CAPACITY_F) {
			new UploadData(queue.toArray(new Float[CAPACITY_F])).execute();
			queue.clear();
		}
	}

	static class UploadData extends AsyncTask<Void, Void, Void> {
		private Float[] preProcessContainer;

		public UploadData(Float[] preProcessContainer) {
			this.preProcessContainer = preProcessContainer;
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < CAPACITY_F; i++) {
				float temp = preProcessContainer[i]; // to test
				bb.putFloat(temp);
			}

			out = bb.array();
			bb.clear();
			try {
				out_str = Base64.encodeToString(out, Base64.DEFAULT);
				uploaderJedis.publish(listName, out_str);
				Log.v("", "---- " + preProcessContainer[0] + " " + +out[0]
						+ " " + out[1] + " " + out[2] + " " + out[3]);
			} catch (Exception e) {
				Log.v("dataservice", "something WRONG in jedisUploading");
				e.printStackTrace();
			} finally {
				out = null;
				out_str = null;
			}
			return null;
		}
	}

	private static void playMp3(byte[] mp3SoundByteArray) {
		try {
			// create temp file that will hold byte array
			File tempMp3 = new File("/sdcard/mysdfile.mp3");
			tempMp3.createNewFile();
			tempMp3.deleteOnExit();
			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(mp3SoundByteArray);
			fos.close();

			// Tried reusing instance of media player
			// but that resulted in system crashes...
			MediaPlayer mediaPlayer = new MediaPlayer();

			// Tried passing path directly, but kept getting
			// "Prepare failed.: status=0x1"
			// so using file descriptor instead
			FileInputStream fis = new FileInputStream(tempMp3);
			mediaPlayer.setDataSource(fis.getFD());

			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IOException ex) {
			String s = ex.toString();
			ex.printStackTrace();
		}
	}

	Runnable locationListening = new Runnable() {

		@Override
		public void run() {
			try {
				// locationUpdateJedis = pool.getResource();
				// Log.v("dataservice", "locaionUpdateJedis started");
				// LocationListener myListener = new LocationListener();
				// locationUpdateJedis.subscribe(myListener, channelName_down);

			} catch (Exception e) {
				Log.v("dataservice", "error when creating locationUpdateJedis");
				e.printStackTrace();

			}

		}

	};

	Runnable recordStart = new Runnable() {

		@Override
		public void run() {

			try {
				startProcess();
				// recordjni();
				Log.v("dataservice", "record started");
			} catch (Exception e) {
				Log.v("dataservice", "error when starts record");
				e.printStackTrace();

			}

		}

	};

	Runnable transmissionSignal = new Runnable() {

		@Override
		public void run() {

			try {
				uploaderJedis.publish(notificationChannel, userName + ":start");
				Log.v("dataservice", "start signal sent");
			} catch (Exception e) {
				Log.v("dataservice", "error when creating uploaderJedis");
				e.printStackTrace();

			}

		}

	};

	Runnable startUploader = new Runnable() {

		@Override
		public void run() {
			try {
				uploaderJedis = pool.getResource();
				Log.v("dataservice", "uploaderJedis started");
			} catch (Exception e) {
				Log.v("dataservice", "error when creating uploaderJedis");
				e.printStackTrace();

			}

		}

	};

	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
			Log.v("dataservice", "Handler ready");
		}

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.v("dataservice", "Message Received");

		}
	}

	private class MyHandlerThread extends HandlerThread {

		public MyHandlerThread(String name) {
			super(name);
		}

		protected void onLooperPrepared() {
		}
	}

	public native boolean init();

	public native void createEngine();

	public native boolean createAudioRecorder();

	public native void recordjni();

	public native void clear();

	public native void shutdown();

	public native void stop();

	public static native void startProcess();

	public static native void stopProcess();

	static {
		System.loadLibrary("record-jni");

	}

}
