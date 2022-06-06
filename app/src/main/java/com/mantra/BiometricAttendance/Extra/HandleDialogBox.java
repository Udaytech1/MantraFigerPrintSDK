package com.mantra.BiometricAttendance.Extra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class HandleDialogBox extends Activity {

	public AlertDialog alert(String title, String msg, String positiveButton, String negativeButton, Context context)
	{
		AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(context);
	    alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(msg);
        alertDialogBuilder.setPositiveButton(positiveButton, 
	         new DialogInterface.OnClickListener() {
	         @Override
	         public void onClick(DialogInterface arg0, int arg1) {
	        	 
	         }
        });

        alertDialogBuilder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
	         @Override
	         public void onClick(DialogInterface dialog, int which) {
	        	 finish();
	         }
        });
        
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        return alertDialog;
	}
	
	public AlertDialog.Builder alertDBuilder(String title, String msg, Context context)
	{
		AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(context);
	    alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(msg);
        return alertDialogBuilder;
	}
	
	public AlertDialog alert(AlertDialog.Builder alertDialogBuilder)
	{
		AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        return alertDialog;
	}
	
}
