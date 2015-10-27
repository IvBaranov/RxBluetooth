package com.github.ivbaranov.rxbluetooth;

import android.bluetooth.BluetoothAdapter;
import rx.functions.Func1;

public enum DiscoveryState {
  DISCOVERY_STARTED(BluetoothAdapter.ACTION_DISCOVERY_STARTED),
  DISCOVERY_FINISHED(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

  private final String state;

  DiscoveryState(String state) {
    this.state = state;
  }

  @Override public String toString() {
    return state;
  }

  /**
   * Creates a function, which checks
   * if single discovery state or many states
   * are equal to current state. It can be used inside filter(...)
   * method from RxJava
   *
   * @param states many discovery states or single state
   * @return {@link Func1} checking function
   */
  public static Func1<DiscoveryState, Boolean> isEqualTo(final DiscoveryState... states) {
    return new Func1<DiscoveryState, Boolean>() {
      @Override public Boolean call(DiscoveryState discoveryState) {
        boolean statesAreEqual = false;

        for (DiscoveryState singleStatus : states) {
          statesAreEqual = singleStatus == discoveryState;
        }

        return statesAreEqual;
      }
    };
  }

  /**
   * Creates a function, which checks
   * if single discovery state or many states
   * are not equal to current state. It can be used inside filter(...)
   * method from RxJava
   *
   * @param states many discovery states or single state
   * @return {@link Func1} checking function
   */
  public static Func1<DiscoveryState, Boolean> isNotEqualTo(final DiscoveryState... states) {
    return new Func1<DiscoveryState, Boolean>() {
      @Override public Boolean call(DiscoveryState discoveryState) {
        boolean statesAreNotEqual = false;

        for (DiscoveryState singleStatus : states) {
          statesAreNotEqual = singleStatus != discoveryState;
        }

        return statesAreNotEqual;
      }
    };
  }
}
