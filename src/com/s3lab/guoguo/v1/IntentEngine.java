package com.s3lab.guoguo.v1;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.util.Log;

public class IntentEngine {
	JedisPool pool;
	JedisPoolConfig conf;

	Jedis toolJedis;
	
	String redisIP;
	
	static final int SUCCESS = 1;
	static final int NOUSER = 2;
	static final int WRONGPWD = 3;
	static final int UNKNOWN = 4;
	static final int DUPLICATED = 5;
	static final int INTERESTED = 1;
	static final int WALKING = 2;
	
	int counter;
	String previousP;
	String currentP;

	public IntentEngine(){
		
	}
	
	public void init(){

		counter = 0;
		previousP = "";
		currentP = "";
		redisIP = "10.136.67.150";
		
		conf = new JedisPoolConfig();
        conf.setTestOnBorrow(true);
        conf.setMaxActive(10000);
        conf.setMaxIdle(5000);
        conf.setMaxWait(10000);
        pool = new JedisPool(conf,redisIP);
        
        toolJedis = pool.getResource();
        
        Log.v("IntentEngine", "init done");
		
	}
	
	public int update(String in){
		if(currentP.contentEquals(in)){
			counter ++;
		}else{
			counter = 0;
		}
		previousP = currentP;
		currentP = in;
		
		return counter;
	}
	
	public int identify(){
		int c = counter;
		if(c>=50){
			return INTERESTED;
		}else{
			return WALKING;
		}
	}

}
