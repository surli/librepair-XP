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
package com.github.rinde.rinsim.scenario.fabrirecht;

import java.io.File;
import java.io.IOException;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

/**
 * Simplest example showing how the Fabri & Recht problem can be configured
 * using a custom vehicle.
 *
 * @author Rinde van Lon
 */
public class RepetitiousGUITest {

  public static void main(String[] args) throws IOException {
    for (int i = 0; i < 100; i++) {
      final FabriRechtScenario scenario = FabriRechtParser.fromJson(Files
        .toString(new File("files/test/fabri-recht/lc101.scenario"),
          Charsets.UTF_8),
        8, 20);

      Simulator.builder()
        .addModel(
          ScenarioController.builder(scenario)
            .withEventHandler(AddVehicleEvent.class,
              new TimedEventHandler<AddVehicleEvent>() {
                @Override
                public void handleTimedEvent(AddVehicleEvent event,
                    SimulatorAPI simulator) {
                  simulator.register(new Truck(event.getVehicleDTO()));
                }
              }))
        .addModel(
          View.builder()
            .with(PlaneRoadModelRenderer.builder())
            .with(RoadUserRenderer.builder())
            .with(PDPModelRenderer.builder())
            .withSpeedUp(50)
            .withAutoClose()
            .withAutoPlay())
        .build().start();
    }
  }
}

/**
 * This truck implementation only picks parcels up, it does not deliver them.
 *
 * @author Rinde van Lon
 */
class Truck extends Vehicle {

  public Truck(VehicleDTO pDto) {
    super(pDto);
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();
    // we always go to the closest available parcel
    final Parcel closest = (Parcel) RoadModels.findClosestObject(
      rm.getPosition(this), rm, new Predicate<RoadUser>() {
        @Override
        public boolean apply(RoadUser input) {
          return input instanceof Parcel
            && pm.getParcelState((Parcel) input) == ParcelState.AVAILABLE;
        }
      });

    if (closest != null) {
      rm.moveTo(this, closest, time);
      if (rm.equalPosition(closest, this)
        && pm.getTimeWindowPolicy().canPickup(closest.getPickupTimeWindow(),
          time.getTime(), closest.getPickupDuration())) {
        pm.pickup(this, closest, time);
      }
    }
  }
}
