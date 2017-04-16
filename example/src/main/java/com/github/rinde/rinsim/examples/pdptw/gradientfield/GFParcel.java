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
package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;

class GFParcel extends Parcel implements FieldEmitter {
  static final float AVAILABLE_STRENGTH = 3.0f;
  private final Point pos;

  GFParcel(ParcelDTO pDto) {
    super(pDto);
    pos = pDto.getPickupLocation();
  }

  @Override
  public void setModel(GradientModel model) {}

  @Override
  public Point getPosition() {
    return pos;
  }

  @Override
  public float getStrength() {
    if (!isInitialized()) {
      return 0f;
    }
    return getPDPModel().getParcelState(this) == ParcelState.AVAILABLE
      ? AVAILABLE_STRENGTH
      : 0.0f;
  }
}
