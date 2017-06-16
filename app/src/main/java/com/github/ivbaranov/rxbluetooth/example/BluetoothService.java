package com.github.ivbaranov.rxbluetooth.example;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService extends Service {
  private static final String TAG = "BluetoothService";

  private RxBluetooth rxBluetooth;
  private CompositeDisposable compositeDisposable = new CompositeDisposable();

  @Override public void onCreate() {
    super.onCreate();

    Log.d(TAG, "BluetoothService started!");
    rxBluetooth = new RxBluetooth(this);

    if (!rxBluetooth.isBluetoothAvailable()) {
      // handle the lack of bluetooth support
      Log.d(TAG, "Bluetooth is not supported!");
    } else {
      // check if bluetooth is currently enabled and ready for use
      if (!rxBluetooth.isBluetoothEnabled()) {
        Log.d(TAG, "Bluetooth should be enabled first!");
      } else {
        compositeDisposable.add(rxBluetooth.observeDevices()
            .observeOn(Schedulers.computation())
            .subscribeOn(Schedulers.computation())
            .subscribe(new Consumer<BluetoothDevice>() {
              @Override public void accept(BluetoothDevice bluetoothDevice) {
                Log.d(TAG, "Device found: "
                    + bluetoothDevice.getAddress()
                    + " - "
                    + bluetoothDevice.getName());
              }
            }));
        rxBluetooth.startDiscovery();
      }
    }
  }

  @Override public void onDestroy() {
    compositeDisposable.dispose();
    rxBluetooth.cancelDiscovery();

    super.onDestroy();
    Log.d(TAG, "BluetoothService stopped!");
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
