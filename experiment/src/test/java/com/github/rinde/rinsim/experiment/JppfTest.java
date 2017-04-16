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
package com.github.rinde.rinsim.experiment;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.JPPFConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.ScenarioTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.google.common.collect.ImmutableList;

/**
 * Test for JPPF computation.
 * @author Rinde van Lon
 */
public class JppfTest {
  @SuppressWarnings("null")
  static JPPFDriver driver;
  @SuppressWarnings("null")
  static Scenario scenario;

  /**
   * Starts the JPPF driver.
   */
  @BeforeClass
  public static void setUp() {
    JPPFConfiguration.getProperties().setBoolean("jppf.local.node.enabled",
      true);
    JPPFDriver.main(new String[] {"noLauncher"});
    driver = JPPFDriver.getInstance();

    scenario = ScenarioTestUtil.createRandomScenario(123L,
      StatsTracker.builder());
  }

  /**
   * Stops the JPPF driver.
   */
  @AfterClass
  public static void tearDown() {
    driver.shutdown();
  }

  /**
   * Checks determinism of two subsequent identical JPPF experiments.
   */
  @Test
  public void determinismJppfVsJppf() {
    final List<Integer> ints = asList(1, 2, 5, 10);
    final List<ExperimentResults> allResults = newArrayList();

    final Experiment.Builder experimentBuilder = Experiment.builder()
      .computeDistributed()
      .addScenario(scenario)
      .withRandomSeed(123)
      .repeat(10)
      .addConfiguration(ExperimentTestUtil.testConfig("A"));
    for (final int i : ints) {
      allResults.add(
        experimentBuilder.numBatches(i)
          .perform());
    }
    assertEquals(4, allResults.size());
    for (int i = 0; i < allResults.size() - 1; i++) {
      assertEquals(allResults.get(i), allResults.get(i + 1));
    }
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a Gendreau scenario.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void determinismLocalVsJppf() {
    final Experiment.Builder experimentBuilder = Experiment.builder()
      .computeDistributed()
      .addScenario(scenario)
      .withRandomSeed(123)
      .repeat(1)
      .usePostProcessor(ExperimentTestUtil.testPostProcessor())
      .addConfiguration(ExperimentTestUtil.testConfig("A"));

    final ExperimentResults results3 = experimentBuilder.perform();
    experimentBuilder.computeLocal();
    final ExperimentResults results4 = experimentBuilder.perform();
    assertEquals(results3, results4);

    assertThat(results3.getResults().asList().get(0).getResultObject())
      .isInstanceOf(ImmutableList.class);
    assertThat(
      (List<Point>) results3.getResults().asList().get(0).getResultObject())
        .hasSize(10);
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a generated scenario.
   */
  @Test
  public void determinismGeneratedScenarioLocalVsJppf() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final Scenario generatedScenario = ScenarioGenerator
      .builder()
      .addModel(
        PDPRoadModel.builder(
          RoadModelBuilders.plane()
            .withMaxSpeed(20d))
          .withAllowVehicleDiversion(true))
      .addModel(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.LIBERAL))
      .setStopCondition(StatsStopConditions.timeOutEvent())
      .build().generate(rng, "hoi");

    final Experiment.Builder experimentBuilder = Experiment.builder()
      .computeDistributed()
      .addScenario(generatedScenario)
      .withRandomSeed(123)
      .repeat(1)
      .usePostProcessor(ExperimentTestUtil.testPostProcessor())
      .addConfiguration(ExperimentTestUtil.testConfig("A"));

    final ExperimentResults resultsDistributed = experimentBuilder.perform();
    final ExperimentResults resultsLocal = experimentBuilder
      .computeLocal()
      .perform();
    assertEquals(resultsLocal, resultsDistributed);
  }

  /**
   * Tests a post processor that returns objects that does not implement
   * {@link Serializable}.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFaultyPostProcessor() {
    Experiment.builder()
      .computeDistributed()
      .addScenario(scenario)
      .withRandomSeed(123)
      .repeat(1)
      .usePostProcessor(new TestFaultyPostProcessor())
      .addConfiguration(ExperimentTestUtil.testConfig("A"))
      .perform();

  }

  @Test
  public void testRetryPostProcessor() {
    final Experiment.Builder builder = Experiment.builder()
      .addScenario(scenario)
      .computeDistributed()
      .addConfiguration(ExperimentTestUtil.testConfig("test"))
      .usePostProcessor(ExperimentTestUtil.retryOncePostProcessor())
      .repeat(3)
      .withRandomSeed(123);

    final ExperimentResults er = builder.perform();

    for (int i = 0; i < er.getResults().size(); i++) {
      assertThat(er.getResults().asList().get(0).getResultObject())
        .isEqualTo("SUCCESS");
    }
  }

  @Test
  public void composite() {
    final Experiment.Builder builder = Experiment.builder()
      .addScenario(scenario)
      .computeDistributed()
      .setCompositeTaskSize(2)
      .addConfiguration(ExperimentTestUtil.testConfig("test"))
      .usePostProcessor(ExperimentTestUtil.testPostProcessor())
      .repeat(3)
      .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    assertThat(er.getResults()).hasSize(3);
  }

  static class TestFaultyPostProcessor implements
      PostProcessor<NotSerializable>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public NotSerializable collectResults(Simulator sim, SimArgs args) {
      return new NotSerializable();
    }

    @Override
    public FailureStrategy handleFailure(
        Exception e, Simulator sim, SimArgs args) {
      return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }
  }

  static class NotSerializableObjFunc implements ObjectiveFunction {
    @Override
    public boolean isValidResult(StatisticsDTO stats) {
      return true;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
      return 0;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
      return "NotSerializableObjFunc";
    }
  }

  static class NotSerializable {}
}
