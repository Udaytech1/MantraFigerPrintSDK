package com.mantra.BiometricAttendance.database;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

@SuppressLint({ "SimpleDateFormat", "DefaultLocale" })
public class DBHelper extends SQLiteOpenHelper {

	protected SQLiteDatabase database;
	public Global global;
	
	public static final String Database_Name="db_BiometricAttendance.db"; //provide name of the database with .db extension
	public static final int Database_Version=1; //provide version of the database created 
	public static final String Key_ID="_id"; //name of the primary key column

	//Code to add tables
	public String tbl_registration_master="tbl_registration_master";
	
	public DBHelper(Context context) {
		super(context, Database_Name, null, Database_Version);
		database = getWritableDatabase();
		//global=(Global)context.getApplicationContext();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.w("Create URL", "reading onCreate database");
		LinkedHashMap<String, String> cols=new LinkedHashMap<String, String>();
		cols.put("name", "varchar(100)");
		cols.put("mobile_no", "varchar(10)");
		cols.put("districtcode", "varchar(50)");
		cols.put("blockcode", "varchar(50)");
		cols.put("clustercode", "varchar(50)");
		cols.put("schoolcode", "varchar(50)");
		cols.put("EnrollTemplate", "blob");
		cols.put("imei_id", "varchar(50)");
		cols.put("log_date", "date default (datetime('now'))");
		cols.put("longitude", "varchar(30)");
		cols.put("latitude", "varchar(30)");
		if(createTable(tbl_registration_master, cols, db))
		{
			Log.w("Create URL", "Successful on new database");
		}
		else
		{
			Log.w("Create URL", "Error on database creation");
		}
		
		cols.clear();
	}

	public boolean createTable(String tableName,LinkedHashMap<String, String> colums,SQLiteDatabase db)
	{
		try
		{
			Set<String> keys=colums.keySet();
			Iterator <String> iterator=keys.iterator();
			String cmd="create table "+tableName+"( _id INTEGER PRIMARY KEY,";
			while(iterator.hasNext())
			{
				String key=iterator.next();
				cmd+=key+" "+colums.get(key)+",";
			}
			cmd=cmd.substring(0,cmd.length()-1);
			cmd+=");";
			
			Log.w("Create Table", "query : "+cmd);
			db.execSQL(cmd);
			Log.w("Create Table", "Success : "+tableName);
			return true;
		}
		catch(Exception ex)
		{
			Log.w("Create Error", ex.toString());
			return false;
		}
	}
	 
	public Cursor getCursor(String tableName, String[] columns, String whereClause, String[] whereArgs, String groupBy, String having, String orderBy)
	{
		Cursor cur = null;
		try
		{
			database=getReadableDatabase();
			cur = database.query(tableName, columns, whereClause, whereArgs, groupBy, having, orderBy);	
		}
		catch(Exception ex)
		{
			Log.w("Error", ex.toString());
		}
		
		return cur;
	}
	
	public String getText(String tableName, String returnValue, String whereClause, String[] whereArgs)
	{
		String value=null;
		
		Cursor cur = getCursor(tableName, (new String[]{ returnValue }), whereClause, whereArgs, null, null, null);
		if(cur.moveToNext())
		{
			value= cur.getString(0);
		}
		
		return value;
	}
	
	public String getMin(String tableName, String returnValue, String whereClause, String[] whereArgs)
	{
		String value=null;
		
		Cursor cur = getCursor(tableName, (new String[]{ "MIN("+returnValue+")" }), whereClause, whereArgs, null, null, null);
		if(cur.moveToNext())
		{
			value= cur.getString(0);
		}
		
		return value;
	}
	
	public String getMax(String tableName, String returnValue, String whereClause, String[] whereArgs)
	{
		String value=null;
		
		Cursor cur = getCursor(tableName, (new String[]{ "MAX("+returnValue+")" }), whereClause, whereArgs, null, null, null);
		if(cur.moveToNext())
		{
			value= cur.getString(0);
		}
		
		return value;
	}
	
	public String getSum(String tableName, String returnValue, String whereClause, String[] whereArgs)
	{
		String value=null;
		
		Cursor cur = getCursor(tableName, (new String[]{ "SUM("+returnValue+")" }), whereClause, whereArgs, null, null, null);
		if(cur.moveToNext())
		{
			value= cur.getString(0);
		}
		
		return value;
	}
	
	public long getCount(String tableName, String whereClause, String[] whereArgs)
	{
		Cursor cur = getCursor(tableName, (new String[]{ "*" }), whereClause, whereArgs, null, null, null);
		return cur.getCount();
	}
	
	public long getColumnCount(String tableName, String[] columns, String whereClause, String[] whereArgs)
	{
		Cursor cur = getCursor(tableName, columns, whereClause, whereArgs, null, null, null);
		return cur.getColumnCount();
	}
	
	public long insertData(String tableName,ContentValues values)
	{
		database=getWritableDatabase();
		return database.insert(tableName, null, values);
		
	}
	
	public int updateData(String tableName,ContentValues values,String whereClause, String[] whereArgs)
	{
		database=getWritableDatabase();
		return database.update(tableName, values, whereClause, whereArgs);
	}

	public int deleteData(String tableName,String whereClause, String[] whereArgs)
	{
		database=getWritableDatabase();
		return database.delete(tableName, whereClause, whereArgs);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		 // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + tbl_registration_master);
 
        // Create tables again
        onCreate(db);
	}
	
	public long DateToInt(String dd_MM_yyyy) {
        return StringToDate(dd_MM_yyyy).getTime() / 1000;
    }

    /**
     * public long TimeToInt(Time date) { return date.getTime() / 1000; }
     *
     *
     *
     * public Date IntToTime(long sec) { return new Time(sec * 1000); }
     *
     */
    public Date IntToDate(Object seconds) {
        long sec = Long.parseLong(seconds.toString());
        return new Date(sec * 1000);
    }

    public String IntToDateStr(Object seconds) {
        long sec = Long.parseLong(seconds.toString());
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        return format.format(new Date(sec * 1000));
    }

    public String SysDateStr() {
        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        return format.format(d);
    }

    public String SysTimeStr() {
        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
        return format.format(d);
    }

    public Date SysDate() {
        return Calendar.getInstance().getTime();
    }

    public Calendar GetCalendar(String dd_MM_yyyy) {
        String[] arr = dd_MM_yyyy.split("-");
        Calendar cal = Calendar.getInstance();
        try {
            int dd = Integer.parseInt(arr[0]);
            int MM = Integer.parseInt(arr[1]);
            int yyyy = Integer.parseInt(arr[2]);
            /**
             * In Java Month is between 0-11 ie: 0=January and 11-December
             */

            cal.set(yyyy, MM - 1, dd, 0, 0, 0);
        } catch (Exception ex) {
            Log.w("GetCalender", ex.toString());
        }
        return cal;
    }

    public Date StringToDate(String dd_MM_yyyy) {
        return GetCalendar(dd_MM_yyyy).getTime();
    }

    public int GetWeekDayInt(String dd_MM_yyyy) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public String WeekDayStr(String dd_MM_yyyy) {
        String[] days = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[GetWeekDayInt(dd_MM_yyyy) - 1];
    }

    public int GetMonthInt(String dd_MM_yyyy) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        return cal.get(Calendar.MONTH) + 1;
    }

    public String GetMonthStr(String dd_MM_yyyy) {
        String[] days = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        return days[GetMonthInt(dd_MM_yyyy) - 1];
    }

    public int GetYear(String dd_MM_yyyy) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        return cal.get(Calendar.YEAR);
    }

    public String DateToString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        return format.format(date);
    }

    public String AddMonth(String dd_MM_yyyy, int months) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        cal.add(Calendar.MONTH, months);
        return DateToString(cal.getTime());
    }

    public String AddDay(String dd_MM_yyyy, int days) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return DateToString(cal.getTime());
    }

    public String AddYear(String dd_MM_yyyy, int years) {
        Calendar cal = GetCalendar(dd_MM_yyyy);
        cal.add(Calendar.YEAR, years);
        return DateToString(cal.getTime());
    }

    /**
     *
     * @param time it need to be supplied as 10:00 AM or 10:00:00 AM for 12 Hour
     * time 10:00 or 10:00:00 for 24 Hour time
     * @return It returns Number of Milliseconds Elapsed Since 00:00:00:000
     */
    public long TimeToInt(String time) {

        String[] arrTime = time.split(":");
        int hours = 0, min = 0, sec = 0;
        hours = Integer.parseInt(arrTime[0]);
        min = Integer.parseInt(arrTime[1].substring(0, 2));

        if (time.toUpperCase().contains("M")) {

            int index = arrTime.length > 2 ? 2 : 1;

            if (arrTime[index].toUpperCase().contains("P"))
                hours += 12;

            if (index == 2)
                sec = Integer.parseInt(arrTime[2].substring(0, 2));
        } else {
            if (arrTime.length > 2) {
                sec = Integer.parseInt(arrTime[2]);
            }
        }

        return Time.valueOf(hours + ":" + min + ":" + sec).getTime();
    }

    public String IntToTime24(Object millisec) {
        long sec = Long.parseLong(millisec.toString());
        Time t = new Time(sec);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(t);
    }

    public String IntToTime12(Object millisec) {
        long sec = Long.parseLong(millisec.toString());
        Time t = new Time(sec);
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
        return format.format(t);
    }

}
