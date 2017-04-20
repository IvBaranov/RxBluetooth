package com.github.ivbaranov.rxbluetooth.example;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class BluetoothService extends Service {
  private static final String TAG = "BluetoothService";

  private RxBluetooth rxBluetooth;
  private Subscription deviceSubscription;

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
        deviceSubscription = rxBluetooth.observeDevices()
            .observeOn(Schedulers.computation())
            .subscribeOn(Schedulers.computation())
            .subscribe(new Action1<BluetoothDevice>() {
              @Override public void call(BluetoothDevice bluetoothDevice) {
                Log.d(TAG,
                    "Device found: " + bluetoothDevice.getAddress() + " - " + bluetoothDevice.getName());
              }
            });
        rxBluetooth.startDiscovery();
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "BluetoothService stopped!");
    rxBluetooth.cancelDiscovery();
    unsubscribe(deviceSubscription);
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private static void unsubscribe(Subscription subscription) {
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
      subscription = null;
    }
  }
}
