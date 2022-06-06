package com.mantra.BiometricAttendance.database;

import android.app.Application;
import android.util.Log;
import java.util.*;

public class Global extends Application {
	
	private Hashtable<Object, Object> data;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.w("Application","Global Object Created");
		data=new Hashtable<Object, Object>();
	}
	
	public void setAttribute(Object key,Object value)
	{
		data.put(key, value);
	}
	
	public Object getAttribute(Object key)
	{
		return data.get(key);
	}
	
	public void removeAttribute(Object key)
	{
		data.remove(key);
	}
	
	public void clear()
	{
		data.clear();
	}

}
