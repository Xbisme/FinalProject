package com.example.finalproject;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;
import io.reactivex.*;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IOTLogs";
    public static Handler handler;
    private final static int ERROR_READ = 0;

    BluetoothDevice BLEModule = null;
    UUID BLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    TextView time,hideShowToken, temperature, humidity;

    EditText organizationId, deviceId, authToken;

    AppCompatButton connected, disconnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        time = findViewById(R.id.currentTime);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        hideShowToken = findViewById(R.id.show_hide_token);

        organizationId = findViewById(R.id.organizationID_editText);
        deviceId = findViewById(R.id.deviceID_editText);
        authToken = findViewById(R.id.auth_editText);

        connected = findViewById(R.id.connected_btn);
        disconnected = findViewById(R.id.disconnected_btn);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    case ERROR_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from BLE
                        Log.e("Error in BLE System", arduinoMsg);
                        break;
                }
            }
        };

        final Observable<String> connectToBTObservable = Observable.create(emitter -> {
            Log.d(TAG, "Calling connectThread class");
            ConnectThread connectThread = new ConnectThread(BLEModule, BLE_UUID, handler);
            connectThread.run();

            if (connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                ConnectedThread connectedThread = new ConnectedThread(connectThread.getMmSocket());
                connectedThread.run();
                if (connectedThread.getValueRead() != null) {
                    emitter.onNext(connectedThread.getValueRead());
                }
                connectedThread.cancel();
            }
            connectThread.cancel();
            emitter.onComplete();
        });

        DateFormat df = new SimpleDateFormat("h:mm a");
        String currentTime = df.format(Calendar.getInstance().getTime());
        time.setText(currentTime.toString());

        hideShowToken.setOnClickListener(v -> {
            try {
                String authData = authToken.getText().toString();
                Log.i("authData", authData);
                if (authToken.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())) {
                    authToken.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    hideShowToken.setText("Show Auth Token");
                } else {
                    authToken.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    hideShowToken.setText("Hide Auth Token");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Error", "An error occurred: " + e.getMessage());
            }
        });

        connected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String btDevicesString="";

                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {

                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        Log.d(TAG, "deviceName:" + deviceName);
                        Log.d(TAG, "deviceHardwareAddress:" + deviceHardwareAddress);
                        //btDevicesString=btDevicesString+deviceName+" || "+deviceHardwareAddress+"\n";
                        if (deviceName.equals("T800ProMax")) {
                            Log.d(TAG, " found");
                            BLE_UUID = device.getUuids()[0].getUuid();
                            BLEModule = device;
                            Log.d(TAG, "Button Pressed");
                            Log.i("Device Name", BLEModule.toString());
                            changedValue(device);
                            break;
                        }

                    }
                }
            }

            private void changedValue(BluetoothDevice device) {
                if (device != null) {
                    connectToBTObservable.
                            observeOn(AndroidSchedulers.mainThread()).
                            subscribeOn(Schedulers.io()).
                            subscribe(valueRead -> {
                                temperature.setText("1");
                                humidity.setText("2");
                            });
                }
            }
        });

        disconnected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BLEModule = null;
                BLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                temperature.setText("");
                humidity.setText("");

                Log.d(TAG, "Disconnected from the Bluetooth device");
            }

        });
    }
}