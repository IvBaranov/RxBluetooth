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
public class AclEvent {

  private String mAction;
  private BluetoothDevice mBluetoothDevice;

  public AclEvent(String action, BluetoothDevice bluetoothDevice) {
    mAction = action;
    mBluetoothDevice = bluetoothDevice;
  }

  public String getAction() {
    return mAction;
  }

  public BluetoothDevice getBluetoothDevice() {
    return mBluetoothDevice;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AclEvent that = (AclEvent) o;

    if (mAction != null && !mAction.equals(that.mAction)) return false;
    return !(mBluetoothDevice != null ? !mBluetoothDevice.equals(that.mBluetoothDevice)
        : that.mBluetoothDevice != null);
  }

  @Override public int hashCode() {
    int result = mAction != null ? mAction.hashCode() : 0;
    result = 31 * result + (mBluetoothDevice != null ? mBluetoothDevice.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "AclEvent{" +
        "mAction=" + mAction +
        ", mBluetoothDevice=" + mBluetoothDevice +
        '}';
  }
}
