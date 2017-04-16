/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.Comparator;

import javax.annotation.Nullable;

/**
 * An event that is part of a scenario. It has a time at which it should be
 * dispatched. Implementations should be immutable.
 * @author Rinde van Lon
 */
public interface TimedEvent {
  /**
   * @return The time at which the event is dispatched.
   */
  long getTime();

  /**
   * Comparator for comparing {@link TimedEvent}s on their time.
   * @author Rinde van Lon
   */
  enum TimeComparator implements Comparator<TimedEvent> {
    /**
     * Comparator for comparing {@link TimedEvent}s on their time.
     */
    INSTANCE;

    @Override
    public int compare(@Nullable TimedEvent o1, @Nullable TimedEvent o2) {
      return Long.compare(
        verifyNotNull(o1).getTime(),
        verifyNotNull(o2).getTime());
    }
  }
}
