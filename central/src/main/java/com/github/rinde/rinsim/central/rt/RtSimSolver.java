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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Preconditions.checkState;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * {@link RtSimSolver} is a usage-friendly facade for {@link RealtimeSolver}s.
 * It provides a convenient API to use a {@link RealtimeSolver} inside a
 * simulation.
 * <p>
 * Instances of {@link RtSimSolver} can only be constructed using
 * {@link RtSimSolverBuilder}, which can be obtained via {@link RtSolverModel}.
 * Note that it is not possible to construct your own subclass of
 * {@link RtSimSolver}.
 * @author Rinde van Lon
 */
public abstract class RtSimSolver {

  RtSimSolver() {}

  /**
   * Tells the underlying real-time solver to start computing. If the solver is
   * currently calculating (because it is not yet finished with a previous solve
   * operation) the previous operation will be cancelled and a new calculation
   * will start.
   * @param args Object that allows to specify the information that will be used
   *          by the solver.
   * @throws IllegalStateException If the clock is not in real-time mode at the
   *           time of calling this method.
   */
  public abstract void solve(SolveArgs args);

  public abstract void solve(GlobalStateObject state);

  // this method should generally not be called in the same tick as solve(..)
  // is called
  public abstract void sendSnapshot(SolveArgs args);

  public abstract GlobalStateObject getCurrentState(SolveArgs args);

  /**
   * @return <code>true</code> indicates that a new schedule has been computed
   *         and has not been retrieved yet via a call to
   *         {@link #getCurrentSchedule()}, if the schedule has not been updated
   *         since a previous call to {@link #getCurrentSchedule()} or no
   *         schedule has been computed yet <code>false</code> is returned.
   */
  public abstract boolean isScheduleUpdated();

  /**
   * Retrieves the current schedule, after this method is called
   * {@link #isScheduleUpdated()} always returns <code>false</code>.
   * @return The current schedule.
   * @throws IllegalStateException If no schedule has finished computing yet.
   */
  public abstract ImmutableList<ImmutableList<Parcel>> getCurrentSchedule();

  public abstract boolean isComputing();

  public abstract void cancel();

  public abstract Object getLock();

  /**
   * The event API, the supported event types are documented in
   * {@link EventType}.
   * @return The {@link EventAPI}.
   */
  public abstract EventAPI getEventAPI();

  public static final class SolverEvent extends Event {
    private final Optional<ImmutableList<ImmutableList<Parcel>>> schedule;
    private final Optional<GlobalStateObject> state;

    SolverEvent(EventType type,
        Optional<ImmutableList<ImmutableList<Parcel>>> sched,
        Optional<GlobalStateObject> st) {
      super(type);
      schedule = sched;
      state = st;
    }

    public boolean hasScheduleAndState() {
      return schedule.isPresent() && state.isPresent();
    }

    public ImmutableList<ImmutableList<Parcel>> getSchedule() {
      checkState(hasScheduleAndState());
      return schedule.get();
    }

    public GlobalStateObject getState() {
      checkState(hasScheduleAndState());
      return state.get();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(getClass())
        .add("schedule", schedule)
        .add("state", state)
        .toString();
    }
  }

  /**
   * The event types dispatched by {@link RtSimSolver}.
   * @author Rinde van Lon
   */
  public enum EventType {
    /**
     * This event is dispatched when the solver has computed a new schedule.
     * Right after this event is dispatched
     * {@link RtSimSolver#isScheduleUpdated()} will return <code>true</code>.
     */
    NEW_SCHEDULE,
    /**
     * This event is dispatched when the solver has stopped computing. Right
     * after this event is dispatched {@link RtSimSolver#isComputing()} returns
     * <code>false</code>.
     */
    DONE;
  }
}
