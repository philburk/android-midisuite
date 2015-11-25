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

package com.android.example.midibtlepairing;

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
import android.os.Bundle;
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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * App that provides a MIDI echo service.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MidiBtlePairing";
    private static final String PACKAGE_NAME = "com.android.example.midibtlepairing";

    private MidiManager mMidiManager;
    private OpenDeviceListAdapter mOpenDeviceListAdapter;
    private static final int REQUEST_BLUETOOTH_SCAN = 1;

    static class BluetoothMidiDeviceTracker {
        final public BluetoothDevice bluetoothDevice;
        final public MidiDevice midiDevice;
        public int inputOpenCount;
        public int outputOpenCount;

        /**
         * @param bluetoothDevice
         * @param midiDevice
         */
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
        private ArrayList<BluetoothMidiDeviceTracker> mOpenDevices;
        private LayoutInflater mInflator;
        private HashMap<MidiDeviceInfo, BluetoothMidiDeviceTracker> mInfoTrackerMap = new HashMap<MidiDeviceInfo, BluetoothMidiDeviceTracker>();

        public OpenDeviceListAdapter() {
            super();
            mOpenDevices = new ArrayList<BluetoothMidiDeviceTracker>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothMidiDeviceTracker deviceTracker) {
            if (!mOpenDevices.contains(deviceTracker)) {
                mOpenDevices.add(deviceTracker);
                MidiDeviceInfo info = deviceTracker.midiDevice.getInfo();
                mInfoTrackerMap.put(info, deviceTracker);
                notifyDataSetChanged();
            }
        }

        /**
         * @param info
         */
        public void remove(MidiDeviceInfo info) {
            BluetoothMidiDeviceTracker deviceTracker = mInfoTrackerMap
                    .get(info);
            if (deviceTracker != null) {
                mOpenDevices.remove(deviceTracker);
                notifyDataSetChanged();
            }
        }

        public BluetoothMidiDeviceTracker getDevice(int position) {
            return mOpenDevices.get(position);
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
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_open_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceStatus = (TextView) view
                        .findViewById(R.id.device_status);
                viewHolder.deviceName = (TextView) view
                        .findViewById(R.id.device_name);
                viewHolder.closeButton = (Button) view
                        .findViewById(R.id.close_button);
                viewHolder.closeButton
                        .setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                BluetoothMidiDeviceTracker deviceTracker = mOpenDevices
                                        .get(i);
                                doClose(deviceTracker.midiDevice);
                                ((Button) v).setEnabled(false);
                            }
                        });
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothMidiDeviceTracker deviceTracker = mOpenDevices.get(i);
            final String deviceName = deviceTracker.bluetoothDevice.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText("--");
            }

            // Show address and number of ports open.
            StringBuilder sb = new StringBuilder();
            sb.append(" - ");
            sb.append(deviceTracker.bluetoothDevice.getAddress());
            sb.append(", [" + deviceTracker.inputOpenCount);
            sb.append("][" + deviceTracker.outputOpenCount);
            sb.append("]");

            if ((deviceTracker.inputOpenCount + deviceTracker.outputOpenCount) > 0) {
                sb.append(" in use");
            }
            viewHolder.deviceStatus.setText(sb.toString());

            return view;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i(TAG, "app started ========================");
        Button bluetoothScanButton = (Button) findViewById(R.id.bluetooth_scan);
        bluetoothScanButton.setOnClickListener(mBluetoothScanListener);

        ListView listView = (ListView) findViewById(R.id.open_device_list);
        listView.setEmptyView(findViewById(R.id.empty));

        // Initializes list view adapter.
        mOpenDeviceListAdapter = new OpenDeviceListAdapter();
        listView.setAdapter(mOpenDeviceListAdapter);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(MainActivity.this, "MIDI not supported!",
                    Toast.LENGTH_LONG).show();
            bluetoothScanButton.setEnabled(false);
        }
    }

    private void setupMidi() {
        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        mMidiManager.registerDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceRemoved(final MidiDeviceInfo info) {
                mOpenDeviceListAdapter.remove(info);
            }

            // Update port open counts so user knows if the device is in use.
            @Override
            public void onDeviceStatusChanged(final MidiDeviceStatus status) {
                MidiDeviceInfo info = status.getDeviceInfo();
                BluetoothMidiDeviceTracker tracker = mOpenDeviceListAdapter
                        .getDevice(info);
                if (tracker != null) {
                    tracker.outputOpenCount = 0;
                    for (int i = 0; i < info.getOutputPortCount(); i++) {
                        tracker.outputOpenCount += status
                                .getOutputPortOpenCount(i);
                    }
                    tracker.inputOpenCount = 0;
                    for (int i = 0; i < info.getInputPortCount(); i++) {
                        tracker.inputOpenCount += status.isInputPortOpen(i) ? 1
                                : 0;
                    }
                    mOpenDeviceListAdapter.notifyDataSetChanged();
                }
            }
        }, new android.os.Handler(Looper.getMainLooper()));
    }

    @Override
    public void onDestroy() {
        mOpenDeviceListAdapter.clear();
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

    private void closeBluetoothDevice(BluetoothMidiDeviceTracker tracker) {
        if (tracker != null) {
            Log.i(TAG, "Closing Bluetooth MIDI device, info = "
                    + tracker.midiDevice.getInfo());
        }
        doClose(tracker.midiDevice);
    }

    public void closeAll() {
        for (int i = 0; i < mOpenDeviceListAdapter.getCount(); i++) {
            BluetoothMidiDeviceTracker tracker = mOpenDeviceListAdapter
                    .getDevice(i);
            if (tracker != null) {
                Log.i(TAG, "Closing Bluetooth MIDI device, info = "
                        + tracker.midiDevice.getInfo());
            }
            doClose(tracker.midiDevice);
        }
        mOpenDeviceListAdapter.clear();
    }

    private final View.OnClickListener mBluetoothScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                openBluetoothScan();
            } else {
                requestPermissions(
                        new String[] { Manifest.permission.BLUETOOTH }, 0);
            }
        }
    };

    private void openBluetoothScan() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setClassName(PACKAGE_NAME, PACKAGE_NAME + ".DeviceScanActivity");
        startActivityForResult(intent, REQUEST_BLUETOOTH_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openBluetoothScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == REQUEST_BLUETOOTH_SCAN && resultCode == RESULT_OK) {
            final BluetoothDevice fBluetoothDevice = (BluetoothDevice) data
                    .getParcelableExtra("device");
            if (fBluetoothDevice != null) {
                Log.i(TAG, "Bluetooth device name = "
                        + fBluetoothDevice.getName()
                        + ", address = "
                        + fBluetoothDevice.getAddress());
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
            }
        }
    }
}
