package com.tinytinybites.android.pvzquiz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import com.tinytinybites.android.pvzquiz.R;
import com.tinytinybites.android.pvzquiz.event.StartNewGameEvent;
import com.tinytinybites.android.pvzquiz.session.GameSession;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
public class DashboardActivity extends AppCompatActivity
{
    //Tag
    private static final String TAG = DashboardActivity.class.getName();
    private static final String BT = "HC-06";
    private static final String MODULE_MAC = "20:17:05:08:31:62";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static BluetoothSocket tmp;
    //Variables
    private EventBus mEventBus;

    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    public static ConnectedThread btt = null;
    Button switchLight, switchRelay;
    TextView response;
    boolean lightflag = false;
    boolean relayFlag = true;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        mEventBus = EventBus.getDefault();
        mEventBus.register(this);

        Log.i("[BLUETOOTH]", "Creating listeners");
        response = (TextView) findViewById(R.id.response);
        switchRelay = (Button) findViewById(R.id.relay);
        switchLight = (Button) findViewById(R.id.switchlight);
//        switchLight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i("[BLUETOOTH]", "Attempting to send data");
//                try {
//                    if (mmDevice != null) {
//                        Log.i(TAG, "Device Name: " + mmDevice.getName());
//                        Log.i(TAG, "Device UUID: " + mmDevice.getUuids()[0].getUuid());
//                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
//
//                    } else Log.d(TAG, "Device is null.");
//                }
//            catch (NullPointerException e){
//                Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + mmDevice.getName());
//                try {
//                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//            catch (IOException e) { }
//
//
//        if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
//            if (!lightflag) {
//                String sendtxt = "LY";
//                btt.write(sendtxt.getBytes());
//                lightflag = true;
//            } else {
//                String sendtxt = "LN";
//                btt.write(sendtxt.getBytes());
//                lightflag = false;
//            }
//        } else {
//
//            Log.i("[BLUETOOTH]", "Something went wrong");
////            Toast.makeText(Ardcon.this, "Something went wrong", Toast.LENGTH_LONG).show();
//        }
//            }
//        });

//        switchRelay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i("[BLUETOOTH]", "Attempting to send data");
//                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
//                    if(relayFlag){
//                        String sendtxt = "RY";
//                        btt.write(sendtxt.getBytes());
//                        relayFlag = false;
//                    }else{
//                        String sendtxt = "RN";
//                        btt.write(sendtxt.getBytes());
//                        relayFlag = true;
//                    }
//
//                    //disable the button and wait for 4 seconds to enable it again
//                    switchRelay.setEnabled(false);
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try{
//                                Thread.sleep(4000);
//                            }catch(InterruptedException e){
//                                return;
//                            }
//
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    switchRelay.setEnabled(true);
//                                }
//                            });
//
//                        }
//                    }).start();
//                } else {
//                    Log.i("[BLUETOOTH]", "Something went wrong");
////                    Toast.makeText(Ardcon.this, "Something went wrong", Toast.LENGTH_LONG).show();
//                }
//            }
//        });

        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if(!bta.isEnabled()){
            Log.i(TAG,"BT is not enabled");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }else{
            Log.i(TAG,"BT is enabled");
            initiateBluetoothProcess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Subscribe
    public void OnStartNewGameClicked(StartNewGameEvent event) {
        GameSession.getInstance().resetSession();
        startActivity(new Intent(this, QuizActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initiateBluetoothProcess();
        }
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()) {
            Log.i(TAG,"Initiate BT Process");
            //attempt to connect to bluetooth module
            BluetoothSocket tmp = null;
            mmDevice = bta.getRemoteDevice(MODULE_MAC);

            //create socket
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();
                Log.i("[BLUETOOTH]", "Connected to: " + mmDevice.getName());
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException c) {
                    return;
                }
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    if (msg.what == ConnectedThread.RESPONSE_MESSAGE) {
                        String txt = (String) msg.obj;
                        if (response.getText().toString().length() >= 30) {
                            response.setText("");
                            response.append(txt);
                        } else {
                            response.append("\n" + txt);
                        }
                    }
                }
            };

            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket, mHandler);
            btt.start();

        }
        }
}
