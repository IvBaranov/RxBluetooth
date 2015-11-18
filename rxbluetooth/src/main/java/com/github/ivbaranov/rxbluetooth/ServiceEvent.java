package com.github.ivbaranov.rxbluetooth;

import android.bluetooth.BluetoothProfile;

/**
 * Event container class.  Contains state (whether the service was just connected or
 * disconnected), profile type and {@link BluetoothProfile}. When service state is {@link
 * State#DISCONNECTED} the mBluetoothProfile is null.
 */
public class ServiceEvent {

  public enum State {
    CONNECTED,
    DISCONNECTED
  }

  private State mState;
  private int mProfileType;
  private BluetoothProfile mBluetoothProfile;

  public ServiceEvent(State state, int profileType, BluetoothProfile bluetoothProfile) {
    mState = state;
    mProfileType = profileType;
    mBluetoothProfile = bluetoothProfile;
  }

  public State getState() {
    return mState;
  }

  public int getProfileType() {
    return mProfileType;
  }

  public BluetoothProfile getBluetoothProfile() {
    return mBluetoothProfile;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceEvent that = (ServiceEvent) o;

    if (mProfileType != that.mProfileType) return false;
    if (mState != that.mState) return false;
    return !(mBluetoothProfile != null ? !mBluetoothProfile.equals(that.mBluetoothProfile)
        : that.mBluetoothProfile != null);
  }

  @Override public int hashCode() {
    int result = mState.hashCode();
    result = 31 * result + mProfileType;
    result = 31 * result + (mBluetoothProfile != null ? mBluetoothProfile.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "ServiceEvent{" +
        "mState=" + mState +
        ", mProfileType=" + mProfileType +
        ", mBluetoothProfile=" + mBluetoothProfile +
        '}';
  }
}
