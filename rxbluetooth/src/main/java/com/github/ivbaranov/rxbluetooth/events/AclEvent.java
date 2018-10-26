/*
 * Copyright (C) 2017 Ivan Baranov
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

import android.bluetooth.BluetoothDevice;

/**
 * Event container class.  Contains broadcast ACL action and {@link BluetoothDevice}.
 *
 * Possible broadcast ACL action values are:
 * {@link BluetoothDevice#ACTION_ACL_CONNECTED},
 * {@link BluetoothDevice#ACTION_ACL_DISCONNECT_REQUESTED},
 * {@link BluetoothDevice#ACTION_ACL_DISCONNECTED}
 */
public final class AclEvent {

  private String action;
  private BluetoothDevice bluetoothDevice;

  public AclEvent(String action, BluetoothDevice bluetoothDevice) {
    this.action = action;
    this.bluetoothDevice = bluetoothDevice;
  }

  public String getAction() {
    return action;
  }

  public BluetoothDevice getBluetoothDevice() {
    return bluetoothDevice;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AclEvent that = (AclEvent) o;

    if (action != null && !action.equals(that.action)) return false;
    return !(bluetoothDevice != null ? !bluetoothDevice.equals(that.bluetoothDevice)
        : that.bluetoothDevice != null);
  }

  @Override public int hashCode() {
    int result = action != null ? action.hashCode() : 0;
    result = 31 * result + (bluetoothDevice != null ? bluetoothDevice.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "AclEvent{" +
        "action=" + action +
        ", bluetoothDevice=" + bluetoothDevice +
        '}';
  }
}
