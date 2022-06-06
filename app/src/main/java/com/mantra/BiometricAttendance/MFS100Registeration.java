package com.mantra.BiometricAttendance;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.content.ContentValues;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import java.io.File;
import java.io.FileOutputStream;

import com.mantra.BiometricAttendance.Extra.GPSTracker;
import com.mantra.BiometricAttendance.Extra.HandleDialogBox;
import com.mantra.BiometricAttendance.database.DBHelper;

public class MFS100Registeration extends Activity implements MFS100Event {

    Button btnInit, btnUninit, btnSyncCapture, btnStopCapture, btnRegister;
    TextView lblMessage;
    EditText txtEmployeeName, txtMobileNo, txtDistrictCode, txtBlockCode, txtClusterCode, txtSchoolCode;
    ImageView imgFinger;
    CheckBox cbFastDetection;

    GPSTracker gps;
    DBHelper db;

    private ProgressDialog progressBar;

    private enum ScannerAction {
        Capture, Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    ScannerAction scannerAction = ScannerAction.Capture;

    int timeout = 10000;
    MFS100 mfs100 = null;

    private boolean isCaptureRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfs100_sample);

        //Create Database
        db = new DBHelper(this);

        // create GPS Object
        gps = new GPSTracker(this);
        if(!gps.canGetLocation()){
            gps.showSettingsAlert();
        }

        //global = (Global)getApplicationContext();

        FindFormControls();
        try {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }
    }

    @Override
    protected void onStart() {
        if (mfs100 == null) {
            mfs100 = new MFS100(this);
            mfs100.SetApplicationContext(MFS100Registeration.this);
        } else {
            InitScanner();
        }
        super.onStart();
    }

    protected void onStop() {
        UnInitScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mfs100 != null) {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    public void FindFormControls() {
        btnInit = (Button) findViewById(R.id.btnInit);
        btnUninit = (Button) findViewById(R.id.btnUninit);
        lblMessage = (TextView) findViewById(R.id.lblMessage);
        imgFinger = (ImageView) findViewById(R.id.imgFinger);
        btnSyncCapture = (Button) findViewById(R.id.btnSyncCapture);
        btnStopCapture = (Button) findViewById(R.id.btnStopCapture);
        cbFastDetection = (CheckBox) findViewById(R.id.cbFastDetection);

        txtEmployeeName = (EditText) findViewById(R.id.txtEmployeeName);
        txtMobileNo = (EditText) findViewById(R.id.txtMobileNo);
        txtDistrictCode = (EditText) findViewById(R.id.txtDistrictCode);
        txtBlockCode = (EditText) findViewById(R.id.txtBlockCode);
        txtClusterCode = (EditText) findViewById(R.id.txtClusterCode);
        txtSchoolCode = (EditText) findViewById(R.id.txtSchoolCode);

        btnRegister = (Button) findViewById(R.id.btnRegister);
    }

    public void onControlClicked(final View v) {

        switch (v.getId()) {
            case R.id.btnInit:
                InitScanner();
                break;
            case R.id.btnUninit:
                UnInitScanner();
                break;
            case R.id.btnSyncCapture:
                scannerAction = ScannerAction.Capture;
                if (!isCaptureRunning) {
                    StartSyncCapture();
                }
                break;
            case R.id.btnStopCapture:
                StopCapture();
                break;
            case R.id.btnRegister:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        this);
                alertDialogBuilder.setTitle("Warning");
                alertDialogBuilder
                        .setMessage("Do You want to save this data.");

                alertDialogBuilder.setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                RegisterUser(v);
                            }
                        });

                alertDialogBuilder.setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(),
                                        "Waiting for your response.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                HandleDialogBox hdb = new HandleDialogBox();
                hdb.alert(alertDialogBuilder);
                break;
            default:
                break;
        }
    }

    public boolean validate_input() {

        if(lastCapFingerData == null)
        {
            HandleDialogBox hdb = new HandleDialogBox();
            hdb.alert("Error Message", "No Fingerprint detected.",
                    null, "OK", this);
            return false;
        }
        else {
            if (txtEmployeeName.getText().toString().trim().equals("")) {
                txtEmployeeName.setError("Employee name field is required.");
                return false;
            }
            if (txtMobileNo.getText().toString().trim().equals("")) {
                txtMobileNo.setError("10 digit mobile no is required.");
                return false;
            }
            if (txtMobileNo.getText().toString().trim().length() != 10) {
                txtMobileNo.setError("Enter 10 digit mobile no.");
                return false;
            }
            if (txtDistrictCode.getText().toString().trim().equals("")) {
                txtDistrictCode.setError("District Code is required.");
                return false;
            }
            if (txtBlockCode.getText().toString().trim().equals("")) {
                txtBlockCode.setError("Block Code is required.");
                return false;
            }
            if (txtClusterCode.getText().toString().trim().equals("")) {
                txtClusterCode.setError("Cluster Code is required.");
                return false;
            }
            if (txtSchoolCode.getText().toString().trim().equals("")) {
                txtSchoolCode.setError("District Code is required.");
                return false;
            }
        }
        return true;
    }

    private void RegisterUser(View v)
    {
        if(validate_input()) {
            try {
                // create class object
                gps = new GPSTracker(this);

                // check if GPS enabled
                if (gps.canGetLocation()) {
                    progressBar = new ProgressDialog(v.getContext());
                    progressBar.setCancelable(true);
                    progressBar.setMessage("Wait for submitting in server......");
                    progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressBar.setProgress(0);
                    progressBar.setMax(100);
                    progressBar.show();

                    double longitude = gps.getLongitude();
                    double latitude = gps.getLatitude();

                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    final String imei = telephonyManager.getDeviceId();

                /* code to insert data in database */
                    ContentValues values = new ContentValues();
                    values.put("name", txtEmployeeName.getText().toString().toUpperCase());
                    values.put("mobile_no", txtMobileNo.getText().toString().toUpperCase());
                    values.put("districtcode", txtDistrictCode.getText().toString().toUpperCase());
                    values.put("blockcode", txtBlockCode.getText().toString().toUpperCase());
                    values.put("clustercode", txtClusterCode.getText().toString().toUpperCase());
                    values.put("schoolcode", txtSchoolCode.getText().toString().toUpperCase());
                    values.put("EnrollTemplate", Enroll_Template);
                    values.put("imei_id", imei);
                    values.put("longitude", longitude);
                    values.put("latitude", latitude);
                    long res = db.insertData(db.tbl_registration_master, values);
                    if(res > 0)
                    {
                        progressBar.dismiss();
                        Toast.makeText(getBaseContext(), "You are successfully registered.",
                                Toast.LENGTH_LONG).show();
                        clear();
                        return;
                    }
                }
                else
                {
                    HandleDialogBox hdb = new HandleDialogBox();
                    hdb.alert("Error Message", "First enable Location Services on your device.",
                            null, "OK", this);
                }
            } catch (Exception e) {
                HandleDialogBox hdb = new HandleDialogBox();
                hdb.alert("Error Message", e.getMessage(),
                        null, "OK", this);
            }
        }
    }

    public void clear() {
        txtEmployeeName.setText("");
        txtMobileNo.setText("");
        txtDistrictCode.setText("");
        txtBlockCode.setText("");
        txtClusterCode.setText("");
        txtSchoolCode.setText("");
        imgFinger.setImageDrawable(null);
        this.UnInitScanner();
        this.StopCapture();
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                SetLogOnUIThread(info);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture() {
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				SetTextOnUIThread("");
                isCaptureRunning = true;
		        try {
		            FingerData fingerData = new FingerData();
		            int ret = mfs100.AutoCapture(fingerData, timeout, cbFastDetection.isChecked());
		            Log.e("StartSyncCapture.RET", ""+ret);
		            if (ret != 0) {
		                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
		            } else {
		                lastCapFingerData = fingerData;
		                final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
		                        fingerData.FingerImage().length);
		                MFS100Registeration.this.runOnUiThread(new Runnable() {
		                    @Override
		                    public void run() {
		                        imgFinger.setImageBitmap(bitmap);
		                    }
		                });

		                SetTextOnUIThread("Capture Success");
		                String log = "\nQuality: " + fingerData.Quality()
		                        + "\nNFIQ: " + fingerData.Nfiq()
		                        + "\nWSQ Compress Ratio: "
		                        + fingerData.WSQCompressRatio()
		                        + "\nImage Dimensions (inch): "
		                        + fingerData.InWidth() + "\" X "
		                        + fingerData.InHeight() + "\""
		                        + "\nImage Area (inch): " + fingerData.InArea()
		                        + "\"" + "\nResolution (dpi/ppi): "
		                        + fingerData.Resolution() + "\nGray Scale: "
		                        + fingerData.GrayScale() + "\nBits Per Pixal: "
		                        + fingerData.Bpp() + "\nWSQ Info: "
		                        + fingerData.WSQInfo();
		                SetLogOnUIThread(log);
		                SetData2(fingerData);
		            }
		        } catch (Exception ex) {
		            SetTextOnUIThread("Error");
		        } finally {
                    isCaptureRunning = false;
                }
            }
		}).start();
    }
    
    private void StopCapture() {
    	try {
			mfs100.StopAutoCapture();
		} catch (Exception e) {
			SetTextOnUIThread("Error");
		}
    }

    /*private void ExtractANSITemplate() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[2000]; // length 2000 is mandatory
            byte[] ansiTemplate;
            int dataLen = mfs100.ExtractANSITemplate(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ANSI Template");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                ansiTemplate = new byte[dataLen];
                System.arraycopy(tempData, 0, ansiTemplate, 0, dataLen);
                WriteFile("ANSITemplate.ansi", ansiTemplate);
                SetTextOnUIThread("Extract ANSI Template Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ANSI Template Error", e);
        }
    }

    private void ExtractISOImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] isoImage;
            int dataLen = mfs100.ExtractISOImage(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ISO Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                isoImage = new byte[dataLen];
                System.arraycopy(tempData, 0, isoImage, 0, dataLen);
                WriteFile("ISOImage.iso", isoImage);
                SetTextOnUIThread("Extract ISO Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ISO Image Error", e);
        }
    }

    private void ExtractWSQImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] wsqImage;
            int dataLen = mfs100.ExtractWSQImage(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract WSQ Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                wsqImage = new byte[dataLen];
                System.arraycopy(tempData, 0, wsqImage, 0, dataLen);
                WriteFile("WSQ.wsq", wsqImage);
                SetTextOnUIThread("Extract WSQ Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract WSQ Image Error", e);
        }
    } */

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetLogOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void WriteFile(String filename, byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /*
    private void ClearLog() {
        txtEventLog.post(new Runnable() {
            public void run() {
                txtEventLog.setText("", BufferType.EDITABLE);
            }
        });
    }

    */

    private void SetTextOnUIThread(final String str) {

        lblMessage.post(new Runnable() {
            public void run() {
                lblMessage.setText(str);
            }
        });
    }

    private void SetLogOnUIThread(final String str) {
    }

    public void SetData2(FingerData fingerData) {
        if (scannerAction.equals(ScannerAction.Capture)) {
            Enroll_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                    fingerData.ISOTemplate().length);
        } else if (scannerAction.equals(ScannerAction.Verify)) {
            Verify_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                    fingerData.ISOTemplate().length);
            int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
            if (ret < 0) {
                SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
            } else {
                if (ret >= 1400) {
                    SetTextOnUIThread("Finger matched with score: " + ret);
                } else {
                    SetTextOnUIThread("Finger not matched, score: " + ret);
                }
            }
        }

        WriteFile("Raw.raw", fingerData.RawData());
        WriteFile("Bitmap.bmp", fingerData.FingerImage());
        WriteFile("ISOTemplate.iso", fingerData.ISOTemplate());
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279) {
            if (pid == 34323) {
                ret = mfs100.LoadFirmware();
                if (ret != 0) {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                } else {
                    SetTextOnUIThread("Load firmware success");
                }
            } else if (pid == 4101) {
                String key = "Without Key";
                ret = mfs100.Init();
                if (ret == 0) {
                    showSuccessLog(key);
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                }

            }
        }
    }

    private void showSuccessLog(String key) {
        SetTextOnUIThread("Init success");
        String info = "\nKey: " + key + "\nSerial: "
                + mfs100.GetDeviceInfo().SerialNo() + " Make: "
                + mfs100.GetDeviceInfo().Make() + " Model: "
                + mfs100.GetDeviceInfo().Model()
                + "\nCertificate: " + mfs100.GetCertification();
        SetLogOnUIThread(info);
    }

    @Override
    public void OnDeviceDetached() {
        UnInitScanner();
        SetTextOnUIThread("Device removed");
    }

    @Override
    public void OnHostCheckFailed(String err) {
        try {
            SetLogOnUIThread(err);
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }

}
