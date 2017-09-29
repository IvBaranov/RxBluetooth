RxBluetooth
===========

[![Build Status](https://travis-ci.org/IvBaranov/RxBluetooth.svg)](https://travis-ci.org/IvBaranov/RxBluetooth)

Android reactive bluetooth library. Basically, RxBluetooth is just wrapper around android [BluetoothAdapter](http://developer.android.com/intl/ru/reference/android/bluetooth/BluetoothAdapter.html), so first of all the [Bluetooth](http://developer.android.com/intl/ru/guide/topics/connectivity/bluetooth.html) developer guide should be read.

RxBluetooth for RxJava 1 is available in [respective branch](https://github.com/IvBaranov/RxBluetooth/tree/rxjava-1.x).

Full documentation
------------------

* [Wiki](https://github.com/IvBaranov/RxBluetooth/wiki/Getting-started)
* [Javadoc](http://ivbaranov.github.io/RxBluetooth/javadoc2/)

Usage
-----

1. Declare permissions:
   ```xml
   <uses-permission android:name="android.permission.BLUETOOTH" />
   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
   // If you intend to run on devices with android 6.0+ you also need to declare:
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   ```

2. Create `RxBluetooth` instance.
   ```java
   RxBluetooth rxBluetooth = new RxBluetooth(this); // `this` is a context
   ```
3. For android 6.0+ you need location permision.
   ```java
   if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
   }
   // And catch the result like this:
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            for (String permission : permissions) {
                if (android.Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                    // Do stuff if permission granted
                }
            }
        }
    }
   ```

4. Check that bluetooth is available and enabled:
   ```java
   // check if bluetooth is supported on your hardware
   if  (!rxBluetooth.isBluetoothAvailable()) {
      // handle the lack of bluetooth support
   } else {
      // check if bluetooth is currently enabled and ready for use
      if (!rxBluetooth.isBluetoothEnabled()) { 
         // to enable bluetooth via startActivityForResult()
         rxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT);
      } else {
         // you are ready
      }
   }
   ```

5. Have fun.
6. Make sure you are unsubscribing and stopping discovery in `OnDestroy()`:

   ```java
   if (rxBluetooth != null) {
         rxBluetooth.cancelDiscovery();
       }
   unsubscribe(rxBluetoothSubscription);
   ```

##### Observing devices
```java
rxBluetooth.observeDevices()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .subscribe(new Consumer<BluetoothDevice>() {
      @Override public void accept(@NonNull BluetoothDevice bluetoothDevice) throws Exception {
        //
      }
    }));
```

##### Create connection to device
```java
// Use 00001101-0000-1000-8000-00805F9B34FB for SPP service 
// (ex. Arduino) or use your own generated UUID.
UUID uuid = UUID.fromString("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");

rxBluetooth.observeConnectDevice(bluetoothDevice, uuid)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.io())
    .subscribe(new Consumer<BluetoothSocket>() {
      @Override public void accept(BluetoothSocket socket) throws Exception {
        // Connected to the device, do anything with the socket
      }
    }, new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        // Error occured
      }
    });
```

##### Observing discovery state

To observe just `ACTION_DISCOVERY_STARTED`:

```java
rxBluetooth.observeDiscovery()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
    .subscribe(new Consumer<String>() {
      @Override public void accept(String action) throws Exception {
        //
      }
    });
```

To observe both `ACTION_DISCOVERY_STARTED` and `ACTION_DISCOVERY_FINISHED`:

```java
rxBluetooth.observeDiscovery()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    .subscribe(new Consumer<String>() {
      @Override public void accept(String action) throws Exception {
        //
      }
    });
```

##### Observing bluetooth state

```java
rxBluetooth.observeBluetoothState()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .filter(BtPredicate.in(BluetoothAdapter.STATE_ON))
    .subscribe(new Consumer<Integer>() {
      @Override public void accept(Integer integer) throws Exception {
        //
      }
    });
```

You can observe single or multiple states:
```java
BluetoothAdapter.STATE_OFF
BluetoothAdapter.STATE_TURNING_ON
BluetoothAdapter.STATE_ON
BluetoothAdapter.STATE_TURNING_OFF
```

##### Observing scan mode

```java
rxBluetooth.observeScanMode()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .filter(BtPredicate.in(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
    .subscribe(new Consumer<Integer>() {
    @Override public void accept(Integer integer) throws Exception {
      //
    }
    });
```

You can observe single or multiple scan modes:
```java
BluetoothAdapter.SCAN_MODE_NONE
BluetoothAdapter.SCAN_MODE_CONNECTABLE
BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
```

##### Getting the profile proxy object

```java
rxBluetooth.observeBluetoothProfile(myProfile)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .subscribe(new Consumer<ServiceEvent>() {
    @Override public void accept(ServiceEvent serviceEvent) throws Exception {
      switch (serviceEvent.getState()) {
       case CONNECTED:
            BluetoothProfile bluetoothProfile = serviceEvent.getBluetoothProfile();
            List<BluetoothDevice> devices = bluetoothProfile.getConnectedDevices();
            for ( final BluetoothDevice dev : devices ) {
              //..
            }
            break;
       case DISCONNECTED:
            //serviceEvent.getBluetoothProfile() returns null
            break;
        }
      }
    });
```

`myProfile` can be one of `BluetoothProfile.HEALTH`, `BluetoothProfile.HEADSET`, `BluetoothProfile.A2DP`, `BluetoothProfile.GATT` or `BluetoothProfile.GATT_SERVER`

Clients should close profile proxy when they are no longer using the proxy obtained from `observeBluetoothProfile`:
```java
rxBluetooth.closeProfileProxy(int profile, BluetoothProfile proxy);
```

##### Observing device state

To observe the current device state, you can receive the `ConnectionStateEvent` which provides the state, previous state, and `BluetoothDevice`.

```java
rxBluetooth.observeConnectionState()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .subscribe(new Consumer<ConnectionStateEvent>() {
    @Override public void accept(ConnectionStateEvent event) throws Exception {
      switch (event.getState()) {
        case BluetoothAdapter.STATE_DISCONNECTED:
            // device disconnected
            break;
        case BluetoothAdapter.STATE_CONNECTING:
            // device connecting
            break;
        case BluetoothAdapter.STATE_CONNECTED:
            // device connected
            break;
        case BluetoothAdapter.STATE_DISCONNECTING:
            // device disconnecting
            break;
      }
    }
    });
```

Possible states are:
```java
BluetoothAdapter.STATE_DISCONNECTED
BluetoothAdapter.STATE_CONNECTING
BluetoothAdapter.STATE_CONNECTED
BluetoothAdapter.STATE_DISCONNECTING
```

##### Observing device bond state

To observe the bond state of devices, you can receive the `BondStateEvent` which provides the state, previous state, and `BluetoothDevice`.

```java
rxBluetooth.observeBondState()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .subscribe(new Consumer<BondStateEvent>() {
    @Override public void accept(BondStateEvent event) throws Exception {
      switch (event.getState()) {
        case BluetoothDevice.BOND_NONE:
            // device unbonded
            break;
        case BluetoothDevice.BOND_BONDING:
            // device bonding
            break;
        case BluetoothDevice.BOND_BONDED:
            // device bonded
            break;
      }
    }
    });
```

Possible states are:
```java
BluetoothDevice.BOND_NONE
BluetoothDevice.BOND_BONDING
BluetoothDevice.BOND_BONDED
```

#### Read and Write with BluetoothSocket
After creating a connection to the device, you can use `BluetoothConnection` class to read and write with its socket.

##### Read:
```java
BluetoothConnection bluetoothConnection = new BluetoothConnection(bluetoothSocket);

// Observe every byte received
bluetoothConnection.observeByteStream()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.io())
    .subscribe(new Consumer<Byte>() {
      @Override public void accept(Byte aByte) throws Exception {
        // This will be called every single byte received
      }
    }, new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {
        // Error occured
      }
    });

// Or just observe string
bluetoothConnection.observeStringStream()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.io())
    .subscribe(new Consumer<String>() {
      @Override public void call(String string) throws Exception {
        // This will be called every string received
      }
    }, new Consumer<Throwable>() {
      @Override public void call(Throwable throwable) throws Exception {
        // Error occured
      }
    });
```

##### Write:
```java
bluetoothConnection.send("Hello"); // String
bluetoothConnection.send("There".getBytes()); // Array of bytes
```

##### Close:
Remember to close streams and socket once you are done.
```java
bluetoothConnection.closeConnection();
```

#### Observe ACL actions
```java
rxBluetooth.observeAclEvent() //
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.computation())
    .subscribe(new Consumer<AclEvent>() {
      @Override public void accept(AclEvent aclEvent) throws Exception {
        switch (aclEvent.getAction()) {
          case BluetoothDevice.ACTION_ACL_CONNECTED:
            //...
            break;
          case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
            //...
            break;
          case BluetoothDevice.ACTION_ACL_DISCONNECTED:
            //...
            break;
        }
      }
    });
```

Download
--------
```groovy
compile 'com.github.ivbaranov:rxbluetooth2:2.0.0-SNAPSHOT'
```
Snapshots of the development version are available in [Sonatype's `snapshots` repository][snapshots].

In order to download from snapshot repository add:
```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
}
```

<a name="contributing"></a>Contributing
------------
Make sure you use SquareAndroid code style. (https://github.com/square/java-code-styles)

Create a branch for each feature.

Developed By
------------
Ivan Baranov

License
-------

```
Copyright 2015 Ivan Baranov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
[snapshots]: https://oss.sonatype.org/content/repositories/snapshots/
 
