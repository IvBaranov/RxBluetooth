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
package com.github.ivbaranov.rxbluetooth.predicates;

import android.content.BroadcastReceiver;
import io.reactivex.functions.Predicate;

/**
 * Class that contains predicates for filtering bluetooth states, actions and other indicators
 * received from {@link BroadcastReceiver}.
 */
public final class BtPredicate {
  /**
   * Function, which checks if current object equals single argument or one of many
   * arguments. It can be used inside filter(...) method from RxJava.
   *
   * @param arguments many arguments or single argument
   * @return Predicate function
   */
  public static <T> Predicate<T> in(final T... arguments) {
    return new Predicate<T>() {
      @Override public boolean test(T object) {
        for (T t : arguments) {
          if (t.equals(object)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private BtPredicate() {
    throw new AssertionError("No instances.");
  }
}
