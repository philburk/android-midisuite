/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobileer.example.midibtlepairing;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiManager.DeviceCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * App that creates a Device for a BLE-MIDI peripheral.
 * The peripheral can then be used by other apps.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MidiBtlePairing";

    private MidiManager mMidiManager;
    private OpenDeviceListAdapter mOpenDeviceListAdapter;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 179384;
    private static final int REQUEST_BLUETOOTH_SCAN = 1;
    private String[] mPermissions;

    // Keep track of one BLE-MIDI device.
    static class BluetoothMidiDeviceTracker {
        final public BluetoothDevice bluetoothDevice;
        final public MidiDevice midiDevice;
        public int inputOpenCount;
        public int outputOpenCount;

        public BluetoothMidiDeviceTracker(BluetoothDevice bluetoothDevice,
                MidiDevice midiDevice) {
            this.bluetoothDevice = bluetoothDevice;
            this.midiDevice = midiDevice;
        }

        @Override
        public int hashCode() {
            return midiDevice.getInfo().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BluetoothMidiDeviceTracker) {
                BluetoothMidiDeviceTracker other = (BluetoothMidiDeviceTracker) o;
                return midiDevice.getInfo().equals(other.midiDevice.getInfo());
            } else {
                return false;
            }
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceStatus;
        Button closeButton;
    }

    // Adapter for holding open devices.
    private class OpenDeviceListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothMidiDeviceTracker> mOpenDevices;
        private final LayoutInflater mInflater;
        private final HashMap<MidiDeviceInfo, BluetoothMidiDeviceTracker> mInfoTrackerMap =
                new HashMap<MidiDeviceInfo, BluetoothMidiDeviceTracker>();

        public OpenDeviceListAdapter() {
            super();
            mOpenDevices = new ArrayList<BluetoothMidiDeviceTracker>();
            mInflater = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothMidiDeviceTracker deviceTracker) {
            if (!mOpenDevices.contains(deviceTracker)) {
                mOpenDevices.add(deviceTracker);
                MidiDeviceInfo info = deviceTracker.midiDevice.getInfo();
                mInfoTrackerMap.put(info, deviceTracker);
                notifyDataSetChanged();
            }
        }

        public void remove(MidiDeviceInfo info) {
            BluetoothMidiDeviceTracker deviceTracker = mInfoTrackerMap.get(info);
            if (deviceTracker != null) {
                mOpenDevices.remove(deviceTracker);
                mInfoTrackerMap.remove(info);
                notifyDataSetChanged();
            }
        }

        public BluetoothMidiDeviceTracker getDevice(int position) {
            if (position >= 0 && position < mOpenDevices.size()) {
                return mOpenDevices.get(position);
            }
            return null;
        }

        public BluetoothMidiDeviceTracker getDevice(MidiDeviceInfo info) {
            return mInfoTrackerMap.get(info);
        }

        public void clear() {
            mOpenDevices.clear();
            mInfoTrackerMap.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mOpenDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mOpenDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_open_device, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceStatus = view.findViewById(R.id.device_status);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.closeButton = view.findViewById(R.id.close_button);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final BluetoothMidiDeviceTracker deviceTracker = mOpenDevices.get(i);
            viewHolder.closeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    doClose(deviceTracker.midiDevice);
                    v.setEnabled(false);
                }
            });

            String deviceName = null;
            try {
                deviceName = deviceTracker.bluetoothDevice.getName();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException getting device name", e);
            }

            if (deviceName != null && !deviceName.trim().isEmpty()) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }

            // Show address and number of ports open.
            StringBuilder sb = new StringBuilder();
            sb.append(deviceTracker.bluetoothDevice.getAddress());
            sb.append(", [In: ").append(deviceTracker.inputOpenCount);
            sb.append("] [Out: ").append(deviceTracker.outputOpenCount);
            sb.append("]");

            if ((deviceTracker.inputOpenCount + deviceTracker.outputOpenCount) > 0) {
                sb.append(" (in use)");
            }
            viewHolder.deviceStatus.setText(sb.toString());

            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i(TAG, "MidiBtlePairing MainActivity started");
        Button bluetoothScanButton = findViewById(R.id.bluetooth_scan);
        bluetoothScanButton.setOnClickListener(mBluetoothScanListener);

        ListView listView = findViewById(R.id.open_device_list);
        listView.setEmptyView(findViewById(R.id.empty));

        // Initializes list view adapter.
        mOpenDeviceListAdapter = new OpenDeviceListAdapter();
        listView.setAdapter(mOpenDeviceListAdapter);

        // Build list of required permissions based on SDK Version.
        ArrayList<String> permissions = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        mPermissions = permissions.toArray(new String[0]);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(MainActivity.this, "MIDI not supported!",
                    Toast.LENGTH_LONG).show();
            bluetoothScanButton.setEnabled(false);
        }
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) return;

        mMidiManager.registerDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceRemoved(final MidiDeviceInfo info) {
                mOpenDeviceListAdapter.remove(info);
            }

            // Update port open counts so user knows if the device is in use.
            @Override
            public void onDeviceStatusChanged(final MidiDeviceStatus status) {
                MidiDeviceInfo info = status.getDeviceInfo();
                BluetoothMidiDeviceTracker tracker = mOpenDeviceListAdapter.getDevice(info);
                if (tracker != null) {
                    tracker.outputOpenCount = 0;
                    for (int i = 0; i < info.getOutputPortCount(); i++) {
                        tracker.outputOpenCount += status.getOutputPortOpenCount(i);
                    }
                    tracker.inputOpenCount = 0;
                    for (int i = 0; i < info.getInputPortCount(); i++) {
                        tracker.inputOpenCount += status.isInputPortOpen(i) ? 1 : 0;
                    }
                    mOpenDeviceListAdapter.notifyDataSetChanged();
                }
            }
        }, new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onDestroy() {
        closeAll();
        super.onDestroy();
    }

    private void onBluetoothDeviceOpen(final BluetoothDevice bluetoothDevice,
            final MidiDevice midiDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (midiDevice != null) {
                    BluetoothMidiDeviceTracker tracker = new BluetoothMidiDeviceTracker(
                            bluetoothDevice, midiDevice);
                    mOpenDeviceListAdapter.addDevice(tracker);
                } else {
                    Toast.makeText(MainActivity.this,
                            "MIDI device open failed!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
    }

    public void closeAll() {
        for (int i = 0; i < mOpenDeviceListAdapter.getCount(); i++) {
            BluetoothMidiDeviceTracker tracker = mOpenDeviceListAdapter.getDevice(i);
            if (tracker != null) {
                Log.i(TAG, "Closing Bluetooth MIDI device: " + tracker.bluetoothDevice.getAddress());
                doClose(tracker.midiDevice);
            }
        }
        mOpenDeviceListAdapter.clear();
    }

    private boolean hasPermissions() {
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private final OnClickListener mBluetoothScanListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (hasPermissions()) {
                openBluetoothScan();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, mPermissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        }
    };

    private void openBluetoothScan() {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(intent, REQUEST_BLUETOOTH_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_ID_MULTIPLE_PERMISSIONS) {
            return;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                                "Permission not granted.", Toast.LENGTH_LONG)
                        .show();
                return;
            }
        }
        openBluetoothScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_SCAN && resultCode == RESULT_OK && data != null) {
            final BluetoothDevice fBluetoothDevice;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fBluetoothDevice = data.getParcelableExtra("device", BluetoothDevice.class);
            } else {
                fBluetoothDevice = (BluetoothDevice) data.getParcelableExtra("device");
            }

            if (fBluetoothDevice != null && mMidiManager != null) {
                Log.i(TAG, "Opening Bluetooth MIDI device: " + fBluetoothDevice.getAddress());
                mMidiManager.openBluetoothDevice(fBluetoothDevice,
                        new MidiManager.OnDeviceOpenedListener() {
                            @Override
                            public void onDeviceOpened(MidiDevice device) {
                                onBluetoothDeviceOpen(fBluetoothDevice, device);
                            }
                        }, null);
            }
        }
    }

    private void doClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing MidiDevice", e);
            }
        }
    }
}
