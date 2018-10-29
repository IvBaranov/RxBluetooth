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
package com.github.ivbaranov.rxbluetooth.events;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Event container class.  Contains connection state (whether the device is disconnected,
 * connecting, connected, or disconnecting), previous connection state, and {@link BluetoothDevice}.
 *
 * Possible state values are:
 * {@link BluetoothAdapter#STATE_DISCONNECTED},
 * {@link BluetoothAdapter#STATE_CONNECTING},
 * {@link BluetoothAdapter#STATE_CONNECTED},
 * {@link BluetoothAdapter#STATE_DISCONNECTING}
 */
public final class ConnectionStateEvent {

  private int state;
  private int previousState;
  private BluetoothDevice bluetoothDevice;

  public ConnectionStateEvent(int state, int previousState, BluetoothDevice bluetoothDevice) {
    this.state = state;
    this.previousState = previousState;
    this.bluetoothDevice = bluetoothDevice;
  }

  public int getState() {
    return state;
  }

  public int getPreviousState() {
    return previousState;
  }

  public BluetoothDevice getBluetoothDevice() {
    return bluetoothDevice;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectionStateEvent that = (ConnectionStateEvent) o;

    if (state != that.state) return false;
    if (previousState != that.previousState) return false;
    return !(bluetoothDevice != null ? !bluetoothDevice.equals(that.bluetoothDevice)
        : that.bluetoothDevice != null);
  }

  @Override public int hashCode() {
    int result = state;
    result = 31 * result + previousState;
    result = 31 * result + (bluetoothDevice != null ? bluetoothDevice.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "ConnectionStateEvent{"
        + "state="
        + state
        + ", previousState="
        + previousState
        + ", bluetoothDevice="
        + bluetoothDevice
        + '}';
  }
}
