package com.github.ivbaranov.rxbluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class Utils {
  static void close(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
        // Ignored.
      }
    }
  }

  @SuppressWarnings("unchecked") static BluetoothSocket createRfcommSocket(
      final BluetoothDevice device, final int channel) {
    try {
      Method method = BluetoothDevice.class.getMethod("createRfcommSocket", Integer.TYPE);
      return (BluetoothSocket) method.invoke(device, channel);
    } catch (final NoSuchMethodException e) {
      throw new UnsupportedOperationException(e);
    } catch (final InvocationTargetException e) {
      throw new UnsupportedOperationException(e);
    } catch (final IllegalAccessException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
