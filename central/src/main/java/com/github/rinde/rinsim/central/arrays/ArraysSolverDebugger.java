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
package com.github.rinde.rinsim.central.arrays;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.out;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.deepToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.central.arrays.ArraysSolvers.ArraysObject;
import com.github.rinde.rinsim.central.arrays.ArraysSolvers.MVArraysObject;

/**
 * A {@link SingleVehicleArraysSolver} wrapper that adds debugging facilities. A
 * history is kept of all inputs and outputs and all inputs can optionally be
 * printed to sys.out.
 * @author Rinde van Lon
 */
public final class ArraysSolverDebugger {

  private ArraysSolverDebugger() {}

  /**
   * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
   * debugging. Every invocation of
   * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   * all inputs and outputs are printed to <code>System.out</code>, also all
   * inputs and outputs are stored (accessible via
   * {@link SVASDebugger#getInputs()} and {@link SVASDebugger#getOutputs()}.
   * @param s The {@link SingleVehicleArraysSolver} to wrap.
   * @return The wrapped solver.
   */
  public static SVASDebugger wrap(SingleVehicleArraysSolver s) {
    return new SVASDebugger(s, true);
  }

  /**
   * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
   * debugging. Stores all invocation arguments and outputs and optionally
   * prints them to <code>System.out</code>.
   * @param s The {@link SingleVehicleArraysSolver} to wrap.
   * @param print If <code>true</code> all information will be printed as well.
   * @return The wrapped solver.
   */
  public static SVASDebugger wrap(SingleVehicleArraysSolver s, boolean print) {
    return new SVASDebugger(s, print);
  }

  /**
   * Wraps the specified {@link MultiVehicleArraysSolver} to allow easy
   * debugging. Stores all invocation arguments and outputs and optionally
   * prints them to <code>System.out</code>.
   * @param s The {@link MultiVehicleArraysSolver} to wrap.
   * @param print If <code>true</code> all information will be printed as well.
   * @return The wrapped solver.
   */
  public static MVASDebugger wrap(MultiVehicleArraysSolver s, boolean print) {
    return new MVASDebugger(s, print);
  }

  static String fix(String s) {
    return s.replace('[', '{').replace(']', '}') + ";";
  }

  private static class Debugger<I extends ArraysObject, O> {
    protected final List<I> inputMemory;
    protected final List<O> outputMemory;
    protected final boolean print;

    Debugger(boolean printing) {
      print = printing;
      inputMemory = newArrayList();
      outputMemory = newArrayList();
    }

    /**
     * Clears the memory.
     */
    public void flush() {
      inputMemory.clear();
      outputMemory.clear();
    }

    /**
     * @return An unmodifiable list with an {@link ArraysObject} in invocation
     *         order for every invocation of <code>solve(..)</code>.
     */
    public List<I> getInputs() {
      return Collections.unmodifiableList(inputMemory);
    }

    /**
     * @return An unmodifiable list with an {@link SolutionObject} in invocation
     *         order for every invocation of <code>solve(..)</code>.
     */
    public List<O> getOutputs() {
      return Collections.unmodifiableList(outputMemory);
    }
  }

  /**
   * Debugger for {@link SingleVehicleArraysSolver}s.
   * @author Rinde van Lon
   */
  public static final class SVASDebugger
      extends Debugger<ArraysObject, SolutionObject>
      implements SingleVehicleArraysSolver {
    private final SingleVehicleArraysSolver solver;

    SVASDebugger(SingleVehicleArraysSolver s, boolean print) {
      super(print);
      solver = s;
    }

    @Override
    public SolutionObject solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        @Nullable SolutionObject currentSolution) {

      inputMemory.add(new ArraysObject(travelTime, releaseDates, dueDates,
        servicePairs, serviceTimes, currentSolution == null ? null
          : new SolutionObject[] {currentSolution}));
      if (print) {
        out.println("int[][] travelTime = " + fix(deepToString(travelTime)));
        out.println("int[] releaseDates = "
          + fix(Arrays.toString(releaseDates)));
        out.println("int[] dueDates = " + fix(Arrays.toString(dueDates)));
        out.println(
          "int[][] servicePairs = " + fix(deepToString(servicePairs)));
        out.println(
          "int[] serviceTime = " + fix(Arrays.toString(serviceTimes)));
      }

      final long start = System.currentTimeMillis();
      final SolutionObject sol = solver.solve(travelTime, releaseDates,
        dueDates, servicePairs, serviceTimes, currentSolution);
      if (print) {
        out.println(System.currentTimeMillis() - start + "ms");
        out.println("route: " + Arrays.toString(sol.route));
        out.println("arrivalTimes: " + Arrays.toString(sol.arrivalTimes));
        out.println("objectiveValue: " + sol.objectiveValue);
      }

      outputMemory
        .add(new SolutionObject(copyOf(sol.route, sol.route.length), copyOf(
          sol.arrivalTimes, sol.arrivalTimes.length), sol.objectiveValue));

      int totalTravelTime = 0;
      for (int i = 1; i < travelTime.length; i++) {
        totalTravelTime += travelTime[sol.route[i - 1]][sol.route[i]];
      }
      if (print) {
        out.println("travel time :  " + totalTravelTime);
      }
      return sol;
    }
  }

  /**
   * Debugger for {@link MultiVehicleArraysSolver}s.
   * @author Rinde van Lon
   */
  public static final class MVASDebugger
      extends Debugger<MVArraysObject, SolutionObject[]>
      implements MultiVehicleArraysSolver {

    private final MultiVehicleArraysSolver solver;

    MVASDebugger(MultiVehicleArraysSolver s, boolean print) {
      super(print);
      solver = s;
    }

    @Override
    public SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        int[][] vehicleTravelTimes, int[][] inventories,
        int[] remainingServiceTimes, int[] currentDestinations,
        @Nullable SolutionObject[] currentSolutions) {

      inputMemory.add(new MVArraysObject(travelTime, releaseDates, dueDates,
        servicePairs, serviceTimes, vehicleTravelTimes, inventories,
        remainingServiceTimes, currentDestinations, currentSolutions));

      final SolutionObject[] output = solver.solve(travelTime, releaseDates,
        dueDates, servicePairs, serviceTimes, vehicleTravelTimes,
        inventories, remainingServiceTimes, currentDestinations,
        currentSolutions);
      outputMemory.add(output);
      return output;
    }
  }
}
