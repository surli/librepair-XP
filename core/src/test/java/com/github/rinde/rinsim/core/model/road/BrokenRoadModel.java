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
package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;

public class BrokenRoadModel extends GraphRoadModelImpl {
  public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
    super(pGraph, RoadModelBuilders.staticGraph(pGraph));
  }

  @Override
  public boolean doRegister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }

  @Override
  public boolean unregister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }
}
