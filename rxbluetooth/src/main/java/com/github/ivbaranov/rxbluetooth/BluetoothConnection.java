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

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import com.github.ivbaranov.rxbluetooth.exceptions.ConnectionClosedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import rx.Observable;
import rx.Subscriber;
import rx.observables.StringObservable;

public class BluetoothConnection {

  private static final String TAG = BluetoothConnection.class.getName();

  private BluetoothSocket socket;

  private InputStream inputStream;
  private OutputStream outputStream;

  private Observable<Byte> mObserveInputStream;

  private boolean connected = false;

  /**
   * Container for simplifying read and write from/to {@link BluetoothSocket}.
   *
   * @param socket bluetooth socket
   * @throws Exception if can't get input/output stream from the socket
   */
  public BluetoothConnection(BluetoothSocket socket) throws Exception {
    if (socket == null) {
      throw new InvalidParameterException("Bluetooth socket can't be null");
    }

    this.socket = socket;

    try {
      inputStream = socket.getInputStream();
      outputStream = socket.getOutputStream();

      connected = true;
    } catch (IOException e) {
      throw new Exception("Can't get stream from bluetooth socket");
    } finally {
      if (!connected) {
        closeConnection();
      }
    }
  }

  /**
   * Observes byte from bluetooth's {@link InputStream}. Will be emitted per byte.
   *
   * @return RxJava Observable with {@link Byte}
   */
  public Observable<Byte> observeByteStream() {
    if (mObserveInputStream == null) {
      mObserveInputStream = Observable.create(new Observable.OnSubscribe<Byte>() {
        @Override public void call(Subscriber<? super Byte> subscriber) {
          while (!subscriber.isUnsubscribed()) {
            try {
              subscriber.onNext((byte) inputStream.read());
            } catch (IOException e) {
              connected = false;
              subscriber.onError(new ConnectionClosedException("Can't read stream"));
            } finally {
              if (!connected) {
                closeConnection();
              }
            }
          }
        }
      }).share();
    }

    return mObserveInputStream;
  }

  /**
   * Observes stream of bytes from bluetooth's {@link InputStream}.
   * @param size size of internal buffer to store bytes
   * @return RxJava Observable
   */
  public Observable<byte[]> observeSeveralBytesStream(int size) {
    return StringObservable.from(inputStream, size);
  }

  /**
   * Observes string from bluetooth's {@link InputStream} with '\r' (Carriage Return)
   * and '\n' (New Line) as delimiter.
   *
   * @return RxJava Observable with {@link String}
   */
  public Observable<String> observeStringStream() {
    return observeStringStream('\r', '\n');
  }

  /**
   * Observes string from bluetooth's {@link InputStream}.
   *
   * @param delimiter char(s) used for string delimiter
   * @return RxJava Observable with {@link String}
   */
  public Observable<String> observeStringStream(final int... delimiter) {
    return observeByteStream().lift(new Observable.Operator<String, Byte>() {
      @Override public Subscriber<? super Byte> call(final Subscriber<? super String> subscriber) {
        return new Subscriber<Byte>(subscriber) {
          ArrayList<Byte> buffer = new ArrayList<>();

          @Override public void onCompleted() {
            if (!buffer.isEmpty()) {
              emit();
            }

            if (!subscriber.isUnsubscribed()) {
              subscriber.onCompleted();
            }
          }

          @Override public void onError(Throwable e) {
            if (!buffer.isEmpty()) {
              emit();
            }

            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }

          @Override public void onNext(Byte b) {
            boolean found = false;
            for (int d : delimiter) {
              if (b == d) {
                found = true;
                break;
              }
            }

            if (found) {
              emit();
            } else {
              buffer.add(b);
            }
          }

          private void emit() {
            if (buffer.isEmpty()) {
              if (!subscriber.isUnsubscribed()) {
                subscriber.onNext("");
              }
              return;
            }

            byte[] bArray = new byte[buffer.size()];

            for (int i = 0; i < buffer.size(); i++) {
              bArray[i] = buffer.get(i);
            }

            if (!subscriber.isUnsubscribed()) {
              subscriber.onNext(new String(bArray));
            }
            buffer.clear();
          }
        };
      }
    }).onBackpressureBuffer();
  }

  /**
   * Send one byte to bluetooth output stream.
   *
   * @param oneByte a byte
   * @return true if success, false if there was error occurred or disconnected
   */
  public boolean send(byte oneByte) {
    return send(new byte[] { oneByte });
  }

  /**
   * Send array of bytes to bluetooth output stream.
   *
   * @param bytes data to send
   * @return true if success, false if there was error occurred or disconnected
   */
  public boolean send(byte[] bytes) {
    if (!connected) return false;

    try {
      outputStream.write(bytes);
      outputStream.flush();
      return true;
    } catch (IOException e) {
      // Error occurred. Better to close terminate the connection
      connected = false;
      Log.e(TAG, "Fail to send data");
      return false;
    } finally {
      if (!connected) {
        closeConnection();
      }
    }
  }

  /**
   * Send string of text to bluetooth output stream.
   *
   * @param text text to send
   * @return true if success, false if there was error occurred or disconnected
   */
  public boolean send(String text) {
    byte[] sBytes = text.getBytes();
    return send(sBytes);
  }

  /**
   * Close the streams and socket connection.
   */
  public void closeConnection() {
    try {
      connected = false;

      if (inputStream != null) {
        inputStream.close();
      }

      if (outputStream != null) {
        outputStream.close();
      }

      if (socket != null) {
        socket.close();
      }
    } catch (IOException ignored) {
    }
  }
}
