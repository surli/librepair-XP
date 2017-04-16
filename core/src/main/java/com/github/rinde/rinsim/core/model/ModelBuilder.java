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

import java.io.Serializable;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

/**
 * Interface for builders of {@link Model}s. All implementations <b>must</b> be
 * immutable. An implementation of the {@link ModelBuilder} interface can
 * specify three different properties of the {@link Model} its constructing:
 * <ul>
 * <li>Associated type, as advertised via {@link #getAssociatedType()}</li>
 * <li>Provided types, as advertised via {@link #getProvidingTypes()}</li>
 * <li>Dependent types, as advertised via {@link #getDependencies()}</li>
 * </ul>
 * <p>
 * <b>Associated type</b> Objects (not models) that are added to the simulator
 * with a type that is assignable to the associated type are automatically
 * registered to the model.
 * <p>
 * <b>Provided types</b> These are the types that the model that is constructed
 * by this builder provides to the simulator. Instances of these types should be
 * provided via {@link Model#get(Class)}.
 * <p>
 * <b>Dependent types</b> These are the types that the model that is constructed
 * by this builder depends on. Instances of this type can be requested via a
 * {@link DependencyProvider} that is made available when
 * {@link #build(DependencyProvider)} is called.
 *
 * @author Rinde van Lon
 * @param <T> The model type.
 * @param <U> The associated type.
 * @see AbstractModelBuilder
 */
public interface ModelBuilder<T extends Model<? extends U>, U> {

  /**
   * @return The type parameter of the {@link Model} that is constructed by this
   *         builder.
   */
  Class<U> getAssociatedType();

  /**
   * @return The type of the {@link Model} that is constructed by this builder.
   */
  Class<T> getModelType();

  /**
   * @return A set of types that are provided by the {@link Model}.
   */
  ImmutableSet<Class<?>> getProvidingTypes();

  /**
   * @return A set of types that are dependencies of this {@link Model}.
   */
  ImmutableSet<Class<?>> getDependencies();

  /**
   * Should build the model. The {@link DependencyProvider} allows to request
   * instances with any of the types specified by {@link #getDependencies()}.
   * Each declared dependency <b>must</b> be requested of the
   * {@link DependencyProvider}. References to the {@link DependencyProvider}
   * should not be kept, the provider is <i>guaranteed</i> to be unusable after
   * this method has been invoked.
   * @param dependencyProvider The dependency provider.
   * @return A new {@link Model} instance.
   */
  T build(DependencyProvider dependencyProvider);

  /**
   * Abstract implementation of {@link ModelBuilder} that provides default
   * implementations for all methods except {@link #build(DependencyProvider)}.
   * @author Rinde van Lon
   * @param <T> The model type.
   * @param <U> The associated type.
   */
  abstract class AbstractModelBuilder<T extends Model<? extends U>, U>
      implements ModelBuilder<T, U>, Serializable {
    private static final long serialVersionUID = -1110189591056185881L;
    private ImmutableSet<Class<?>> provTypes;
    private ImmutableSet<Class<?>> deps;
    private final Class<T> modelType;
    private final Class<U> associatedType;

    /**
     * Construct a new instance.
     *
     */
    @SuppressWarnings("unchecked")
    protected AbstractModelBuilder() {
      provTypes = ImmutableSet.of();
      deps = ImmutableSet.of();
      modelType = (Class<T>) new TypeToken<T>(getClass()) {
        private static final long serialVersionUID = -7292931677375164212L;
      }.getRawType();
      associatedType = (Class<U>) new TypeToken<U>(getClass()) {
        private static final long serialVersionUID = 5451272042925600897L;
      }.getRawType();
    }

    /**
     * Sets the providing types, overwrites previous calls.
     * @param types The providing types of this builder, see
     *          {@link ModelBuilder} header comment for more info.
     */
    protected final void setProvidingTypes(Class<?>... types) {
      provTypes = ImmutableSet.copyOf(types);
    }

    /**
     * Sets the providing types, overwrites previous calls.
     * @param types The providing types of this builder, see
     *          {@link ModelBuilder} header comment for more info.
     */
    protected final void setProvidingTypes(Iterable<? extends Class<?>> types) {
      provTypes = ImmutableSet.<Class<?>>copyOf(types);
    }

    /**
     * Sets the dependencies, overwrites previous calls.
     * @param types The dependencies of this builder, see {@link ModelBuilder}
     *          header comment for more info.
     */
    protected final void setDependencies(Class<?>... types) {
      deps = ImmutableSet.copyOf(types);
    }

    /**
     * Sets the dependencies, overwrites previous calls.
     * @param types The dependencies of this builder, see {@link ModelBuilder}
     *          header comment for more info.
     */
    protected final void setDependencies(Iterable<? extends Class<?>> types) {
      deps = ImmutableSet.<Class<?>>copyOf(types);
    }

    @Override
    public final Class<U> getAssociatedType() {
      return associatedType;
    }

    @Override
    public final Class<T> getModelType() {
      return modelType;
    }

    @Override
    public final ImmutableSet<Class<?>> getProvidingTypes() {
      return provTypes;
    }

    @Override
    public final ImmutableSet<Class<?>> getDependencies() {
      return deps;
    }

    @Override
    public String toString() {
      return getModelType().getSimpleName() + ".builder()";
    }
  }
}
