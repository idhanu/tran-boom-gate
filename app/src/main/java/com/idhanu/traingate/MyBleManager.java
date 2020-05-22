package com.idhanu.traingate;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;

import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;

class MyBleManager extends BleManager {
    final static UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    final static UUID CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // Client characteristics
    private BluetoothGattCharacteristic characteristic;

    MyBleManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new MyManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class MyManagerGattCallback extends BleManagerGattCallback {

        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                characteristic = service.getCharacteristic(CHARACTERISTIC);
            }

            // Return true if all required services have been found
            return characteristic != null;
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        @Override
        protected void initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                    .add(enableNotifications(characteristic))
                    .done(device -> log(Log.INFO, "Target initialized"))
                    .enqueue();

            characteristic.setWriteType(WRITE_TYPE_DEFAULT);
        }

        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            characteristic = null;
        }
    }

    public void write(String text) {
        writeCharacteristic(characteristic, text.getBytes())
                .done(device -> log(Log.INFO, "Greetings sent"))
                .enqueue();
    }

    /**
     * Aborts time travel. Call during 3 sec after enabling Flux Capacitor and only if you don't
     * like 2020.
     */
    public void abort() {
        cancelQueue();
    }
}