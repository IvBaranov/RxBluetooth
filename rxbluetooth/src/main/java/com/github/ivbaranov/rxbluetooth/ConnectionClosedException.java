package com.github.ivbaranov.rxbluetooth;

import java.io.IOException;

/**
 * Created by resna on 2016-07-02.
 */
public class ConnectionClosedException extends IOException {

    public ConnectionClosedException() {
        super("Connection is closed.");
    }

    public ConnectionClosedException(String message) {
        super(message);
    }
}
