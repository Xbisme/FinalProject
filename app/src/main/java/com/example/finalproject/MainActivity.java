package com.example.finalproject;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;




import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

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

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    BluetoothGattCharacteristic characteristic;

    private boolean mConnected = false;

    private boolean isClick = false;

    private boolean isConnectedFirebase = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;


    private String mDeviceAddress;


    BluetoothLeService mBluetoothLeService;
    String BLE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String BLE_UUID1 = "0000ffe1-0000-1000-8000-00805f9b34fb";


    TextView time,hideShowToken, temperature, humidity;

    EditText organizationId, deviceId, authToken;

    AppCompatButton connected, disconnected, connected1;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    divideText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }


    };

    private void divideText(String stringExtra) {
        String value = "";
        for (int i = 0; i < stringExtra.length();i++) {
            value += stringExtra.charAt(i);
            if(stringExtra.charAt(i) == '\n') {
                break;
            }
        }
        String[] parts = value.split("; ");
        String[] tempArray = parts[0].split(": ");
        float temperatureFloat = Float.parseFloat(tempArray[1]);
        String[] humArray = parts[1].split(": ");
        float humidityFloat = Float.parseFloat(humArray[1]);
        String tempString = temperatureFloat + "°C";
        String humiString = humidityFloat+ "%";
        temperature.setText(tempString);
        humidity.setText(humiString);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().show();
        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        findViewById(R.id.main_layout).setOnClickListener(view -> {
            organizationId.clearFocus();
            deviceId.clearFocus();
            authToken.clearFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });

        time = findViewById(R.id.currentTime);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        hideShowToken = findViewById(R.id.show_hide_token);
        temperature.setText(getIntent().getStringExtra(BluetoothLeService.EXTRA_DATA));
        organizationId = findViewById(R.id.organizationID_editText);
        deviceId = findViewById(R.id.deviceID_editText);
        authToken = findViewById(R.id.auth_editText);

        connected = findViewById(R.id.connected_btn);
        connected1 = findViewById(R.id.connected1_btn);
        disconnected = findViewById(R.id.disconnected_btn);



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

        connected.setOnClickListener(v -> {
            String organizationText = String.valueOf(organizationId.getText());
            String deviceText = String.valueOf(deviceId.getText());
            String authText = String.valueOf(authToken.getText());
            String temperatureText = temperature.getText().toString();
            String humidityText = humidity.getText().toString();
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
            if(!isConnectedFirebase) {
                isConnectedFirebase = true;
                connected.setText(R.string.disconnected);
                updateDatatoFirebase(organizationText,deviceText,authText,temperatureText,humidityText);

            }
            else {
                isConnectedFirebase = false;
                connected.setText(R.string.connected);
                organizationId.setText("");
                deviceId.setText("");
                authToken.setText("");
            }

        });
        Handler updateHandler = new Handler();

        Runnable updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if(mConnected == true && isClick == true) {
                        getValue();
                    }
                    if(isConnectedFirebase == true) {
                        String organizationText = String.valueOf(organizationId.getText());
                        String deviceText = String.valueOf(deviceId.getText());
                        String authText = String.valueOf(authToken.getText());
                        String temperatureText = temperature.getText().toString();
                        String humidityText = humidity.getText().toString();
                        updateDatatoFirebase(organizationText,deviceText,authText,temperatureText,humidityText);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error in updateDataRunnable: " + e.getMessage());
                } finally {
                    updateHandler.postDelayed(this, 1000);
                }
            }

        };

        updateHandler.postDelayed(updateDataRunnable, 1000);




        connected1.setOnClickListener(v -> {
            if(!mConnected) {
                mBluetoothLeService.connect(mDeviceAddress);
            }
            isClick = true;
            getValue();
        });
        disconnected.setOnClickListener(v -> {

                temperature.setText("");
                humidity.setText("");
                mBluetoothLeService.disconnect();
                Log.d(TAG, "Disconnected from the Bluetooth device");
                isClick = false;
                mConnected = false;
            });
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void updateDatatoFirebase(String organizationText, String deviceText, String authText, String temperatureText, String humidityText) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(organizationText);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteData writeData = new ReadWriteData(organizationText,deviceText,authText,temperatureText,humidityText);

                if(writeData != null){
                    databaseReference.setValue(writeData);
                } else {
                    Toast.makeText(getBaseContext(),"Something is wrong, please check your input!",Toast.LENGTH_SHORT).show();
                    Log.e("Failed binding", "Failed binding");
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void getValue() {
        String uuid;
        for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
            uuid = gattService.getUuid().toString();
            if(uuid.equals(BLE_UUID)) {

                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if(uuid.equals(BLE_UUID1)) {
                        characteristic = gattCharacteristic;
                    }
                }
                final int charPro = characteristic.getProperties();
                if((charPro | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic,false);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.readCharacteristic(characteristic);
                    if((charPro | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}

