RxBluetooth
===========

[![Build Status](https://travis-ci.org/IvBaranov/RxBluetooth.svg)](https://travis-ci.org/IvBaranov/RxBluetooth)

Android reactive bluetooth library. Basically, RxBluetooth is just wrapper around android [BluetoothAdapter](http://developer.android.com/intl/ru/reference/android/bluetooth/BluetoothAdapter.html), so first of all the [Bluetooth](http://developer.android.com/intl/ru/guide/topics/connectivity/bluetooth.html) developer guide should be read.

RxBluetooth is in early-stage. There is a lot of missing stuff. Feel free to [contribute](#contributing).

Full documentation
------------------

* [Wiki](https://github.com/IvBaranov/RxBluetooth/wiki/Getting-started)
* [Javadoc](http://ivbaranov.github.io/RxBluetooth/javadoc/)

Usage
-----

1. Declare permissions:
   ```xml
   <uses-permission android:name="android.permission.BLUETOOTH" />
   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
   ```

2. Create `RxBluetooth` instance.

3. Check if bluetooth is currently enabled and ready for use:
   ```java
   if  (!rxBluetooth.isBluetoothAvailable()) {
      // to enable blutooth via startActivityForResult()
      rxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT);
   }
   ```

4. Have fun.
5. Make sure you are unsubscribing and stopping discovery `OnDestroy()`:

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
      .subscribe(new Action1<BluetoothDevice>() {
        @Override public void call(BluetoothDevice bluetoothDevice) {
          //
        }
      });
```

##### Create connection to device
```java
// Use 00001101-0000-1000-8000-00805F9B34FB for SPP service 
// (ex. Arduino) or use your own generated UUID.
UUID uuid = UUID.fromString("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");

rxBluetooth.observeConnectDevice(bluetoothDevice, uuid)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .subscribe(new Action1<BluetoothSocket>() {
        @Override public void call(BluetoothSocket socket) {
          // Connected to the device, do anything with the socket 
        }
      }, new Action1<Throwable>() {
       @Override public void call(Throwable throwable) {
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
      .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
      .subscribe(new Action1<String>() {
        @Override public void call(String action) {
          //
        }
      });
```

To observe both `ACTION_DISCOVERY_STARTED` and `ACTION_DISCOVERY_FINISHED`:

```java
rxBluetooth.observeDiscovery()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.computation())
      .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
      .subscribe(new Action1<String>() {
        @Override public void call(String action) {
          //
        }
      });
```

##### Observing bluetooth state

```java
rxBluetooth.observeBluetoothState()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.computation())
      .filter(Action.isEqualTo(BluetoothAdapter.STATE_ON))
      .subscribe(new Action1<Integer>() {
        @Override public void call(Integer integer) {
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
      .filter(Action.isEqualTo(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
      .subscribe(new Action1<Integer>() {
        @Override public void call(Integer integer) {
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
      .subscribe(new Action1<ServiceEvent>() {
        @Override public void call(ServiceEvent serviceEvent) {
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
      .subscribe(new Action1<ConnectionStateEvent>() {
        @Override public void call(ConnectionStateEvent event) {
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
      .subscribe(new Action1<BondStateEvent>() {
        @Override public void call(BondStateEvent event) {
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
    .subscribe(new Action1<Byte>() {
      @Override public void call(Byte aByte) {
        // This will be called every single byte received
      }
    }, new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        // Error occured
      }
    });

// Or just observe string
bluetoothConnection.observeStringStream()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeOn(Schedulers.io())
    .subscribe(new Action1<String>() {
      @Override public void call(String string) {
        // This will be called every string received
      }
    }, new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        // Error occured
      }
    });
```

##### Write:
```java
bluetoothConnection.send("Hello"); // String
bluetoothConnection.send("There".getBytes()); // Array of bytes
```

Download
--------
```groovy
compile 'com.github.ivbaranov:rxbluetooth:0.1.4'
```
Snapshots of the development version are available in [Sonatype's `snapshots` repository][snapshots].

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
 
