package com.github.ivbaranov.rxbluetooth;

import android.bluetooth.BluetoothAdapter;

/**
 * Thrown when {@link BluetoothAdapter#getProfileProxy} returns true, which means that connection
 * to bluetooth profile failed.
 */
public class GetProfileProxyException extends RuntimeException {

  public GetProfileProxyException() {
    super("Failed to get profile proxy");
  }
}
