/*
 * Copyright (C) 2015 Ivan Baranov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ivbaranov.rxbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import com.github.ivbaranov.rxbluetooth.events.AclEvent;
import com.github.ivbaranov.rxbluetooth.events.BondStateEvent;
import com.github.ivbaranov.rxbluetooth.events.ConnectionStateEvent;
import com.github.ivbaranov.rxbluetooth.events.ServiceEvent;
import com.github.ivbaranov.rxbluetooth.exceptions.GetProfileProxyException;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.MainThreadDisposable;
import io.reactivex.annotations.NonNull;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.os.Build.VERSION.SDK_INT;
import static com.github.ivbaranov.rxbluetooth.Utils.createRfcommSocket;

/**
 * Enables clients to listen to bluetooth events using RxJava Observables.
 */
public final class RxBluetooth {
  BluetoothAdapter bluetoothAdapter;
  Context context;

  public RxBluetooth(Context context) {
    this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    this.context = context;
  }

  /**
   * Return true if Bluetooth is available.
   *
   * @return true if bluetoothAdapter is not null or it's address is empty, otherwise Bluetooth is
   * not supported on this hardware platform
   */
  public boolean isBluetoothAvailable() {
    return !(bluetoothAdapter == null || TextUtils.isEmpty(bluetoothAdapter.getAddress()));
  }

  /**
   * Return true if Bluetooth is currently enabled and ready for use.
   * <p>Equivalent to:
   * <code>getBluetoothState() == STATE_ON</code>
   * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
   *
   * @return true if the local adapter is turned on
   */
  public boolean isBluetoothEnabled() {
    return bluetoothAdapter.isEnabled();
  }

  /**
   * Return true if Location permission is granted.
   *
   * @return true if the local permission is granted. Pre 23 it will always return true. Post 22
   * it will ask the Context whether the permission has been granted or not.
   */
  public boolean isLocationPermissionGranted() {
    if (SDK_INT >= 23) {
      return context.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    return true;
  }

  /**
   * Return true if a location service is enabled.
   *
   * @return true if either the GPS or Network provider is enabled
   */
  public boolean isLocationServiceEnabled() {
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    return locationManager.isProviderEnabled(GPS_PROVIDER) || locationManager.isProviderEnabled(NETWORK_PROVIDER);
  }

  /**
   * This will issue a request to enable Bluetooth through the system settings (without stopping
   * your application) via ACTION_REQUEST_ENABLE action Intent.
   *
   * @param activity Activity
   * @param requestCode request code
   */
  public void enableBluetooth(Activity activity, int requestCode) {
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      activity.startActivityForResult(enableBtIntent, requestCode);
    }
  }

  /**
   * Turn on the local Bluetooth adapter — do not use without explicit user action to turn on
   * Bluetooth.
   *
   * @return true to indicate adapter startup has begun, or false on
   * immediate error
   * @see BluetoothAdapter#enable()
   */
  public boolean enable() {
    return bluetoothAdapter.enable();
  }

  /**
   * Turn off the local Bluetooth adapter — do not use without explicit user action to turn off
   * Bluetooth.
   *
   * @return true to indicate adapter shutdown has begun, or false on
   * immediate error
   * @see BluetoothAdapter#enable()
   */
  public boolean disable() {
    return bluetoothAdapter.disable();
  }

  /**
   * Return the set of {@link BluetoothDevice} objects that are bonded
   * (paired) to the local adapter.
   * <p>If Bluetooth state is not {@link BluetoothAdapter#STATE_ON}, this API
   * will return an empty set. After turning on Bluetooth,
   * wait for {@link BluetoothAdapter#ACTION_STATE_CHANGED} with {@link BluetoothAdapter#STATE_ON}
   * to get the updated value.
   * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
   *
   * @return unmodifiable set of {@link BluetoothDevice}, or null on error
   */
  @Nullable public Set<BluetoothDevice> getBondedDevices() {
    return bluetoothAdapter.getBondedDevices();
  }

  /**
   * Start the remote device discovery process.
   *
   * @return true on success, false on error
   */
  public boolean startDiscovery() {
    return bluetoothAdapter.startDiscovery();
  }

  /**
   * Return true if the local Bluetooth adapter is currently in the device
   * discovery process.
   *
   * @return true if discovering
   */
  public boolean isDiscovering() {
    return bluetoothAdapter.isDiscovering();
  }

  /**
   * Cancel the current device discovery process.
   *
   * @return true on success, false on error
   */
  public boolean cancelDiscovery() {
    return bluetoothAdapter.cancelDiscovery();
  }

  /**
   * This will issue a request to make the local device discoverable to other devices. By default,
   * the device will become discoverable for 120 seconds.
   *
   * @param activity Activity
   * @param requestCode request code
   */
  public void enableDiscoverability(Activity activity, int requestCode) {
    enableDiscoverability(activity, requestCode, -1);
  }

  /**
   * This will issue a request to make the local device discoverable to other devices. By default,
   * the device will become discoverable for 120 seconds.  Maximum duration is capped at 300
   * seconds.
   *
   * @param activity Activity
   * @param requestCode request code
   * @param duration discoverability duration in seconds
   */
  public void enableDiscoverability(Activity activity, int requestCode, int duration) {
    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    if (duration >= 0) {
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
    }
    activity.startActivityForResult(discoverableIntent, requestCode);
  }

  /**
   * Observes Bluetooth devices found while discovering.
   *
   * @return RxJava Observable with BluetoothDevice found
   */
  public Observable<BluetoothDevice> observeDevices() {
    final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    return Observable.create(new ObservableOnSubscribe<BluetoothDevice>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<BluetoothDevice> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
              BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
              emitter.onNext(device);
            }
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Observes DiscoveryState, which can be ACTION_DISCOVERY_STARTED or ACTION_DISCOVERY_FINISHED
   * from {@link BluetoothAdapter}.
   *
   * @return RxJava Observable with DiscoveryState
   */
  public Observable<String> observeDiscovery() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

    return Observable.create(new ObservableOnSubscribe<String>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<String> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            emitter.onNext(intent.getAction());
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Observes BluetoothState. Possible values are:
   * {@link BluetoothAdapter#STATE_OFF},
   * {@link BluetoothAdapter#STATE_TURNING_ON},
   * {@link BluetoothAdapter#STATE_ON},
   * {@link BluetoothAdapter#STATE_TURNING_OFF},
   *
   * @return RxJava Observable with BluetoothState
   */
  public Observable<Integer> observeBluetoothState() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

    return Observable.create(new ObservableOnSubscribe<Integer>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<Integer> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            emitter.onNext(bluetoothAdapter.getState());
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Observes scan mode of device. Possible values are:
   * {@link BluetoothAdapter#SCAN_MODE_NONE},
   * {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE},
   * {@link BluetoothAdapter#SCAN_MODE_CONNECTABLE_DISCOVERABLE}
   *
   * @return RxJava Observable with scan mode
   */
  public Observable<Integer> observeScanMode() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);

    return Observable.create(new ObservableOnSubscribe<Integer>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<Integer> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            emitter.onNext(bluetoothAdapter.getScanMode());
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Observes connection to specified profile. See also {@link BluetoothProfile.ServiceListener}.
   *
   * @param bluetoothProfile bluetooth profile to connect to. Can be either {@link
   * BluetoothProfile#HEALTH},{@link BluetoothProfile#HEADSET}, {@link BluetoothProfile#A2DP},
   * {@link BluetoothProfile#GATT} or {@link BluetoothProfile#GATT_SERVER}.
   * @return RxJava Observable with {@link ServiceEvent}
   */
  public Observable<ServiceEvent> observeBluetoothProfile(final int bluetoothProfile) {
    return Observable.create(new ObservableOnSubscribe<ServiceEvent>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<ServiceEvent> emitter)
          throws Exception {
        if (!bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
          @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
            emitter.onNext(new ServiceEvent(ServiceEvent.State.CONNECTED, profile, proxy));
          }

          @Override public void onServiceDisconnected(int profile) {
            emitter.onNext(new ServiceEvent(ServiceEvent.State.DISCONNECTED, profile, null));
          }
        }, bluetoothProfile)) {
          emitter.onError(new GetProfileProxyException());
        }
      }
    });
  }

  /**
   * Close the connection of the profile proxy to the Service.
   *
   * <p> Clients should call this when they are no longer using the proxy obtained from {@link
   * #observeBluetoothProfile}.
   * <p>Profile can be one of {@link BluetoothProfile#HEALTH},{@link BluetoothProfile#HEADSET},
   * {@link BluetoothProfile#A2DP}, {@link BluetoothProfile#GATT} or {@link
   * BluetoothProfile#GATT_SERVER}.
   *
   * @param profile the Bluetooth profile
   * @param proxy profile proxy object
   */
  public void closeProfileProxy(int profile, BluetoothProfile proxy) {
    bluetoothAdapter.closeProfileProxy(profile, proxy);
  }

  /**
   * Observes connection state of devices.
   *
   * @return RxJava Observable with {@link ConnectionStateEvent}
   */
  public Observable<ConnectionStateEvent> observeConnectionState() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

    return Observable.create(new ObservableOnSubscribe<ConnectionStateEvent>() {
      @Override
      public void subscribe(@NonNull final ObservableEmitter<ConnectionStateEvent> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                BluetoothAdapter.STATE_DISCONNECTED);
            int previousStatus =
                intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE,
                    BluetoothAdapter.STATE_DISCONNECTED);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            emitter.onNext(new ConnectionStateEvent(status, previousStatus, device));
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Observes bond state of devices.
   *
   * @return RxJava Observable with {@link BondStateEvent}
   */
  public Observable<BondStateEvent> observeBondState() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

    return Observable.create(new ObservableOnSubscribe<BondStateEvent>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<BondStateEvent> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            int state =
                intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
            int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                BluetoothDevice.BOND_NONE);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            emitter.onNext(new BondStateEvent(state, previousState, device));
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

  /**
   * Opens {@link BluetoothServerSocket}, listens for a single connection request, releases socket
   * and returns a connected {@link BluetoothSocket} on successful connection. Notifies observers
   * with {@link IOException} {@code onError()}.
   *
   * @param name service name for SDP record
   * @param uuid uuid for SDP record
   * @return observable with connected {@link BluetoothSocket} on successful connection
   * @deprecated use {{@link #connectAsServer(String, UUID)}} instead
   */
  @Deprecated public Observable<BluetoothSocket> observeBluetoothSocket(final String name, final UUID uuid) {
    return connectAsServer(name, uuid).toObservable();
  }

  /**
   * Create connection to {@link BluetoothDevice} and returns a connected {@link BluetoothSocket}
   * on successful connection. Notifies observers with {@link IOException} via {@code onError()}.
   *
   * @param bluetoothDevice bluetooth device to connect
   * @param uuid uuid for SDP record
   * @return observable with connected {@link BluetoothSocket} on successful connection
   * @deprecated use {{@link #connectAsClient(BluetoothDevice, UUID)}} instead
   */
  @Deprecated public Observable<BluetoothSocket> observeConnectDevice(final BluetoothDevice bluetoothDevice,
      final UUID uuid) {
    return connectAsClient(bluetoothDevice, uuid).toObservable();
  }

  /**
   * Opens {@link BluetoothServerSocket}, listens for a single connection request, releases socket
   * and returns a connected {@link BluetoothSocket} on successful connection. Notifies observers
   * with {@link IOException} {@code onError()}.
   *
   * @param name service name for SDP record
   * @param uuid uuid for SDP record
   * @return Single with connected {@link BluetoothSocket} on successful connection
   */
  public Single<BluetoothSocket> connectAsServer(final String name, final UUID uuid) {
    return Single.create(new SingleOnSubscribe<BluetoothSocket>() {
      @Override public void subscribe(@NonNull SingleEmitter<BluetoothSocket> emitter) {
        try {
          BluetoothServerSocket bluetoothServerSocket =
              bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
          try {
            emitter.onSuccess(bluetoothServerSocket.accept());
          } finally {
            bluetoothServerSocket.close();
          }
        } catch(IOException e) {
            emitter.onError(e);
        }
      }
    });
  }

  /**
   * Create connection to {@link BluetoothDevice} and returns a connected {@link BluetoothSocket}
   * on successful connection. Notifies observers with {@link IOException} via {@code onError()}.
   *
   * @param bluetoothDevice bluetooth device to connect
   * @param uuid uuid for SDP record
   * @return Single with connected {@link BluetoothSocket} on successful connection
   */
  public Single<BluetoothSocket> connectAsClient(final BluetoothDevice bluetoothDevice,
      final UUID uuid) {
    return Single.create(new SingleOnSubscribe<BluetoothSocket>() {
      @Override public void subscribe(@NonNull SingleEmitter<BluetoothSocket> emitter) {
        BluetoothSocket bluetoothSocket = null;
        try {
          bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
          bluetoothSocket.connect();
          emitter.onSuccess(bluetoothSocket);
        } catch (IOException e) {
          if (bluetoothSocket != null) {
            try {
              bluetoothSocket.close();
            } catch (IOException suppressed) {
              if (SDK_INT >= 19) {
                e.addSuppressed(suppressed);
              }
            }
          }
          emitter.onError(e);
        }
      }
    });
  }

  /**
   * Create connection to {@link BluetoothDevice} via createRfcommSocket and returns a connected {@link BluetoothSocket}
   * on successful connection.
   * Note: createRfcommSocket is not public API and hence this might break in the future.
   * Notifies observers with {@link IOException} or any reflection related exception via {@code onError()}.
   *
   * @param bluetoothDevice bluetooth device to connect
   * @param channel RFCOMM channel to connect to
   * @return Single with connected {@link BluetoothSocket} on successful connection
   */
  public Single<BluetoothSocket> connectAsClient(final BluetoothDevice bluetoothDevice,
      final int channel) {
    return Single.create(new SingleOnSubscribe<BluetoothSocket>() {
      @Override public void subscribe(@NonNull SingleEmitter<BluetoothSocket> emitter) {
        BluetoothSocket bluetoothSocket = null;
        try {
          bluetoothSocket = createRfcommSocket(bluetoothDevice, channel);
          bluetoothSocket.connect();
          emitter.onSuccess(bluetoothSocket);
        } catch (IOException e) {
          if (bluetoothSocket != null) {
            try {
              bluetoothSocket.close();
            } catch (IOException suppressed) {
              if (SDK_INT >= 19) {
                e.addSuppressed(suppressed);
              }
            }
          }
          emitter.onError(e);
        }
      }
    });
  }

  /**
   * Observes ACL broadcast actions from {@link BluetoothDevice}. Possible broadcast ACL action
   * values are:
   * {@link BluetoothDevice#ACTION_ACL_CONNECTED},
   * {@link BluetoothDevice#ACTION_ACL_DISCONNECT_REQUESTED},
   * {@link BluetoothDevice#ACTION_ACL_DISCONNECTED}
   *
   * @return RxJava Observable with {@link AclEvent}
   */
  public Observable<AclEvent> observeAclEvent() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);

    return Observable.create(new ObservableOnSubscribe<AclEvent>() {
      @Override public void subscribe(@NonNull final ObservableEmitter<AclEvent> emitter)
          throws Exception {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            emitter.onNext(new AclEvent(action, device));
          }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
          @Override protected void onDispose() {
            context.unregisterReceiver(receiver);
          }
        });
      }
    });
  }

    /**
     * Performs a service discovery and fetches a list of UUIDs that can be used to connect to {@link BluetoothDevice}
     *
     * @param bluetoothDevice bluetooth device to connect
     * @return RxJava Observable with an array of Device UUIDs that can be used to connect to the device
     */

  @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
  public Observable<Parcelable[]> observeFetchDeviceUuids(final BluetoothDevice bluetoothDevice) {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_UUID);

    return Observable.create(new ObservableOnSubscribe<Parcelable[]>() {
      @Override
      public void subscribe(@NonNull final ObservableEmitter<Parcelable[]> emitter) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override public void onReceive(Context context, Intent intent) {
                  Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                  emitter.onNext(uuids);
                  emitter.onComplete();
              }
        };

        context.registerReceiver(receiver, filter);

        emitter.setDisposable(new MainThreadDisposable() {
            @Override
            protected void onDispose() {
                context.unregisterReceiver(receiver);
            }
        });

        bluetoothDevice.fetchUuidsWithSdp();
      }
    });
  }
}
