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
import android.os.Looper;
import android.text.TextUtils;
import java.io.IOException;
import java.util.UUID;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.subscriptions.Subscriptions;

/**
 * Enables clients to listen to bluetooth events using RxJava Observables.
 */
public class RxBluetooth {
  private BluetoothAdapter mBluetoothAdapter;
  private Context context;

  public RxBluetooth(Context context) {
    this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    this.context = context;
  }

  /**
   * Return true if Bluetooth is available.
   *
   * @return true if mBluetoothAdapter is not null or it's address is empty, otherwise Bluetooth is
   * not supported on this hardware platform
   */
  public boolean isBluetoothAvailable() {
    return !(mBluetoothAdapter == null || TextUtils.isEmpty(mBluetoothAdapter.getAddress()));
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
    return mBluetoothAdapter.isEnabled();
  }

  /**
   * This will issue a request to enable Bluetooth through the system settings (without stopping
   * your application) via ACTION_REQUEST_ENABLE action Intent.
   *
   * @param activity Activity
   * @param requestCode request code
   */
  public void enableBluetooth(Activity activity, int requestCode) {
    if (!mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      activity.startActivityForResult(enableBtIntent, requestCode);
    }
  }

  /**
   * Start the remote device discovery process.
   *
   * @return true on success, false on error
   */
  public boolean startDiscovery() {
    return mBluetoothAdapter.startDiscovery();
  }

  /**
   * Return true if the local Bluetooth adapter is currently in the device
   * discovery process.
   *
   * @return true if discovering
   */
  public boolean isDiscovering() {
    return mBluetoothAdapter.isDiscovering();
  }

  /**
   * Cancel the current device discovery process.
   *
   * @return true on success, false on error
   */
  public boolean cancelDiscovery() {
    return mBluetoothAdapter.cancelDiscovery();
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

    return Observable.defer(new Func0<Observable<BluetoothDevice>>() {

      @Override public Observable<BluetoothDevice> call() {
        return Observable.create(new Observable.OnSubscribe<BluetoothDevice>() {

          @Override public void call(final Subscriber<? super BluetoothDevice> subscriber) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
              @Override public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                  BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                  subscriber.onNext(device);
                }
              }
            };

            context.registerReceiver(receiver, filter);

            subscriber.add(unsubscribeInUiThread(new Action0() {
              @Override public void call() {
                context.unregisterReceiver(receiver);
              }
            }));
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

    return Observable.defer(new Func0<Observable<String>>() {

      @Override public Observable<String> call() {
        return Observable.create(new Observable.OnSubscribe<String>() {

          @Override public void call(final Subscriber<? super String> subscriber) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
              @Override public void onReceive(Context context, Intent intent) {
                subscriber.onNext(intent.getAction());
              }
            };

            context.registerReceiver(receiver, filter);

            subscriber.add(unsubscribeInUiThread(new Action0() {
              @Override public void call() {
                context.unregisterReceiver(receiver);
              }
            }));
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

    return Observable.defer(new Func0<Observable<Integer>>() {
      @Override public Observable<Integer> call() {

        return Observable.create(new Observable.OnSubscribe<Integer>() {

          @Override public void call(final Subscriber<? super Integer> subscriber) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
              @Override public void onReceive(Context context, Intent intent) {
                subscriber.onNext(mBluetoothAdapter.getState());
              }
            };

            context.registerReceiver(receiver, filter);

            subscriber.add(unsubscribeInUiThread(new Action0() {
              @Override public void call() {
                context.unregisterReceiver(receiver);
              }
            }));
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

    return Observable.defer(new Func0<Observable<Integer>>() {
      @Override public Observable<Integer> call() {

        return Observable.create(new Observable.OnSubscribe<Integer>() {

          @Override public void call(final Subscriber<? super Integer> subscriber) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
              @Override
              public void onReceive(Context context, Intent intent) {
                subscriber.onNext(mBluetoothAdapter.getScanMode());
              }
            };

            context.registerReceiver(receiver, filter);

            subscriber.add(unsubscribeInUiThread(new Action0() {
              @Override
              public void call() {
                context.unregisterReceiver(receiver);
              }
            }));
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
    return Observable.defer(new Func0<Observable<ServiceEvent>>() {
      @Override public Observable<ServiceEvent> call() {
        return Observable.create(new Observable.OnSubscribe<ServiceEvent>() {
          @Override public void call(final Subscriber<? super ServiceEvent> subscriber) {
            if (!mBluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
              @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                subscriber.onNext(new ServiceEvent(ServiceEvent.State.CONNECTED, profile, proxy));
              }

              @Override public void onServiceDisconnected(int profile) {
                subscriber.onNext(new ServiceEvent(ServiceEvent.State.DISCONNECTED, profile, null));
              }
            }, bluetoothProfile)) {
              subscriber.onError(new GetProfileProxyException());
            }
          }
        });
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
   * @param proxy Profile proxy object
   */
  public void closeProfileProxy(int profile, BluetoothProfile proxy) {
    mBluetoothAdapter.closeProfileProxy(profile, proxy);
  }

  /**
   * Opens {@link BluetoothServerSocket}, listens for a single connection request, releases socket
   * and returns a connected {@link BluetoothSocket} on successful connection. Notifies observers
   * with {@link IOException} {@code onError()}.
   *
   * @param name service name for SDP record
   * @param uuid uuid for SDP record
   * @return observable with connected {@link BluetoothSocket} on successful connection
   */
  public Observable<BluetoothSocket> observeBluetoothSocket(final String name, final UUID uuid) {
    return Observable.defer(new Func0<Observable<BluetoothSocket>>() {
      @Override public Observable<BluetoothSocket> call() {
        return Observable.create(new Observable.OnSubscribe<BluetoothSocket>() {
          @Override public void call(Subscriber<? super BluetoothSocket> subscriber) {
            try {
              BluetoothServerSocket bluetoothServerSocket =
                      mBluetoothAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
              subscriber.onNext(bluetoothServerSocket.accept());
              bluetoothServerSocket.close();
            } catch (IOException e) {
              subscriber.onError(e);
            }
          }
        });
      }
    });
  }

  private Subscription unsubscribeInUiThread(final Action0 unsubscribe) {
    return Subscriptions.create(new Action0() {

      @Override public void call() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
          unsubscribe.call();
        } else {
          final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
          inner.schedule(new Action0() {
            @Override public void call() {
              unsubscribe.call();
              inner.unsubscribe();
            }
          });
        }
      }
    });
  }
}
