package com.s3lab.guoguo.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.Deflater;

import com.s3lab.guoguo.v1.Adpcm.AdpcmState;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

	static final int L = 1024 * 8;
	static final int CAPACITY_F = 1024 * 8;
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
	public static final String AUDIO_BYTES_RECIEVED = "AudioBytesRecieved";

	JedisPoolConfig conf;
	JedisPool pool;

	static Jedis uploaderJedis;
	Jedis locationUpdateJedis;
	Jedis MsgReceiverJedis;

	static final String redisIP = "10.136.32.218";
	static String out_str;

	static String userName;

	static String listName;
	static String notificationChannel = "notification";

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		context = this;
		bb.order(ByteOrder.LITTLE_ENDIAN);

		fromnativeThread = new MyHandlerThread("fromnativeThread");
		fromnativeThread.start();

		fromnativeHandler = new MyHandler(fromnativeThread.getLooper());
		Log.v("dataservice", "fromnativeThread ready");

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
			listName = userName + "_list";
			Log.v("onstart", listName);
			fromnativeHandler.post(transmissionSignal);
			fromnativeHandler.post(recordStart);
			// nativeInitMediaEncoder(0, 0);
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

		// System.out.println(queue.size());
		for (float value : input) {
			queue.add(value);
		}
		if (queue.size() >= CAPACITY_F) {
			Float[] floatArray = queue.toArray(new Float[CAPACITY_F]);
			queue.clear();
			Intent intent = new Intent();
			intent.setAction(AUDIO_BYTES_RECIEVED);
			intent.putExtra("data", input);
			context.sendBroadcast(intent);
			new UploadData(floatArray).execute();
		}
	}

	static class UploadData1 extends AsyncTask<Void, Void, Void> {
		private Float[] preProcessContainer;

		public UploadData1(Float[] preProcessContainer) {
			this.preProcessContainer = preProcessContainer;
		}

		@Override
		protected Void doInBackground(Void... params) {
			byte[] eArray = new byte[preProcessContainer.length];
			int i = 0;
			for (float f : preProcessContainer) {
				bb.putShort((short) f);
			}
			byte[] bArray = bb.array();
			int size = nativeDoAudioEncode(bArray, bArray.length, eArray);

			try {
				out_str = Base64.encodeToString(eArray, Base64.DEFAULT);
				uploaderJedis.publish(listName, out_str);
				Log.v("", "---- " + bArray[0] + " " + size + " "
						+ bArray.length);
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

	static class UploadData extends AsyncTask<Void, Void, Void> {
		private Float[] preProcessContainer;

		public UploadData(Float[] preProcessContainer) {
			this.preProcessContainer = preProcessContainer;
		}

		public static byte[] compress(byte[] data) throws IOException {
			Deflater deflater = new Deflater();
			deflater.setInput(data);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
					data.length);

			deflater.finish();
			byte[] buffer = new byte[1024];
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer); // returns the generated
														// code... index
				outputStream.write(buffer, 0, count);
			}
			outputStream.close();
			byte[] output = outputStream.toByteArray();

			deflater.end();

			System.out.println("Original: " + data.length + " Kb");
			System.out.println("Compressed: " + output.length + " Kb");
			return output;
		}

		@Override
		protected Void doInBackground(Void... params) {
			short[] barray = new short[preProcessContainer.length];
			for (int i = 0; i < preProcessContainer.length; i++) {
				float temp = preProcessContainer[i]; // to test
				barray[i] = (short) temp;
			}
			Adpcm adpcm = new Adpcm();
			AdpcmState state = adpcm.new AdpcmState();
			byte[] eArray = new byte[preProcessContainer.length];
			int size = adpcm.code(state, barray, 0, preProcessContainer.length,
					eArray, 0);
			Log.v("", "--------  " + preProcessContainer.length + " " + size
					+ " - " + barray[0] + " " + barray[1] + " " + barray[2]
					+ " " + barray[3]);
			try {
				out_str = Base64.encodeToString(Arrays.copyOf(eArray, size),
						Base64.DEFAULT);
				uploaderJedis.publish(listName, out_str);
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

	Runnable recordStart = new Runnable() {

		@Override
		public void run() {

			try {
				startProcess();
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

	public static native void startProcess();

	public static native void stopProcess();

	private static native void nativeInitMediaEncoder(int width, int height);

	private static native void nativeReleaseMediaEncoder(int width, int height);

	private static native int nativeDoVideoEncode(byte[] in, byte[] out,
			int flag);

	private static native int nativeDoAudioEncode(byte[] in, int length,
			byte[] out);

	static {
		System.loadLibrary("MediaEncoder");

	}

}
