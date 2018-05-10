package com.github.ivbaranov.rxbluetooth.example;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.ivbaranov.rxbluetooth.predicates.BtPredicate;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private static final int REQUEST_PERMISSION_COARSE_LOCATION = 0;
  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = "MainActivity";

  private Button start;
  private Button stop;
  private ListView result;
  private Toolbar toolbar;
  private RxBluetooth rxBluetooth;
  private CompositeDisposable compositeDisposable = new CompositeDisposable();
  private List<BluetoothDevice> devices = new ArrayList<>();
  private Intent bluetoothServiceIntent;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    start = (Button) findViewById(R.id.start);
    stop = (Button) findViewById(R.id.stop);
    result = (ListView) findViewById(R.id.result);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("RxBluetooth");
    toolbar.inflateMenu(R.menu.main_menu);
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.menu_enable_bt:
            if (rxBluetooth.isBluetoothAvailable() && !rxBluetooth.isBluetoothEnabled()) {
              Log.d(TAG, "Enabling Bluetooth");
              rxBluetooth.enableBluetooth(MainActivity.this, REQUEST_ENABLE_BT);
            }
            return true;

          case R.id.menu_service_start:
            showToast("Starting service");
            startService(bluetoothServiceIntent);
            return true;

          case R.id.menu_service_stop:
            showToast("Stopping service");
            stopService(bluetoothServiceIntent);
            return true;
        }
        return false;
      }
    });

    bluetoothServiceIntent = new Intent(MainActivity.this, BluetoothService.class);

    rxBluetooth = new RxBluetooth(this);

    if (!rxBluetooth.isBluetoothAvailable()) {
      // handle the lack of bluetooth support
      Log.d(TAG, "Bluetooth is not supported!");
    } else {
      initEventListeners();
      // check if bluetooth is currently enabled and ready for use
      if (!rxBluetooth.isBluetoothEnabled()) {
        // to enable bluetooth via startActivityForResult()
        Log.d(TAG, "Enabling Bluetooth");
        rxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT);
      } else {
        // you are ready
        start.setBackgroundColor(getResources().getColor(R.color.colorActive));
      }
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (rxBluetooth != null) {
      // Make sure we're not doing discovery anymore
      rxBluetooth.cancelDiscovery();
    }
    compositeDisposable.dispose();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_PERMISSION_COARSE_LOCATION) {
      for (String permission : permissions) {
        if (android.Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
          // Start discovery if permission granted
          rxBluetooth.startDiscovery();
        }
      }
    }
  }

  private void initEventListeners() {
    compositeDisposable.add(rxBluetooth.observeDevices()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .subscribe(new Consumer<BluetoothDevice>() {
          @Override public void accept(@NonNull BluetoothDevice bluetoothDevice)
              throws Exception {
            addDevice(bluetoothDevice);
          }
        }));

    compositeDisposable.add(rxBluetooth.observeDiscovery()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        .subscribe(new Consumer<String>() {
          @Override public void accept(String action) throws Exception {
            start.setText(R.string.button_searching);
          }
        }));

    compositeDisposable.add(rxBluetooth.observeDiscovery()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        .subscribe(new Consumer<String>() {
          @Override public void accept(String action) throws Exception {
            start.setText(R.string.button_restart);
          }
        }));

    compositeDisposable.add(rxBluetooth.observeBluetoothState()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .filter(BtPredicate.in(BluetoothAdapter.STATE_ON))
        .subscribe(new Consumer<Integer>() {
          @Override public void accept(Integer integer) throws Exception {
            start.setBackgroundColor(getResources().getColor(R.color.colorActive));
          }
        }));

    compositeDisposable.add(rxBluetooth.observeBluetoothState()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .filter(BtPredicate.in(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF,
            BluetoothAdapter.STATE_TURNING_ON))
        .subscribe(new Consumer<Integer>() {
          @Override public void accept(Integer integer) {
            start.setBackgroundColor(getResources().getColor(R.color.colorInactive));
          }
        }));

    start.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        devices.clear();
        setAdapter(devices);
        if (ContextCompat.checkSelfPermission(MainActivity.this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(MainActivity.this,
              new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION },
              REQUEST_PERMISSION_COARSE_LOCATION);
        } else {
          rxBluetooth.startDiscovery();
        }
      }
    });
    stop.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        rxBluetooth.cancelDiscovery();
      }
    });
  }

  private void addDevice(BluetoothDevice device) {
    devices.add(device);
    setAdapter(devices);
  }

  private void setAdapter(List<BluetoothDevice> list) {
    int itemLayoutId = android.R.layout.simple_list_item_1;
    result.setAdapter(new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_2,
        android.R.id.text1, list) {
      @NonNull @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        BluetoothDevice device = devices.get(position);
        String devName = device.getName();
        String devAddress = device.getAddress();

        if (TextUtils.isEmpty(devName)) {
          devName = "NO NAME";
        }
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        TextView text2 = (TextView) view.findViewById(android.R.id.text2);

        text1.setText(devName);
        text2.setText(devAddress);
        return view;
      }
    });
  }

  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }
}
