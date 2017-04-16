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
package com.github.rinde.rinsim.ui.renderers;

import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ViewRect {

  public final Point min;
  public final Point max;
  public final double width;
  public final double height;

  public ViewRect(Point pMin, Point pMax) {
    min = pMin;
    max = pMax;
    width = max.x - min.x;
    height = max.y - min.y;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("{ViewRect: ").append(min).append(" ")
      .append(max).append("}").toString();
  }
}
