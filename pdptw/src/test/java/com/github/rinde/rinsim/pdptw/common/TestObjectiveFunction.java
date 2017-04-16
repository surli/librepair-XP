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
package com.github.rinde.rinsim.pdptw.common;

import com.google.common.base.Joiner;

public enum TestObjectiveFunction implements ObjectiveFunction {
  INSTANCE {
    @Override
    public boolean isValidResult(StatisticsDTO stats) {
      return stats.totalParcels == stats.totalDeliveries
        && stats.totalParcels == stats.totalPickups;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
      return stats.totalDistance;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
      return Joiner.on("").join("{dist=", stats.totalDistance, ",parcels=",
        stats.totalParcels, "}");
    }
  }
}
