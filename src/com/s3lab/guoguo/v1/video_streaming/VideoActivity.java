package com.s3lab.guoguo.v1.video_streaming;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.s3lab.guoguo.v1.DataService;
import com.s3lab.guoguo.v1.R;
import com.s3lab.guoguo.v1.jedis.JedisService;

//import com.google.android.gms.ads.*;

public class VideoActivity extends Activity implements
		CameraView.CameraReadyCallback {
	public static String TAG = "TEAONLY";
	private final int PictureWidth = 480;
	private final int PictureHeight = 360;
	private final int MediaBlockNumber = 3;
	private final int MediaBlockSize = 1024 * 512;
	// private final int EstimatedFrameNumber = 30;
	private final int StreamingInterval = 100;

	private CameraView cameraView = null;

	ExecutorService executor = Executors.newFixedThreadPool(3);
	VideoEncodingTask videoTask = new VideoEncodingTask();
	private ReentrantLock previewLock = new ReentrantLock();
	boolean inProcessing = false;

	byte[] yuvFrame = new byte[1920 * 1280 * 2];

	MediaBlock[] mediaBlocks = new MediaBlock[MediaBlockNumber];
	int mediaWriteIndex = 0;
	int mediaReadIndex = 0;

	Handler streamingHandler;
	private JedisService jedisService;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// application setting
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		jedisService = new JedisService(this);
		jedisService.start();
		// load and setup GUI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_view);

		// init audio and camera
		for (int i = 0; i < MediaBlockNumber; i++) {
			mediaBlocks[i] = new MediaBlock(MediaBlockSize);
		}
		resetMediaBuffer();

		initCamera();

		streamingHandler = new Handler();
		streamingHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				doStreaming();
			}
		}, StreamingInterval);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		jedisService.stop();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (cameraView != null) {
			previewLock.lock();
			cameraView.StopPreview();
			cameraView.Release();
			previewLock.unlock();
			cameraView = null;
		}

		finish();
		// System.exit(0);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	//
	// Interface implementation
	//
	public void onCameraReady() {
		cameraView.StopPreview();
		cameraView.setupCamera(PictureWidth, PictureHeight, 4, 25.0, previewCb);

		DataService.nativeInitMediaEncoder(cameraView.Width(),
				cameraView.Height());

		cameraView.StartPreview();
	}

	//
	// Internal help functions
	//

	private void initCamera() {
		SurfaceView cameraSurface = (SurfaceView) findViewById(R.id.surface_camera);
		cameraView = new CameraView(cameraSurface);
		cameraView.setCameraReadyCallback(this);

		// overlayView_.setOnTouchListener(this);
		// overlayView_.setUpdateDoneCallback(this);
	}

	private void resetMediaBuffer() {
		synchronized (VideoActivity.this) {
			for (int i = 1; i < MediaBlockNumber; i++) {
				mediaBlocks[i].reset();
			}
			mediaWriteIndex = 0;
			mediaReadIndex = 0;
		}
	}

	private void doStreaming() {
		synchronized (VideoActivity.this) {

			MediaBlock targetBlock = mediaBlocks[mediaReadIndex];
			if (targetBlock.flag == 1) {
				sendMedia(targetBlock.data(), targetBlock.length());
				targetBlock.reset();

				mediaReadIndex++;
				if (mediaReadIndex >= MediaBlockNumber) {
					mediaReadIndex = 0;
				}
			}
		}

		streamingHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				doStreaming();
			}
		}, StreamingInterval);

	}

	private PreviewCallback previewCb = new PreviewCallback() {
		public void onPreviewFrame(byte[] frame, Camera c) {
			previewLock.lock();
			doVideoEncode(frame);
			c.addCallbackBuffer(frame);
			previewLock.unlock();
		}
	};

	private void doVideoEncode(byte[] frame) {
		if (inProcessing == true) {
			return;
		}
		inProcessing = true;

		int picWidth = cameraView.Width();
		int picHeight = cameraView.Height();
		int size = picWidth * picHeight + picWidth * picHeight / 2;
		System.arraycopy(frame, 0, yuvFrame, 0, size);

		executor.execute(videoTask);
	};

	private class VideoEncodingTask implements Runnable {
		private byte[] resultNal = new byte[1024 * 1024];
		private byte[] videoHeader = new byte[8];

		public VideoEncodingTask() {
			videoHeader[0] = (byte) 0x19;
			videoHeader[1] = (byte) 0x79;
		}

		public void run() {
			MediaBlock currentBlock = mediaBlocks[mediaWriteIndex];
			if (currentBlock.flag == 1) {
				inProcessing = false;
				return;
			}

			int intraFlag = 0;
			if (currentBlock.videoCount == 0) {
				intraFlag = 1;
			}
			int millis = (int) (System.currentTimeMillis() % 65535);
			int ret = DataService.nativeDoVideoEncode(yuvFrame, resultNal,
					intraFlag);
			if (ret <= 0) {
				return;
			}
			// sendMedia(resultNal, ret);
			// timestamp
			videoHeader[2] = (byte) (millis & 0xFF);
			videoHeader[3] = (byte) ((millis >> 8) & 0xFF);
			// length
			videoHeader[4] = (byte) (ret & 0xFF);
			videoHeader[5] = (byte) ((ret >> 8) & 0xFF);
			videoHeader[6] = (byte) ((ret >> 16) & 0xFF);
			videoHeader[7] = (byte) ((ret >> 24) & 0xFF);
			Log.i("vdfvd", "length: " + ret);
			synchronized (VideoActivity.this) {
				if (currentBlock.flag == 0) {
					boolean changeBlock = false;

					if (currentBlock.length() + ret + 8 <= MediaBlockSize) {
						currentBlock.write(videoHeader, 8);
						currentBlock.writeVideo(resultNal, ret);
						// changeBlock = true;
					} else {
						changeBlock = true;
					}

					// if (changeBlock == false) {
					// if (currentBlock.videoCount >= EstimatedFrameNumber) {
					// changeBlock = true;
					// }
					// }

					if (changeBlock == true) {
						currentBlock.flag = 1;

						mediaWriteIndex++;
						if (mediaWriteIndex >= MediaBlockNumber) {
							mediaWriteIndex = 0;
						}
					}
				}

			}

			inProcessing = false;
		}
	};

	public boolean sendMedia(byte[] data, int length) {
		boolean ret = false;

		byte[] output = new byte[length];
		System.arraycopy(data, 0, output, 0, length);
		// Log.i("-------", output[10] + " " + output[20] + " " + output[30] +
		// " "
		// + output[40]);
		String d = Base64.encodeToString(output, Base64.DEFAULT);
		jedisService.sendData("vv_list", d);

		return ret;
	}

	private class StreamingServer {
		ByteBuffer buf = ByteBuffer.allocate(MediaBlockSize);

		public boolean sendMedia(byte[] data, int length) {
			boolean ret = false;
			System.out.println(data[0] + " " + data[1]);
			String d = Base64.encodeToString(data, Base64.DEFAULT);
			jedisService.sendData("vv_list", d);
			buf.clear();
			buf.put(data, 0, length);
			buf.flip();
			return ret;
		}
	}

	// private native void nativeInitMediaEncoder(int width, int height);
	//
	// private native void nativeReleaseMediaEncoder(int width, int height);
	//
	// private native int nativeDoVideoEncode(byte[] in, byte[] out, int flag);

	// static {
	// System.loadLibrary("MediaEncoder");
	// }

}
