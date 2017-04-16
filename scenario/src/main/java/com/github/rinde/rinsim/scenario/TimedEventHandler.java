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

import com.github.rinde.rinsim.core.SimulatorAPI;

/**
 * Interface for handling scenario events. Typically, implementors of this
 * interface behave like a factory, upon receiving the event they will add a new
 * object to the simulator.
 * @author Rinde van Lon
 * @param <T> The type of TimedEvent that this handler handles.
 */
public interface TimedEventHandler<T extends TimedEvent> {

  /**
   * Should handle the timed event. Typically, a object that corresponds to the
   * event is added to the simulator.
   * @param event The event to handle.
   * @param simulator The simulator.
   */
  void handleTimedEvent(T event, SimulatorAPI simulator);
}
