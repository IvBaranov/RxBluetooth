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

import android.bluetooth.BluetoothDevice;

/**
 * Event container class.  Contains bond state (whether the device is unbonded, bonding, or bonded),
 * previous bond state, and {@link BluetoothDevice}.
 *
 * Possible state values are:
 * {@link BluetoothDevice#BOND_NONE},
 * {@link BluetoothDevice#BOND_BONDING},
 * {@link BluetoothDevice#BOND_BONDED}
 */
public class BondStateEvent {

  private int mState;
  private int mPreviousState;
  private BluetoothDevice mBluetoothDevice;

  public BondStateEvent(int state, int previousState, BluetoothDevice bluetoothDevice) {
    mState = state;
    mPreviousState = previousState;
    mBluetoothDevice = bluetoothDevice;
  }

  public int getState() {
    return mState;
  }

  public int getPreviousState() {
    return mPreviousState;
  }

  public BluetoothDevice getBluetoothDevice() {
    return mBluetoothDevice;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BondStateEvent that = (BondStateEvent) o;

    if (mState != that.mState) return false;
    if (mPreviousState != that.mPreviousState) return false;
    return !(mBluetoothDevice != null ? !mBluetoothDevice.equals(that.mBluetoothDevice)
        : that.mBluetoothDevice != null);
  }

  @Override public int hashCode() {
    int result = mState;
    result = 31 * result + mPreviousState;
    result = 31 * result + (mBluetoothDevice != null ? mBluetoothDevice.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "BondStateEvent{" +
        "mState=" + mState +
        ", mPreviousState=" + mPreviousState +
        ", mBluetoothDevice=" + mBluetoothDevice +
        '}';
  }
}
