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
package com.github.rinde.rinsim.core.model;

/**
 * Provider of dependencies. An instance is automatically injected into
 * {@link ModelBuilder#build(DependencyProvider)}.
 * @author Rinde van Lon
 */
public abstract class DependencyProvider {

  DependencyProvider() {}

  /**
   * Retrieves an instance of the specified type.
   * @param type The type to request an instance of.
   * @param <T> The type to request an instance of.
   * @return An instance of the specified type.
   * @throws IllegalArgumentException if the specified type can not be provided.
   */
  public abstract <T> T get(Class<T> type);
}
