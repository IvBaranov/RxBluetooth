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

import android.bluetooth.BluetoothProfile;

/**
 * Event container class.  Contains state (whether the service was just connected or
 * disconnected), profile type and {@link BluetoothProfile}. When service state is {@link
 * State#DISCONNECTED} the bluetoothProfile is null.
 */
public final class ServiceEvent {

  public enum State {
    CONNECTED, DISCONNECTED
  }

  private State state;
  private int profileType;
  private BluetoothProfile bluetoothProfile;

  public ServiceEvent(State state, int profileType, BluetoothProfile bluetoothProfile) {
    this.state = state;
    this.profileType = profileType;
    this.bluetoothProfile = bluetoothProfile;
  }

  public State getState() {
    return state;
  }

  public int getProfileType() {
    return profileType;
  }

  public BluetoothProfile getBluetoothProfile() {
    return bluetoothProfile;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceEvent that = (ServiceEvent) o;

    if (profileType != that.profileType) return false;
    if (state != that.state) return false;
    return !(bluetoothProfile != null ? !bluetoothProfile.equals(that.bluetoothProfile)
        : that.bluetoothProfile != null);
  }

  @Override public int hashCode() {
    int result = state.hashCode();
    result = 31 * result + profileType;
    result = 31 * result + (bluetoothProfile != null ? bluetoothProfile.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "ServiceEvent{" +
        "state=" + state +
        ", profileType=" + profileType +
        ", bluetoothProfile=" + bluetoothProfile +
        '}';
  }
}
