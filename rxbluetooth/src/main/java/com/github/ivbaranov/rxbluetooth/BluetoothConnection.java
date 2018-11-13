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
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableOperator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class BluetoothConnection {

  private static final String TAG = BluetoothConnection.class.getName();

  private BluetoothSocket socket;

  InputStream inputStream;
  private OutputStream outputStream;

  private Flowable<Byte> observeInputStream;

  boolean connected = false;

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
  public Flowable<Byte> observeByteStream() {
    if (observeInputStream == null) {
      observeInputStream = Flowable.create(new FlowableOnSubscribe<Byte>() {
        @Override public void subscribe(final FlowableEmitter<Byte> subscriber) {
          while (!subscriber.isCancelled()) {
            try {
              subscriber.onNext((byte) inputStream.read());
            } catch (IOException e) {
              connected = false;
              subscriber.onError(new ConnectionClosedException("Can't read stream", e));
            } finally {
              if (!connected) {
                closeConnection();
              }
            }
          }
        }
      }, BackpressureStrategy.BUFFER).share();
    }

    return observeInputStream;
  }

  /**
   * Observes string from bluetooth's {@link InputStream} with '\r' (Carriage Return)
   * and '\n' (New Line) as delimiter.
   *
   * @return RxJava Observable with {@link String}
   */
  public Flowable<String> observeStringStream() {
    return observeStringStream('\r', '\n');
  }

  /**
   * Observes string from bluetooth's {@link InputStream}.
   *
   * @param delimiter char(s) used for string delimiter
   * @return RxJava Observable with {@link String}
   */
  public Flowable<String> observeStringStream(final int... delimiter) {
    return observeByteStream().lift(new FlowableOperator<String, Byte>() {
      @Override public Subscriber<? super Byte> apply(final Subscriber<? super String> subscriber) {
        return new Subscriber<Byte>() {
          ArrayList<Byte> buffer = new ArrayList<>();
          List<Integer> receivedDelimiters = new ArrayList<>();

          @Override public void onSubscribe(Subscription d) {
            subscriber.onSubscribe(d);
          }

          @Override public void onComplete() {
            if (!buffer.isEmpty()) {
              emit();
            }
            subscriber.onComplete();
          }

          @Override public void onError(Throwable e) {
            if (!buffer.isEmpty()) {
              emit();
            }
            subscriber.onError(e);
          }

          @Override public void onNext(Byte b) {
            boolean found = false;
            for (int d : delimiter) {
              if (b == d) {
                receivedDelimiters.add((int) b);
                found = true;
                break;
              }
            }

            if (found) {
              if (!delimitersMatched()) {
                emit();
              }
            } else {
              buffer.add(b);
            }
          }

          private void emit() {
            if (buffer.isEmpty()) {
              subscriber.onNext("");
              return;
            }

            byte[] bArray = new byte[buffer.size()];

            for (int i = 0; i < buffer.size(); i++) {
              bArray[i] = buffer.get(i);
            }

            subscriber.onNext(new String(bArray));
            buffer.clear();
            receivedDelimiters.clear();
          }

          /** Returns true if list of received delimiter(s) matched the provided one(s).*/
          private boolean delimitersMatched() {
            int[] array = new int[receivedDelimiters.size()];
            for (int i = 0; i < receivedDelimiters.size(); i++) {
              array[i] = receivedDelimiters.get(i);
            }

            return Arrays.equals(array, delimiter);
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
    connected = false;
    Utils.close(inputStream);
    Utils.close(outputStream);
    Utils.close(socket);
  }
}
