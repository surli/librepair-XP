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
package com.github.rinde.rinsim.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.measure.unit.NonSI;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.core.model.time.TimeModel.Builder;
import com.github.rinde.rinsim.event.ListenerEventHistory;

/**
 * @author Rinde van Lon
 *
 */
public class SimulatedTimeModelTest extends TimeModelTest<SimulatedTimeModel> {

  /**
   * @param sup The supplier to use for creating model instances.
   */
  public SimulatedTimeModelTest(Builder sup) {
    super(sup);
  }

  /**
   * @return The models to test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
      {TimeModel.builder()},
      {TimeModel.builder().withTickLength(333L).withTimeUnit(NonSI.HOUR)}
    });
  }

  /**
   * Test starting and stopping time.
   */
  @Test
  public void testStartStop() {
    final LimitingTickListener ltl = new LimitingTickListener(getModel(), 3);
    final ListenerEventHistory leh = new ListenerEventHistory();

    getModel().getEventAPI().addListener(leh, ClockEventType.values());
    getModel().register(ltl);
    getModel().start();
    assertEquals(3 * getModel().getTickLength(), getModel().getCurrentTime());

    getModel().start();

    assertEquals(6 * getModel().getTickLength(), getModel().getCurrentTime());
    assertThat(leh.getEventTypeHistory()).isEqualTo(
      asList(
        ClockEventType.STARTED,
        ClockEventType.STOPPED,
        ClockEventType.STARTED,
        ClockEventType.STOPPED));
  }

  /**
   * Tests that single ticks works.
   */
  @Test
  public void testSingleTick() {
    final TickListenerChecker checker = new TickListenerChecker(
      getModel().getTickLength(), getModel().getTimeUnit());
    getModel().register(checker);
    assertThat(getModel().getCurrentTime()).isEqualTo(0L);
    getModel().tick();
    assertThat(getModel().getCurrentTime())
      .isEqualTo(getModel().getTickLength());
    assertThat(checker.getTickCount()).isEqualTo(1);
  }

  /**
   * Test for provided types.
   */
  @Test
  public void testProvidingTypes() {
    assertThat(getModel().get(Clock.class)).isNotNull();
    assertThat(getModel().get(ClockController.class)).isNotNull();

    boolean fail = false;
    try {
      getModel().get(RealtimeClockController.class);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "does not provide instances of com.github.rinde.rinsim.core.model.time.RealtimeClockController");
    }
    assertThat(fail).isTrue();
  }

  /**
   * Tests that the time model correctly stops when receiving an interrupt.
   */
  @Test
  public void testInterrupt() {
    final Thread main = Thread.currentThread();
    Executors.newScheduledThreadPool(1).schedule(new Runnable() {
      @Override
      public void run() {
        main.interrupt();
      }
    }, 100, TimeUnit.MILLISECONDS);

    getModel().start();

    // the actual time is hardware dependent as it depends on how many ticks are
    // computed in the period before the interrupt is received
    assertThat(getModel().getCurrentTime()).isGreaterThan(0L);
  }
}
