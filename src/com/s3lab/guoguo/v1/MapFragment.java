package com.s3lab.guoguo.v1;

import java.util.*;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;

public class MapFragment extends Fragment {
	SOIReceiver mReceiver;
	SurfaceView drawView;
	SurfaceHolder drawHolder;
	
//	MyCallback mCallback;
	
	int w_display;
	int h_display;
	
	Paint mPaint = new Paint();
	
//	private MotionThread mt;
//	private MotionHandler mh;
	
	Bitmap mark; 
	Bitmap backgroundPic;
	Bitmap adjustedBg;
	
	private int screenW, screenH;  ///ygx//	private class MyCallback implements SurfaceHolder.Callback{
	//
//	@Override
//	public void surfaceChanged(SurfaceHolder holder, int format, int width,
//			int height) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void surfaceCreated(SurfaceHolder holder) {
//		Log.v("surfaceviewcallback", "created");
//		Canvas canvas = holder.lockCanvas();
//		canvas.drawBitmap(adjustedBg, 0,0, mPaint);
//		holder.unlockCanvasAndPost(canvas);
//	}
//
//	@Override
//	public void surfaceDestroyed(SurfaceHolder holder) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	
//}
	private boolean flag = true; 
	private Paint paint; 
	private float rate = 1;
	private int p_x;
	private int p_y; 
    
    private float oldRate = 1;  
   
    private float oldLineDistance;  
    
    private boolean isFirst = true; 
    
    private int x1, x2, y1, y2;  
	
//	private ImageView iv;
//	
////	private int ivWidth =0;         
////	private int ivHeight = 0; 
//	private int pcount=0;
//	private int countclick=0;
	
	private Handler uiHandler;
	View view;
	
	public MapFragment() {
	}
	
	
//	public static final String ARG_SECTION_NUMBER = "section_number";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_map, container,false);
		

        drawView  = (SurfaceView) view.findViewById(R.id.motionlayer);
        
        
		drawHolder = drawView.getHolder();
		
		mark = BitmapFactory.decodeResource(getResources(), R.drawable.markcutout);			
		backgroundPic = BitmapFactory.decodeResource(getResources(), R.drawable.map);
		adjustedBg = adjustSize(backgroundPic);
		
//		mCallback = new MyCallback();
//		drawHolder.addCallback(mCallback);
		 drawHolder.addCallback(new DisplaySurfaceView());  
		 
		 
		 drawView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				 System.out.println("get in Ontouch Event");
			    	
			        if (event.getAction() == MotionEvent.ACTION_UP) {  
			            isFirst = true;  
			            oldRate = rate;  
			        } else {  
			            if (event.getPointerCount() > 1) {   
			                x1 = (int) event.getX(0);  
			                y1 = (int) event.getY(0);  
			                x2 = (int) event.getX(1);  
			                y2 = (int) event.getY(1);  
			                
			                if (event.getPointerCount() == 2) {  
			                    if (isFirst) {  
			                        //得到第一次触屏时线段的长�?  
			                        oldLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2) + Math.pow(event.getY(1) - event.getY(0), 2));  
			                        isFirst = false;  
			                    } else {  
			                        //得到非第�?��触屏时线段的长度  
			                        float newLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2) + Math.pow(event.getY(1) - event.getY(0), 2));  
			                        //获取本次的缩放比�?  
			                        rate = oldRate * newLineDistance / oldLineDistance;  
			                    }  
			                }  
			            }  
			        }  
			        return true;  
			}
		});

		mReceiver = new SOIReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.guoguotest");
		getActivity().getApplicationContext().registerReceiver(mReceiver, filter);
		Log.v("MapFragment", "broadcastreceiver registered");
		
		uiHandler = new Handler(getActivity().getMainLooper());
		Log.v("uiHandler","ui handler ready");
		////add code below
		System.out.println("Display activity start~");
       // setContentView(R.layout.media_play);  
      //  videoView = (SurfaceView)findViewById(R.id.videoView);  
       // sfh = videoView.getHolder();  
    //    sfh.addCallback(new DisplaySurfaceView());  
//        
//        DisplayMetrics dm = new DisplayMetrics();   
//        getWindowManager().getDefaultDisplay().getMetrics(dm);   
////        screenW = dm.widthPixels;         //set display area 
//       // screenH = (dm.heightPixels)/2;  //original set
//        screenH = dm.heightPixels;  
        
        screenW=800;
        screenH=1205;
        System.out.println("onCreat end");
		
		
		
		///add code end
		
		return view;
	}
	 class DisplaySurfaceView implements SurfaceHolder.Callback{  
	        @Override  
	        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {  
	              System.out.println("DisplaySurfaceView----surfaceChangedChanged");
	        }  
	  
	        @Override  
	        public void surfaceCreated(SurfaceHolder arg0) { 
	        	 System.out.println("DisplaySurfaceView----surfaceCreatedCreated");
//	            new ImageThread().start();
	        	 myDraw();
	        }  
	  
	        @Override  
	        public void surfaceDestroyed(SurfaceHolder arg0) { 
	        	System.out.println("DisplaySurfaceView----surfaceDestroyed");
	            flag = false;  
	        }  
	          
	    }  
	
//	private class MotionHandler extends Handler{
//    	public MotionHandler(Looper looper){
//    		super(looper);
//    		Log.v("Handler", "MotionHandler ready");
//    	}
//    	
//    	public void handleMessage(Message msg){
//    		super.handleMessage(msg);
//    		Log.v("Handler", "Message Received");
//   		
//    	}
//    }	
	
//	private class MotionThread extends HandlerThread{
//		public MotionThread(String name) {
//			super(name);
//		}
//		
//		protected void onLooperPrepared(){
//		}

//		public void run(){
//			Bitmap mark = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);			
//			Bitmap backgroundPic = BitmapFactory.decodeResource(getResources(), R.drawable.mockupbackground_big);
//			for(int i = 0;i<50;i++){
//				Canvas canvas = drawHolder.lockCanvas();
//				Bitmap adjustedPic = adjustSize(backgroundPic);
////
//				canvas.drawBitmap(adjustedPic, 0,0,mPaint);
//				canvas.drawBitmap(mark, i*20,i*20, mPaint);
//				
////				Log.v("surfaceCreated", "background draw");
//				
//				drawHolder.unlockCanvasAndPost(canvas);
//				Log.v("surfaceCreated", "mark draw");
//				
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//			
//		}
		
//	}
	
//	private class MyCallback implements SurfaceHolder.Callback{
//
//		@Override
//		public void surfaceChanged(SurfaceHolder holder, int format, int width,
//				int height) {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public void surfaceCreated(SurfaceHolder holder) {
//			Log.v("surfaceviewcallback", "created");
//			Canvas canvas = holder.lockCanvas();
//			canvas.drawBitmap(adjustedBg, 0,0, mPaint);
//			holder.unlockCanvasAndPost(canvas);
//		}
//
//		@Override
//		public void surfaceDestroyed(SurfaceHolder holder) {
//			// TODO Auto-generated method stub
//			
//		}
//		
//		
//	}
	
	public Bitmap adjustSize(Bitmap in){
		int h_original = in.getHeight();
		int w_original = in.getWidth();
		h_display = getResources().getDisplayMetrics().heightPixels;
		w_display = getResources().getDisplayMetrics().widthPixels;
		
		float f_h_o = (float)h_original;
		float f_w_o = (float)w_original;
		float f_h_d = (float)h_display;
		float f_w_d = (float)w_display;
		
		float f_w_new = f_w_d*(f_h_d/f_h_o);
		
		float ratio = (float)(f_w_d/f_w_o);
		float f_h_new = h_original*ratio;
		Log.v("test", "ratio is"+ratio+", f_h_new is"+f_h_new);
		int h_new = Math.round(f_h_new);
		Log.v("test", "h_original:"+h_original +"; w_original:"+w_original+"; h_display:"+h_display+"; w_display:"+w_display+"; h_new:"+h_new+";");
//		Log.v("test","ratio is"+ (float)h_display/h_original);
//		Log.v("test","new w is"+ (float)((float)(h_display/h_original)*w_original));
//		Bitmap newPic = Bitmap.createScaledBitmap(in, w_display, w_display, true);
		Bitmap newPic = Bitmap.createScaledBitmap(in, w_display, h_new, false);
		return newPic;
	} 

	
	private class SOIReceiver extends BroadcastReceiver{
		
		
		 
		public SOIReceiver(){
			Log.v("MapFragment", "SOIReceiver ready");
			System.out.println("ygx SOIReceiver ready");
			
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle data = intent.getExtras();
			String update = data.getString("update");
			String soi = (update.split(":"))[0];
			String x_str = ((update.split(":"))[1].split(","))[0];
//			Log.v("test", "x is "+x_str);
			String y_str = ((update.split(":"))[1].split(","))[1];
//			Log.v("test", "y is "+y_str);
			float x = Float.valueOf(x_str)/1180;
			float y = Float.valueOf(y_str)/1540;
			
			Log.v("MapFragment", "the lastest update is "+update);
			System.out.println("ygx x= "+x+" y= "+y);
			System.out.println("Current soi=  "+ soi);
	
			//pcount++;
			
		    	LocationUpdate lu = new LocationUpdate(x, y);
//			    mh.post(lu);
			    uiHandler.post(lu);
		
		}
    }
	
	class LocationUpdate implements Runnable{
		
		
		
		public LocationUpdate(float in_x, float in_y){
			Log.v("new update", "x is "+in_x+"; y is "+in_y);
			
			p_x = (int)(in_x*adjustedBg.getWidth());
			p_y = (int)(in_y*adjustedBg.getHeight());
		}

		@Override
		public void run() {
			System.out.println("ygx begin Draw pinpoint");
//			Canvas canvas = drawHolder.lockCanvas();
//			canvas.drawBitmap(adjustedBg, 0,0, mPaint);
//			canvas.drawBitmap(mark, p_y, p_x, mPaint);
//			drawHolder.unlockCanvasAndPost(canvas);
			myDraw();
		}
		
	}	
	
	//add code here
	 public boolean onTouchEvent(MotionEvent event) {  
		 System.out.println("get in Ontouch Event!!!!!!!!!!!!!");
	    	
	        if (event.getAction() == MotionEvent.ACTION_UP) {  
	            isFirst = true;  
	            oldRate = rate;  
	        } else {  
	            if (event.getPointerCount() > 1) {   
	                x1 = (int) event.getX(0);  
	                y1 = (int) event.getY(0);  
	                x2 = (int) event.getX(1);  
	                y2 = (int) event.getY(1);  
	                
	                if (event.getPointerCount() == 2) {  
	                    if (isFirst) {  
	                        //得到第一次触屏时线段的长�?  
	                        oldLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2) + Math.pow(event.getY(1) - event.getY(0), 2));  
	                        isFirst = false;  
	                    } else {  
	                        //得到非第�?��触屏时线段的长度  
	                        float newLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2) + Math.pow(event.getY(1) - event.getY(0), 2));  
	                        //获取本次的缩放比�?  
	                        rate = oldRate * newLineDistance / oldLineDistance;  
	                    }  
	                }  
	            }  
	        }  
	        return true;  
	    }  
	      
	  
	    
	class ImageThread extends Thread{  
	    @Override  
	    public void run() {  
	    	System.out.println("ImageThread start!");
	        while (flag) {  
	           // long start = System.currentTimeMillis();  
	          //  myDraw();  
	            //long end = System.currentTimeMillis();  
//	            try {  
//	                if (end - start < 50) {  
//	                    Thread.sleep(50 - (end - start));  
//	                }  
//	            } catch (InterruptedException e) {  
//	                e.printStackTrace();  
//	            }  
	        }  
	    }  
	}


	public void myDraw() {  
		//System.out.println("myDraw start!~");
		
	    try {  
	        Canvas canvas = drawHolder.lockCanvas();  
	        if (canvas != null) {  
	            canvas.drawColor(Color.BLACK);  
	            canvas.save();  
	            
	            //bmpIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.museummap);  
	            System.out.println("current rate=: "+rate);
	            canvas.scale(rate, rate, screenW / 2, screenH / 2);  
//	            int width = screenW / 2 - backgroundPic.getWidth() / 2;  
//	            int height = screenH / 2 - backgroundPic.getHeight() / 2;  
//	           
//	            canvas.drawBitmap(backgroundPic, width, height, paint);  
//	            canvas.drawBitmap(backgroundPic, 0,0, mPaint);
	            canvas.drawBitmap(adjustedBg, 0,0, mPaint);
				canvas.drawBitmap(mark, p_y, p_x, mPaint);
	            
	            canvas.restore();  
	            
//	          canvas.drawLine(x1, y1, x2, y2, paint);  
	            drawHolder.unlockCanvasAndPost(canvas);  
	        }  
	    } catch (Exception e) {  
	        e.printStackTrace();  
	    } finally {  
	    }  
	}  

	
}
