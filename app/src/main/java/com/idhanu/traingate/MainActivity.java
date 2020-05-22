package com.idhanu.traingate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 23;
    private Button buttonConnect;
    private TextView textViewStatus;
    private ImageView imageViewGate;
    private boolean isGateOpen = true;
    BluetoothAdapter bluetoothAdapter;
    MyBleManager manager;
    MediaPlayer mp;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        buttonConnect = findViewById(R.id.button_connect);
        textViewStatus = findViewById(R.id.textViewStatus);
        imageViewGate = findViewById(R.id.imageViewGate);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the MainActivity.this
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        3);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }



        buttonConnect.setOnClickListener((View view) -> {
            Thread thread = new Thread(() -> {
                if (((Button) view).getText().equals("Connect")) {
                    connect();
                } else {
                    disconnect();
                }
            });

            thread.start();
        });

        imageViewGate.setOnClickListener((View view) -> {
            if(manager == null || !manager.isConnected()) {
                setStatusText("Not connected!");
                return;
            }
            isGateOpen = !isGateOpen;

            if (isGateOpen) {
                stopSound();
            } else {
                startSound();
            }

            runOnUiThread(() -> {
                imageViewGate.setImageResource(isGateOpen ? R.drawable.gate_closed : R.drawable.gate_open);
            });
            manager.write(isGateOpen ? "O" : "C");
            setStatusText(isGateOpen ? "Opening" : "Closing");
        });
    }

    private void connect() {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("00:15:80:91:55:72");
        final String name = device.getName();

        if (manager == null) {
            manager = new MyBleManager(MainActivity.this.getBaseContext());
        }

        if (manager.isConnected()) {
            setStatusText("Already connected!");
            return;
        }


        setStatusText("Connecting to " + name + " ...");
        manager.connect(device)
                .timeout(100000)
                .retry(3, 100)
                .enqueue();
        setConnectButtonText("Disconnect");
        setStatusText("Ready");
    }

    private void disconnect() {
        setStatusText("Disconnecting...");
        if (manager != null) {
            manager.close();
            manager = null;
        }
        setStatusText("");
        setConnectButtonText("Connect");
    }

    private void setConnectButtonText(String text) {
        MainActivity.this.runOnUiThread(() -> buttonConnect.setText(text));
    }

    private void setStatusText(String text) {
        MainActivity.this.runOnUiThread(() -> textViewStatus.setText(text));
    }

    private void startSound() {
        if (mp != null && mp.isPlaying()) {
            stopSound();
        }

        mp = MediaPlayer.create(this, R.raw.bell);
        mp.setLooping(true);
        mp.start();
    }

    private void stopSound() {
        if (mp != null && mp.isPlaying()) {
            mp.stop();
            mp.release();
        }

        mp = null;
    }

    @Override
    protected void onDestroy() {
        disconnect();
        stopSound();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        disconnect();
        stopSound();
        super.onPause();
    }

    @Override
    protected void onResume() {
        connect();
        super.onResume();
    }
}
