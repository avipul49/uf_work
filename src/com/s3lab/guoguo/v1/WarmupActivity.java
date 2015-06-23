package com.s3lab.guoguo.v1;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class WarmupActivity extends Activity {
	
	ImageView img;
	TextView des;
	
//	IntentEngine ie;
	
	ContentThread ct;

	Handler uiHandler;  
	
	String userName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warmup);
        
        img = (ImageView) findViewById(R.id.tsImage);
        des = (TextView) findViewById(R.id.tsText);
        
        uiHandler = new Handler(){
    		
    		public void handleMessage(Message msg){
    			
    			Log.v("uiHandler", "message received");
    			if (msg.what == 1){
    				Log.v("uiHandler", "des update message received");
    				des.setText(msg.obj.toString());
    			}
    		}
    	};
    	
    	ct = new ContentThread("content");
    	ct.start();
    	
    }
    
	private class ContentThread extends Thread{
		
		
		String hashName = "12/23/2012";

		
		public ContentThread(String name) {
			super(name);

		}
		
		
		public void run(){
			
			Log.v("content thread", "content thread starts");
			ArrayList<String> content;
/*************			
//			ie = new IntentEngine();
//			ie.init();
//			
//			content = ie.handleTSIntent(hashName);
			
//			String desStr = content.get(0);
//			Log.v("content thread", "desStr is"+desStr);
			
//			Message updateDesMsg = new Message();
//			updateDesMsg.what = 1;
//			updateDesMsg.obj = desStr;
//			uiHandler.sendMessage(updateDesMsg);
//			Log.v("content thread", "msg sent, its what is "+updateDesMsg.what);
*****************/			
//			String imgStr = content.get(1);

		}
	}

    
}
