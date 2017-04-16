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

import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.create;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.measure.unit.SI;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class TimeLapseTest {

  @Test
  public void unitConstructor() {
    final TimeLapse tl = new TimeLapse(SI.SECOND, 0, 1);
    assertEquals(0, tl.getTime());
    assertEquals(0, tl.getStartTime());
    assertEquals(1, tl.getEndTime());
    assertEquals(SI.SECOND, tl.getTimeUnit());
  }

  @Test
  public void constructor() {
    final TimeLapse tl = create(0, 10);

    assertEquals(0, tl.getTime());
    assertEquals(0, tl.getTimeConsumed());
    assertEquals(10, tl.getTickLength());
    assertEquals(10, tl.getTimeLeft());
    assertTrue(tl.hasTimeLeft());

  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail1() {
    create(-1, 0);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail2() {
    create(1, 0);
  }

  @Test
  public void consume1() {

    final int[] start = {0, 10, 100, 500};
    final int[] end = {100, 1000, 113, 783};

    for (int i = 0; i < start.length; i++) {
      final TimeLapse tl = create(start[i], end[i]);
      assertEquals(end[i] - start[i], tl.getTimeLeft());
      assertEquals(start[i], tl.getTime());
      assertEquals(0, tl.getTimeConsumed());
      assertTrue(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTickLength());

      tl.consume(10);
      assertEquals(end[i] - start[i] - 10, tl.getTimeLeft());
      assertEquals(start[i] + 10, tl.getTime());
      assertEquals(10, tl.getTimeConsumed());
      assertTrue(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTickLength());

      tl.consumeAll();
      assertEquals(0, tl.getTimeLeft());
      assertEquals(end[i], tl.getTime());
      assertEquals(end[i] - start[i], tl.getTimeConsumed());
      assertFalse(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTickLength());
    }
  }

  /**
   * Tests flyweight implementation.
   */
  @Test
  public void testFlyweight() {
    final TimeLapse tl = create(0, 10);
    assertThat(tl.toString()).isEqualTo("[0,10)");
    assertThat(tl.isIn(7)).isTrue();
    assertThat(tl.isIn(-1)).isFalse();
    assertThat(tl.isIn(0)).isTrue();
    assertThat(tl.isIn(10)).isFalse();
    assertThat(tl.isIn(11)).isFalse();
    tl.consume(7L);
    assertThat(tl.getTimeConsumed()).isEqualTo(7L);
    assertThat(tl.getStartTime()).isEqualTo(0);
    assertThat(tl.getEndTime()).isEqualTo(10L);
    tl.reset();
    assertThat(tl.getTimeConsumed()).isEqualTo(0L);
    assertThat(tl.getStartTime()).isEqualTo(0);
    assertThat(tl.getEndTime()).isEqualTo(10L);

    tl.consume(6L);
    tl.next();
    assertThat(tl.getTimeConsumed()).isEqualTo(0L);
    assertThat(tl.getStartTime()).isEqualTo(10);
    assertThat(tl.getEndTime()).isEqualTo(20L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void consumeFail1() {
    final TimeLapse tl = create(0, 10);
    tl.consume(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void consumeFail2() {
    final TimeLapse tl = create(0, 10);
    tl.consume(11);
  }

}
