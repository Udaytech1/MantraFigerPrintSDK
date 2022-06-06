package com.mantra.BiometricAttendance;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.mantra.BiometricAttendance.database.DBHelper;
import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;


public class MFS100MatchFinger extends Activity implements MFS100Event {

    Button btnInit, btnUninit, btnClearLog;
    EditText txtEventLog, txtSearchMobileNo;
    ImageView imgFinger;
    TextView lblMessage;

    private enum ScannerAction {
        Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    MFS100MatchFinger.ScannerAction scannerAction = ScannerAction.Verify;

    int timeout = 10000;
    MFS100 mfs100 = null;

    private boolean isCaptureRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfs100_match_finger);

        btnInit = (Button) findViewById(R.id.btnInit);
        btnUninit = (Button) findViewById(R.id.btnUninit);
        btnClearLog = (Button) findViewById(R.id.btnClear);
        txtEventLog = (EditText)  findViewById(R.id.txtEventLog);
        imgFinger = (ImageView) findViewById(R.id.imgFinger);
        txtSearchMobileNo = (EditText) findViewById(R.id.txtSearchMobileNo);
        txtSearchMobileNo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(txtSearchMobileNo.getText().length() == 10) {
                    scannerAction = ScannerAction.Verify;
                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                }
            }
        });
        lblMessage = (TextView) findViewById(R.id.lblMessage);

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
            mfs100.SetApplicationContext(MFS100MatchFinger.this);
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

    public void onControlClicked(View v) {

        switch (v.getId()) {
            case R.id.btnInit:
                InitScanner();
                break;
            case R.id.btnUninit:
                UnInitScanner();
                break;
            case R.id.btnClear:
                ClearLog();
                break;
            default:
                break;
        }
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
                    int ret = mfs100.AutoCapture(fingerData, timeout, false);
                    Log.e("StartSyncCapture.RET", ""+ret);
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        lastCapFingerData = fingerData;
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);
                        MFS100MatchFinger.this.runOnUiThread(new Runnable() {
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
                    SetTextOnUIThread("Error" + ex.toString());
                } finally {
                    isCaptureRunning = false;
                }
            }
        }).start();
    }

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

    private void ClearLog() {
        txtEventLog.post(new Runnable() {
            public void run() {
                txtEventLog.setText("", BufferType.EDITABLE);
            }
        });
    }

    private void SetTextOnUIThread(final String str) {
        lblMessage.post(new Runnable() {
            public void run() {
                lblMessage.setText(str);
            }
        });
    }

    private void SetLogOnUIThread(final String str) {
        txtEventLog.post(new Runnable() {
            public void run() {
                txtEventLog.append("\n" + str);
            }
        });
    }

    public void SetData2(FingerData fingerData) {
        try {
            Verify_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                    fingerData.ISOTemplate().length);

            DBHelper db = new DBHelper(this);
            String tableName = "tbl_registration_master";
            String[] Columns = new String[]{"name", "EnrollTemplate"};
            String WhereClause = "mobile_no=?";
            String[] WhereArgs = new String[]{String.valueOf(txtSearchMobileNo.getText().toString())};
            Cursor cur = db.getCursor(tableName, Columns, WhereClause, WhereArgs, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    if (cur.getBlob(cur.getColumnIndex("EnrollTemplate")) != null) {
                        Enroll_Template = cur.getBlob(cur.getColumnIndex("EnrollTemplate"));
                    }
                    int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
                    if (ret < 0) {
                        SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                    } else {
                        if (ret >= 1400) {
                            SetTextOnUIThread("Finger matched with score: " + ret);
                            String log = "\nEmployee Name: " + cur.getString(cur.getColumnIndex("name"))
                                    + "\n...";
                            SetLogOnUIThread(log);
                            return;
                        } else {
                            SetTextOnUIThread("Finger not matched, score: " + ret);
                        }
                    }
                }
                else
                {
                    SetLogOnUIThread("Cursor Next Data not found.");
                }
            }
            else
            {
                SetLogOnUIThread("Cursor Data not found.");
            }
        } catch (Exception e) {
            SetLogOnUIThread(e.toString());
        }
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
