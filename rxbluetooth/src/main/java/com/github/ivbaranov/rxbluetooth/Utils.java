package com.github.ivbaranov.rxbluetooth;

import java.io.Closeable;
import java.io.IOException;

final class Utils {
  static void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
        // Ignored.
      }
    }
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
