package com.test.ginopaydemo;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.bbpos.bbdevice.BBDeviceController.AccountSelectionResult;
import com.bbpos.bbdevice.BBDeviceController.AudioAutoConfigError;
import com.bbpos.bbdevice.BBDeviceController.ContactlessStatus;
import com.bbpos.bbdevice.BBDeviceController.ContactlessStatusTone;
import com.bbpos.bbdevice.BBDeviceController.NfcDetectCardResult;
import com.bbpos.bbdevice.BBDeviceController.VASResult;
import com.bbpos.bbdevice.CAPK;
import com.bbpos.bbdevice.VASMerchantConfig;
import com.bbpos.bbdevice.BBDeviceController;
import com.bbpos.bbdevice.BBDeviceController.AmountInputType;
import com.bbpos.bbdevice.BBDeviceController.BatteryStatus;
import com.bbpos.bbdevice.BBDeviceController.CheckCardMode;
import com.bbpos.bbdevice.BBDeviceController.CheckCardResult;
import com.bbpos.bbdevice.BBDeviceController.ConnectionMode;
import com.bbpos.bbdevice.BBDeviceController.CurrencyCharacter;
import com.bbpos.bbdevice.BBDeviceController.DisplayText;
import com.bbpos.bbdevice.BBDeviceController.EmvOption;
import com.bbpos.bbdevice.BBDeviceController.EncryptionKeySource;
import com.bbpos.bbdevice.BBDeviceController.EncryptionKeyUsage;
import com.bbpos.bbdevice.BBDeviceController.EncryptionMethod;
import com.bbpos.bbdevice.BBDeviceController.EncryptionPaddingMethod;
import com.bbpos.bbdevice.BBDeviceController.Error;
import com.bbpos.bbdevice.BBDeviceController.PhoneEntryResult;
import com.bbpos.bbdevice.BBDeviceController.PinEntryResult;
import com.bbpos.bbdevice.BBDeviceController.PinEntrySource;
import com.bbpos.bbdevice.BBDeviceController.PrintResult;
import com.bbpos.bbdevice.BBDeviceController.ReadNdefRecord;
import com.bbpos.bbdevice.BBDeviceController.SessionError;
import com.bbpos.bbdevice.BBDeviceController.TerminalSettingStatus;
import com.bbpos.bbdevice.BBDeviceController.TransactionResult;
import com.bbpos.bbdevice.BBDeviceController.TransactionType;
import com.bbpos.bbdevice.BBDeviceController.DisplayPromptOption;
import com.bbpos.bbdevice.BBDeviceController.DisplayPromptResult;
import com.bbpos.bbdevice.BBDeviceController.BBDeviceControllerListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    protected static final String[] DEVICE_NAMES = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    protected enum State {
        GETTING_PSE, READING_RECORD, READING_AID, GETTING_PROCESS_OPTION, READING_DATA
    }

    private static String[] aids = new String[]{"A0000000031010", "A0000000041010", "A00000002501"};


    protected static Dialog dialog;
    protected static ProgressDialog progressDialog;
    protected static MainActivity currentActivity;
    protected static EditText statusEditText;
    private TextView textViewCardData;

    private static boolean isVersionShown = false;
    protected static BBDeviceController bbDeviceController;
    protected static MyBBDeviceControllerListener listener;
    protected static ArrayAdapter<String> arrayAdapter;
    protected static List<BluetoothDevice> foundDevices;
    protected static final int PERMISSION_REQUEST_CODE = 200;
    protected static String cardholderName;
    protected static String expiryDate;
    protected static String track2 = "";
    protected static String pan = "";
    protected static State state = null;
    protected static long startTime;
    protected static final String CBC = "CBC";
    protected static final String DATA_KEY = "Data Key";
    protected static String keyMode = DATA_KEY;
    protected static final String DATA_KEY_VAR = "Data Key Var";
    protected static String encryptionMode = CBC;

    private static int readingFileIndex = 0;
    private static int aflCounter = 0;
    private static int aidCounter = 0;
    private static String sfi = "";
    protected static boolean isPKCS7 = false;
    private static String[] afls = null;
    private static int total = 0;

    protected String ksn = "";

    protected static boolean isApduEncrypted = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusEditText = findViewById(R.id.statusEditText);
        textViewCardData = findViewById(R.id.textViewCardData);
        currentActivity = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.BBPOS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.BBPOS"}, PERMISSION_REQUEST_CODE);
            }
        }

        if (bbDeviceController == null) {
            listener = new MyBBDeviceControllerListener();
            bbDeviceController = BBDeviceController.getInstance(getApplicationContext(), listener);
            BBDeviceController.setDebugLogEnabled(true);
            bbDeviceController.setDetectAudioDevicePlugged(true);
        }
        setTitle(getString(R.string.app_name) + " " + BBDeviceController.getApiVersion());


    }

    @SuppressLint("Override")
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AAAAAA", "5");
                    // permission was granted.
                } else {
                    // permission denied.
                    Log.d("AAAAAA", "6");
                }
                return;
            }
        }
    }


    class MyBBDeviceControllerListener implements BBDeviceControllerListener {

        @Override
        public void onWaitingForCard(CheckCardMode checkCardMode) {
            dismissDialog();
            switch (checkCardMode) {
                case INSERT:
                    statusEditText.setText(getString(R.string.please_insert_card));
                    break;
                case SWIPE:
                    statusEditText.setText(getString(R.string.please_swipe_card));
                    break;
                case SWIPE_OR_INSERT:
                    statusEditText.setText(getString(R.string.please_swipe_insert_card));
                    break;
                case TAP:
                    statusEditText.setText(getString(R.string.please_tap_card));
                    break;
                case SWIPE_OR_TAP:
                    statusEditText.setText(getString(R.string.please_swipe_tap_card));
                    break;
                case INSERT_OR_TAP:
                    statusEditText.setText(getString(R.string.please_insert_tap_card));
                    break;
                case SWIPE_OR_INSERT_OR_TAP:
                    statusEditText.setText(getString(R.string.please_swipe_insert_tap_card));
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onWaitingReprintOrPrintNext() {

        }

        @Override
        public void onBTReturnScanResults(List<BluetoothDevice> foundDevices) {
            currentActivity.foundDevices = foundDevices;
            if (arrayAdapter != null) {
                arrayAdapter.clear();
                for (int i = 0; i < foundDevices.size(); ++i) {
                    arrayAdapter.add(foundDevices.get(i).getName());
                }
                arrayAdapter.notifyDataSetChanged();
            }

        }

        @Override
        public void onBTScanTimeout() {

        }

        @Override
        public void onBTScanStopped() {

        }

        @Override
        public void onBTConnected(BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onBTDisconnected() {

        }

        @Override
        public void onBTRequestPairing() {

        }

        @Override
        public void onUsbConnected() {

        }

        @Override
        public void onUsbDisconnected() {

        }

        @Override
        public void onSerialConnected() {

        }

        @Override
        public void onSerialDisconnected() {

        }

        @Override
        public void onReturnCheckCardResult(CheckCardResult checkCardResult, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnCancelCheckCardResult(boolean b) {

        }

        @Override
        public void onReturnDeviceInfo(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnTransactionResult(TransactionResult transactionResult) {

        }

        @Override
        public void onReturnBatchData(String s) {

        }

        @Override
        public void onReturnReversalData(String s) {

        }

        @Override
        public void onReturnAmountConfirmResult(boolean b) {

        }

        @Override
        public void onReturnPinEntryResult(PinEntryResult pinEntryResult, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPrintResult(PrintResult printResult) {

        }

        @Override
        public void onReturnAccountSelectionResult(AccountSelectionResult accountSelectionResult, int i) {

        }

        @Override
        public void onReturnAmount(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnUpdateAIDResult(Hashtable<String, TerminalSettingStatus> hashtable) {

        }

        @Override
        public void onReturnUpdateGprsSettingsResult(boolean b, Hashtable<String, TerminalSettingStatus> hashtable) {

        }

        @Override
        public void onReturnUpdateTerminalSettingResult(TerminalSettingStatus terminalSettingStatus) {

        }

        @Override
        public void onReturnUpdateWiFiSettingsResult(boolean b, Hashtable<String, TerminalSettingStatus> hashtable) {

        }

        @Override
        public void onReturnReadAIDResult(Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onReturnReadGprsSettingsResult(boolean b, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onReturnReadTerminalSettingResult(Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onReturnReadWiFiSettingsResult(boolean b, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onReturnEnableAccountSelectionResult(boolean b) {

        }

        @Override
        public void onReturnEnableInputAmountResult(boolean b) {

        }

        @Override
        public void onReturnCAPKList(List<CAPK> list) {

        }

        @Override
        public void onReturnCAPKDetail(CAPK capk) {

        }

        @Override
        public void onReturnCAPKLocation(String s) {

        }

        @Override
        public void onReturnUpdateCAPKResult(boolean b) {

        }

        @Override
        public void onReturnRemoveCAPKResult(boolean b) {

        }

        @Override
        public void onReturnEmvReportList(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnEmvReport(String s) {

        }

        @Override
        public void onReturnDisableAccountSelectionResult(boolean b) {

        }

        @Override
        public void onReturnDisableInputAmountResult(boolean b) {

        }

        @Override
        public void onReturnPhoneNumber(PhoneEntryResult phoneEntryResult, String s) {

        }

        @Override
        public void onReturnEmvCardDataResult(boolean b, String s) {

        }

        @Override
        public void onReturnEmvCardNumber(boolean b, String s) {

        }

        @Override
        public void onReturnEncryptPinResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnEncryptDataResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnInjectSessionKeyResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPowerOnIccResult(boolean isSuccess, String ksn, String atr, int atrLength) {

            dismissDialog();
            if (isSuccess) {
                MainActivity.this.ksn = ksn;

                setStatus(getString(R.string.power_on_icc_success));
                setStatus(getString(R.string.ksn) + ksn);
                setStatus(getString(R.string.atr) + atr);
                setStatus(getString(R.string.atr_length) + atrLength);
            } else {
                setStatus(getString(R.string.power_on_icc_failed));
            }

        }

        @Override
        public void onReturnPowerOffIccResult(boolean b) {

        }

        @Override
        public void onReturnApduResult(boolean isSuccess, Hashtable<String, Object> data) {
            try {
                String apdu = "";
                int apduLength = 0;

                if ((data != null) && (data.containsKey("apduLength")) && (data.get("apduLength") instanceof String)) {
                    apduLength = Integer.parseInt((String) data.get("apduLength"));
                } else if ((data != null) && (data.containsKey("apduLength")) && (data.get("apduLength") instanceof Integer)) {
                    apduLength = (Integer) data.get("apduLength");
                }

                if ((data != null) && (data.containsKey("apdu"))) {
                    apdu = (String) data.get("apdu");
                    handleApduResult(isSuccess, apdu, apduLength);
                }
            } catch (Exception e) {

            }

        }

        @Override
        public void onRequestSelectApplication(ArrayList<String> arrayList) {

        }

        @Override
        public void onRequestSetAmount() {

        }

        @Override
        public void onRequestPinEntry(PinEntrySource pinEntrySource) {

        }

        @Override
        public void onRequestOnlineProcess(String s) {

        }

        @Override
        public void onRequestTerminalTime() {

        }

        @Override
        public void onRequestDisplayText(DisplayText displayText) {

        }

        @Override
        public void onRequestDisplayAsterisk(int i) {

        }

        @Override
        public void onRequestDisplayLEDIndicator(ContactlessStatus contactlessStatus) {

        }

        @Override
        public void onRequestProduceAudioTone(ContactlessStatusTone contactlessStatusTone) {

        }

        @Override
        public void onRequestClearDisplay() {

        }

        @Override
        public void onRequestFinalConfirm() {

        }

        @Override
        public void onRequestPrintData(int i, boolean b) {

        }

        @Override
        public void onPrintDataCancelled() {

        }

        @Override
        public void onPrintDataEnd() {

        }

        @Override
        public void onBatteryLow(BatteryStatus batteryStatus) {

        }

        @Override
        public void onAudioDevicePlugged() {

        }

        @Override
        public void onAudioDeviceUnplugged() {

        }

        @Override
        public void onError(Error error, String s) {

        }

        @Override
        public void onSessionInitialized() {

        }

        @Override
        public void onSessionError(SessionError sessionError, String s) {

        }

        @Override
        public void onAudioAutoConfigProgressUpdate(double v) {

        }

        @Override
        public void onAudioAutoConfigCompleted(boolean b, String s) {

        }

        @Override
        public void onAudioAutoConfigError(AudioAutoConfigError audioAutoConfigError) {

        }

        @Override
        public void onNoAudioDeviceDetected() {

        }

        @Override
        public void onDeviceHere(boolean b) {

        }

        @Override
        public void onPowerDown() {

        }

        @Override
        public void onPowerButtonPressed() {

        }

        @Override
        public void onDeviceReset() {

        }

        @Override
        public void onEnterStandbyMode() {

        }

        @Override
        public void onReturnNfcDataExchangeResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnNfcDetectCardResult(NfcDetectCardResult nfcDetectCardResult, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onReturnControlLEDResult(boolean b, String s) {

        }

        @Override
        public void onReturnVasResult(VASResult vasResult, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onRequestStartEmv() {

        }

        @Override
        public void onDeviceDisplayingPrompt() {

        }

        @Override
        public void onRequestKeypadResponse() {

        }

        @Override
        public void onReturnDisplayPromptResult(DisplayPromptResult displayPromptResult) {

        }

        @Override
        public void onReturnFunctionKey(BBDeviceController.FunctionKey functionKey) {

        }

        @Override
        public void onBarcodeReaderConnected() {

        }

        @Override
        public void onBarcodeReaderDisconnected() {

        }

        @Override
        public void onReturnBarcode(String s) {

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isVersionShown) {
            isVersionShown = true;
            statusEditText.setText("BBDevice API : " + BBDeviceController.getApiVersion());
        }
    }


    public void onStartConnection(View view) {
        promptForConnection();

    }

    public void onPowerOnIcc(View view) {
        bbDeviceController.powerOnIcc(new Hashtable<String, Object>());
    }

    public void onPowerOffIcc(View view) {
        bbDeviceController.powerOffIcc();
    }

    public void onClearLog(View view) {
        statusEditText.setText("");
        textViewCardData.setText(" ");
    }

    public void onSendAPDU(View view) {

        if (ksn.equals("")) {
            setStatus(getString(R.string.please_power_on_icc));
            return;
        }
        cardholderName = "";
        expiryDate = "";
        pan = "";
        track2 = "";

        state = State.GETTING_PSE;
        sendApdu("00A404000E315041592E5359532E444446303100");
        setStatus("Getting PSE...");

        startTime = System.currentTimeMillis();
    }

    public void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public void promptForConnection() {
        dismissDialog();
        dialog = new Dialog(currentActivity);
        dialog.setContentView(R.layout.connection_dialog);
        dialog.setTitle(getString(R.string.connection));
        dialog.setCanceledOnTouchOutside(false);

        String[] connections = new String[4];
        connections[0] = "Bluetooth";
        connections[1] = "Audio";
        connections[2] = "Serial";
        connections[3] = "USB";

        ListView listView = (ListView) dialog.findViewById(R.id.connectionList);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, connections));
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismissDialog();
                if (position == 0) {
                    if (checkBluetoothPermission()) {
                        Object[] pairedObjects = BluetoothAdapter.getDefaultAdapter().getBondedDevices().toArray();
                        final BluetoothDevice[] pairedDevices = new BluetoothDevice[pairedObjects.length];
                        for (int i = 0; i < pairedObjects.length; ++i) {
                            pairedDevices[i] = (BluetoothDevice) pairedObjects[i];
                        }

                        final ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(currentActivity, android.R.layout.simple_list_item_1);
                        for (int i = 0; i < pairedDevices.length; ++i) {
                            mArrayAdapter.add(pairedDevices[i].getName());
                        }

                        dismissDialog();

                        dialog = new Dialog(currentActivity);
                        dialog.setContentView(R.layout.bluetooth_2_device_list_dialog);
                        dialog.setTitle(R.string.bluetooth_devices);

                        ListView listView1 = (ListView) dialog.findViewById(R.id.pairedDeviceList);
                        listView1.setAdapter(mArrayAdapter);
                        listView1.setOnItemClickListener(new OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                statusEditText.setText(getString(R.string.connecting_bluetooth));
                                bbDeviceController.connectBT(pairedDevices[position]);
                                dismissDialog();
                            }

                        });

                        arrayAdapter = new ArrayAdapter<String>(currentActivity, android.R.layout.simple_list_item_1);
                        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
                        listView2.setAdapter(arrayAdapter);
                        listView2.setOnItemClickListener(new OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                statusEditText.setText(getString(R.string.connecting_bluetooth));
                                bbDeviceController.connectBT(foundDevices.get(position));
                                dismissDialog();
                            }

                        });

                        dialog.findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                bbDeviceController.stopBTScan();
                                dismissDialog();
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.show();

                        bbDeviceController.startBTScan(DEVICE_NAMES, 120);
                    } else {
                        setStatus(getString(R.string.bluetooth_permission_denied));
                        dismissDialog();
                    }
                } else if (position == 1) {
                    bbDeviceController.startAudio();
                } else if (position == 2) {
                    bbDeviceController.startSerial();
                } else if (position == 3) {
                    bbDeviceController.startUsb();
                }
            }

        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });

        dialog.show();
    }

    protected boolean checkBluetoothPermission() {
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static void setStatus(String message) {
        String tmp = message + "\n" + statusEditText.getText();
        int maxLength = 20000;
        if (tmp.length() >= maxLength) {
            int index = tmp.indexOf("\n", maxLength);
            if (index >= maxLength) {
                statusEditText.setText(tmp.substring(0, index));
                return;
            }
        }
        statusEditText.setText(tmp);
    }


    private void handleApduResult(boolean isSuccess, String apdu, int apduLength) {
        dismissDialog();

        try {
            if (isSuccess) {

                if (isApduEncrypted) {
                    String key;
                    if (keyMode.equals(DATA_KEY)) {
                        key = DUKPTServer.GetDataKey(ksn, "0123456789ABCDEFFEDCBA9876543210");
                    } else if (keyMode.equals(DATA_KEY_VAR)) {
                        key = DUKPTServer.GetDataKeyVar(ksn, "0123456789ABCDEFFEDCBA9876543210");
                    } else {
                        key = DUKPTServer.GetPinKeyVar(ksn, "0123456789ABCDEFFEDCBA9876543210");
                    }

                    if (encryptionMode.equals(CBC)) {
                        apdu = TripleDES.decrypt_CBC(apdu, key);
                    } else {
                        apdu = TripleDES.decrypt(apdu, key);
                    }

                    if (apduLength == 0) {
                        int padding = Integer.parseInt(apdu.substring(apdu.length() - 2));
                        apdu = apdu.substring(0, apdu.length() - padding * 2);
                    } else {
                        apdu = apdu.substring(0, apduLength * 2);
                    }
                }

                setStatus(getString(R.string.apdu_result) + apdu);

                if (apdu.startsWith("61") && apdu.length() == 4) {
                    sendApdu("00C00000" + apdu.substring(2));
                    return;
                }

                if (state == State.GETTING_PSE) {
                    if (apdu.endsWith("9000")) {
                        List<TLV> tlvList = TLVParser.parse(apdu.substring(0, apdu.length() - 4));
                        TLV tlv = TLVParser.searchTLV(tlvList, "88", state.toString());
                        if (tlv != null && tlv.value.equals("01")) {
                            state = State.READING_RECORD;
                            sendApdu("00B2010C00");
                            // setStatus("Reading record...");
                        }
                    } else if (apdu.equalsIgnoreCase("6A82")) {
                        aidCounter = 0;
                        state = State.READING_AID;
                        sendApdu("00A40400" + toHexString((byte) (aids[aidCounter].length() / 2)) + aids[aidCounter]);
                        // setStatus("Get PSE Failed.");
                        // setStatus("Trying to read AID " + aids[aidCounter] +
                        // "...");
                    }
                } else if (state == State.READING_RECORD) {
                    if (apdu.endsWith("9000")) {
                        List<TLV> tlvList = TLVParser.parse(apdu.substring(0, apdu.length() - 4));
                        TLV tlv = TLVParser.searchTLV(tlvList, "4F", state.toString());
                        if (tlv != null) {
                            state = State.READING_AID;
                            sendApdu("00A40400" + tlv.length + tlv.value);
                            // setStatus("Reading AID...");
                        }
                    }
                } else if (state == State.READING_AID) {
                    if (apdu.endsWith("9000")) {
                        List<TLV> tlvList = TLVParser.parse(apdu.substring(0, apdu.length() - 4));
                        TLV tlvMaster = TLVParser.searchTLV(tlvList, "50", "");
                        if (tlvMaster != null) {
                            String cardType = new String(hexToByteArray(tlvMaster.value));
                            textViewCardData.setText(textViewCardData.getText().toString() + "Card Type: " + cardType + "\n");
                        }

                        TLV tlv = TLVParser.searchTLV(tlvList, "9F38", state.toString());
                        state = State.GETTING_PROCESS_OPTION;
                        String command = "80A800000283";
                        if (tlv != null) {
                            int len = 0;
                            List<TLV> challenges = TLVParser.parseWithoutValue(tlv.value);
                            for (int i = 0; i < challenges.size(); ++i) {
                                len += Integer.parseInt(challenges.get(i).length);
                            }

                            command = "80A80000" + toHexString((byte) (len + 2)) + "83" + toHexString((byte) len);
                            for (int i = 0; i < len; ++i) {
                                command += "00";
                            }
                        } else {
                            command += "00";
                        }

                        sendApdu(command);
                        // setStatus("Getting Process Option...");
                    } else if (apdu.equalsIgnoreCase("6A82")) {
                        ++aidCounter;
                        if (aidCounter < aids.length) {
                            sendApdu("00A40400" + toHexString((byte) (aids[aidCounter].length() / 2)) + aids[aidCounter]);
                        } else {
                            setStatus(getString(R.string.no_aid_matched));
                        }
                        // setStatus("Read AID failed");
                        // setStatus("Trying to read AID " + aids[aidCounter] +
                        // "...");
                    }
                } else if (state == State.GETTING_PROCESS_OPTION) {
                    if (apdu.endsWith("9000")) {
                        List<TLV> tlvList = TLVParser.parse(apdu.substring(0, apdu.length() - 4));
                        TLV tlv = TLVParser.searchTLV(tlvList, "94", state.toString());
                        if (tlv != null) {
                            aflCounter = 0;
                            afls = new String[tlv.value.length() / 8];
                            for (int i = 0; i < afls.length; ++i) {
                                afls[i] = tlv.value.substring(i * 8, i * 8 + 8);
                            }
                            readingFileIndex = Integer.parseInt(afls[aflCounter].substring(2, 4), 16);
                            total = Integer.parseInt(afls[aflCounter].substring(4, 6), 16);
                            sfi = toHexString((byte) (((Integer.parseInt(afls[aflCounter].substring(0, 2), 16) & 0xF8) | 0x04)));

                            state = State.READING_DATA;

                            sendApdu("00B2" + toHexString((byte) readingFileIndex) + sfi + "00");

                            // setStatus("Reading record...");
                        } else if (apdu.startsWith("80")) {
                            afls = new String[(apdu.length() - 12) / 8];
                            for (int i = 0; i < afls.length; ++i) {
                                afls[i] = apdu.substring(i * 8 + 8, i * 8 + 16);
                            }

                            aflCounter = 0;
                            readingFileIndex = Integer.parseInt(afls[aflCounter].substring(2, 4), 16);
                            total = Integer.parseInt(afls[aflCounter].substring(4, 6), 16);
                            sfi = toHexString((byte) (((Integer.parseInt(afls[aflCounter].substring(0, 2), 16) & 0xF8) | 0x04)));

                            state = State.READING_DATA;

                            sendApdu("00B2" + toHexString((byte) readingFileIndex) + sfi + "00");
                            // setStatus("Reading record...");
                        }
                    }
                } else if (state == State.READING_DATA) {
                    if (apdu.endsWith("9000")) {
                        List<TLV> tlvList = TLVParser.parse(apdu.substring(0, apdu.length() - 4));
                        TLV tlv;
                        tlv = TLVParser.searchTLV(tlvList, "5F20", state.toString());
                        if (tlv != null) {
                            cardholderName = new String(hexToByteArray(tlv.value));
                            textViewCardData.setText(textViewCardData.getText().toString() + "Holder Name: " + cardholderName + "\n");
                        }

                        tlv = TLVParser.searchTLV(tlvList, "5F24", state.toString());
                        if (tlv != null) {
                            expiryDate = tlv.value;
                            textViewCardData.setText(textViewCardData.getText().toString() + "Expiry Date: " + expiryDate + "\n");
                        }

                        tlv = TLVParser.searchTLV(tlvList, "57", state.toString());
                        if (tlv != null) {
                            track2 = tlv.value;
                        }

                        tlv = TLVParser.searchTLV(tlvList, "5A", state.toString());
                        if (tlv != null) {
                            pan = tlv.value;
                            textViewCardData.setText(textViewCardData.getText().toString() + "Card Number: " + pan + "\n");
                        }


                        tlv = TLVParser.searchTLV(tlvList, "5f34", state.toString());
                        if (tlv != null) {
                            String security = tlv.value;
                            textViewCardData.setText(textViewCardData.getText().toString() + "security: " + security + "\n");
                        }


                        tlv = TLVParser.searchTLV(tlvList, "70", state.toString());
                        if (tlv != null) {
                            String cardSecurity = toHexString(tlv.value.getBytes());
                            textViewCardData.setText(textViewCardData.getText().toString() + "security: " + cardSecurity + "\n");
                        }

                        if (!cardholderName.equals("") && !expiryDate.equals("") && !track2.equals("") && !pan.equals("")) {
                            setStatus("");
                            setStatus("Cardholder Name: " + cardholderName);
                            setStatus("Expire Date: " + expiryDate);
                            setStatus("Track 2: " + track2);
                            setStatus("PAN: " + pan);
                            if (startTime != 0) {
                                setStatus((System.currentTimeMillis() - startTime) + "ms");
                                startTime = 0;
                            }
                            bbDeviceController.bypassPinEntry();
                            return;
                        }

                        ++readingFileIndex;
                        if (readingFileIndex <= total) {
                            sendApdu("00B2" + toHexString((byte) readingFileIndex) + sfi + "00");
                        } else if (aflCounter < afls.length - 1) {
                            ++aflCounter;
                            readingFileIndex = Integer.parseInt(afls[aflCounter].substring(2, 4), 16);
                            total = Integer.parseInt(afls[aflCounter].substring(4, 6), 16);
                            sfi = toHexString((byte) (((Integer.parseInt(afls[aflCounter].substring(0, 2), 16) & 0xF8) | 0x04)));

                            state = State.READING_DATA;

                            sendApdu("00B2" + toHexString((byte) readingFileIndex) + sfi + "00");
                            // setStatus("Reading record...");
                        }
                    }
                }
                /*
                 * ++count; if(count < apduCommands.length) {
                 *
                 * //setStatus(getString(R.string.sending) + apduCommands[count]); //emvSwipeController.sendApdu(apduCommands[count], apduCommands[count].length() / 2);
                 *
                 * setStatus(getString(R.string.sending) + apduCommands[count]);
                 *
                 * String command = apduCommands[count]; while((command.length() / 2) % 8 != 0) { command = command + "00"; } String encryptedCommand = TripleDES.encrypt_CBC(command, key); emvSwipeController.sendApdu(encryptedCommand, apduCommands[count].length() / 2); }
                 */
            } else {
                setStatus(getString(R.string.apdu_failed));
            }
        } catch (Exception e) {
            setStatus(e.getMessage());
            StackTraceElement[] elements = e.getStackTrace();
            for (int i = 0; i < elements.length; ++i) {
                setStatus(elements[i].toString());
            }
        }
    }

    protected void sendApdu(String command) {
        try {
            if (isApduEncrypted) {
                String key;
                if (keyMode.equals(DATA_KEY)) {
                    key = DUKPTServer.GetDataKey(ksn, "0123456789ABCDEFFEDCBA9876543210");
                } else if (keyMode.equals(DATA_KEY_VAR)) {
                    key = DUKPTServer.GetDataKeyVar(ksn, "0123456789ABCDEFFEDCBA9876543210");
                } else {
                    key = DUKPTServer.GetPinKeyVar(ksn, "0123456789ABCDEFFEDCBA9876543210");
                }

                String temp = command;
                if (isPKCS7) {
                    int padding = 8 - (temp.length() / 2) % 8;
                    for (int i = 0; i < padding; ++i) {
                        temp += "0" + padding;
                    }
                } else {
                    while ((temp.length() / 2) % 8 != 0) {
                        temp += "00";
                    }
                }

                String encryptedCommand;

                if (encryptionMode.equals(CBC)) {
                    encryptedCommand = TripleDES.encrypt_CBC(temp, key);
                } else {
                    encryptedCommand = TripleDES.encrypt(temp, key);
                }

                Hashtable<String, Object> apduInput = new Hashtable<String, Object>();
                apduInput.put("apdu", encryptedCommand);
                if (isPKCS7) {
                    bbDeviceController.sendApdu(apduInput);
                } else {
                    apduInput.put("apduLength", command.length() / 2);
                    bbDeviceController.sendApdu(apduInput);
                }

                setStatus(getString(R.string.sending) + command);
            } else {
                Hashtable<String, Object> apduInput = new Hashtable<String, Object>();
                apduInput.put("apdu", command);
                apduInput.put("apduLength", command.length() / 2);
                bbDeviceController.sendApdu(apduInput);
            }
        } catch (Exception e) {
            setStatus(e.getMessage());
            StackTraceElement[] elements = e.getStackTrace();
            for (int i = 0; i < elements.length; ++i) {
                setStatus(elements[i].toString());
            }
        }
    }

    private static String toHexString(byte[] b) {
        if (b == null) {
            return "null";
        }
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xFF) + 0x100, 16).substring(1);
        }
        return result;
    }

    private static byte[] hexToByteArray(String s) {
        if (s == null) {
            s = "";
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (int i = 0; i < s.length() - 1; i += 2) {
            String data = s.substring(i, i + 2);
            bout.write(Integer.parseInt(data, 16));
        }
        return bout.toByteArray();
    }

    protected static String toHexString(byte b) {
        return Integer.toString((b & 0xFF) + 0x100, 16).substring(1);
    }


}
