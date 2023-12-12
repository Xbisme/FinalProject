package com.example.finalproject;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalproject.Data.ReadWriteData;
import com.example.finalproject.Service.BluetoothLeService;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    BluetoothLeService mBluetoothLeService;
    boolean mBound = false;
    UUID BLE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");


    TextView time,hideShowToken, temperature, humidity;

    EditText organizationId, deviceId, authToken;

    AppCompatButton connected, disconnected;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            mBluetoothLeService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.main_layout).setOnClickListener(view -> {
            organizationId.clearFocus();
            deviceId.clearFocus();
            authToken.clearFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        boolean check = bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (check) Log.i("oke", "oke");
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
            connectThread.start();

            if (connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                ConnectedThread connectedThread = new ConnectedThread(connectThread.getMmSocket());
                connectedThread.start();
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
        time.setText(currentTime);

        hideShowToken.setOnClickListener(v -> {
            try {
                String authData = authToken.getText().toString();
                Log.i("authData", authData);
                if (authToken.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())) {
                    authToken.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    hideShowToken.setText(R.string.show);
                } else {
                    authToken.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    hideShowToken.setText(R.string.hide);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Error", "An error occurred: " + e.getMessage());
            }
        });

        connected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String organizationText = String.valueOf(organizationId.getText());
                String deviceText = String.valueOf(deviceId.getText());
                String authText = String.valueOf(authToken.getText());
                if(TextUtils.isEmpty(organizationText)) {
                    organizationId.setError("Please enter your Organization Id");
                    organizationId.requestFocus();
                    return;
                }
                else if(TextUtils.isEmpty(deviceText)) {
                    deviceId.setError("Please enter your Device Id");
                    deviceId.requestFocus();
                    return;
                }else if(TextUtils.isEmpty(authText)) {
                    authToken.setError("Please enter your Auth Token");
                    authToken.requestFocus();
                    return;
                }
                if(connected.getText().equals("Connected")) {

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ReadWriteData writeData = snapshot.getValue(ReadWriteData.class);

                            if(writeData != null){
                                String organization = writeData.getOrganizationID();
                                String device = writeData.getDeviceID();
                                String auth = writeData.getAuthToken();
                                if (TextUtils.equals(organization, organizationText)
                                        && TextUtils.equals(device, deviceText)
                                        && TextUtils.equals(auth, authText)) {
                                    Toast.makeText(getBaseContext(),"Connect Success",Toast.LENGTH_SHORT).show();
                                    changeUi(writeData);
                                } else {
                                    Toast.makeText(getBaseContext(),"Something is wrong, please check your input!",Toast.LENGTH_SHORT).show();
                                    Log.e("Failed binding", "Failed binding");
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
                else {
                    connected.setText(R.string.connected);
                    organizationId.setText("");
                    deviceId.setText("");
                    authToken.setText("");
                }
            }
            private void changeUi(ReadWriteData writeData) {
                String temp = writeData.getTemperature();
                String humi = writeData.getHumidity();
                temperature.setText(temp);
                humidity.setText(humi);
                connected.setText(R.string.disconnected);
            }



        });
        Handler updateHandler = new Handler();

        Runnable updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    updateDataFromFirebase();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error in updateDataRunnable: " + e.getMessage());
                } finally {
                    updateHandler.postDelayed(this, 2000);
                }
            }

            private void updateDataFromFirebase() {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            ReadWriteData writeData = snapshot.getValue(ReadWriteData.class);

                            if (writeData != null) {
                                if (connected.getText().equals("Disconnected")) {
                                    String temperatureValue = snapshot.child("temperature").getValue(String.class);
                                    String humidityValue = snapshot.child("humidity").getValue(String.class);
                                    temperature.setText(temperatureValue);
                                    humidity.setText(humidityValue);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error in onDataChange: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error in onCancelled: " + error.getMessage());
                    }
                });
            }
        };

        updateHandler.postDelayed(updateDataRunnable, 2000);




        disconnected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (disconnected.getText().equals("Disconnected")) {
                    BLEModule = null;
                    BLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                    temperature.setText("");
                    humidity.setText("");

                    disconnected.setText(R.string.connected);
                    Log.d(TAG, "Disconnected from the Bluetooth device");
                }
                else {
                    if (!bluetoothAdapter.isEnabled()) {
                        // Mở Bluetooth
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 1);
                    }
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
                                BLEModule = device;
                                Log.d(TAG, "Button Pressed");
                                Log.i("Device Name", BLEModule.toString());
                                disconnected.setText(R.string.disconnected);
                                changedValue(device);
                            }

                        }
                    }

                }
            }
            private void changedValue(BluetoothDevice device) {

                if (device != null) {
                    Log.i("Success","Connected Success");
                    connectToBTObservable.
                            observeOn(AndroidSchedulers.mainThread()).
                            subscribeOn(Schedulers.io()).
                            subscribe(valueRead -> {
                                temperature.setText("1");
                                humidity.setText("2");
                            });
                }
                else {
                    Log.e("Failed","Connected Failed");
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }
}