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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.util.CategoryMap;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Assumptions of the model, any vehicle can pickup any (kind of) parcel (as
 * long as size constraints are met).
 *
 * Currently supports three kinds of objects:
 * <ul>
 * <li>{@link Parcel}</li>
 * <li>{@link Vehicle}</li>
 * <li>{@link Depot}</li>
 * </ul>
 *
 * A parcel must be in one of three locations: on a vehicle, in a depot or on a
 * road (roadmodel).
 *
 * Variable pickup and delivery times are supported. Even when pickup time spans
 * multiple simulation ticks, the {@link DefaultPDPModel} ensures time
 * consistency.
 *
 * TODO write more about assumptions in model <br>
 * TODO write about extensibility
 *
 *
 *
 * {@link Parcel}s can be added on any node in the {@link RoadModel}.
 * {@link Depot}s have no real function in the current implementation.
 *
 *
 *
 * @author Rinde van Lon
 */
public final class DefaultPDPModel extends PDPModel {

  /**
   * The {@link EventDispatcher} used for generating events.
   */
  protected final EventDispatcher eventDispatcher;

  /**
   * Reference to the {@link RoadModel} on which the pdp objects are situated.
   */
  protected final RoadModel roadModel;

  /**
   * Multimap for keeping references to the contents of {@link Container}s.
   */
  protected final Multimap<Container, Parcel> containerContents;

  /**
   * Map for keeping the size of the contents of {@link Container}s.
   */
  protected final Map<Container, Double> containerContentsSize;

  /**
   * Map for keeping the capacity of {@link Container}s.
   */
  protected final Map<Container, Double> containerCapacities;

  /**
   * Map that stores the state of {@link Vehicle}s.
   */
  protected volatile Map<Vehicle, VehicleState> vehicleState;

  /**
   * Map that stores the state of {@link Parcel}s.
   */
  protected volatile CategoryMap<ParcelState, Parcel> parcelState;

  /**
   * The current time.
   */
  protected long currentTime;

  /**
   * The {@link TimeWindowPolicy} that is used.
   */
  protected final TimeWindowPolicy timeWindowPolicy;

  /**
   * Map that stores any pending {@link Action}s of {@link Vehicle}s.
   */
  final Map<Vehicle, Action> pendingVehicleActions;

  /**
   * Initializes the PDPModel.
   * @param twp The {@link TimeWindowPolicy} which is used in the model.
   */
  DefaultPDPModel(RoadModel rm, TimeWindowPolicy twp) {
    timeWindowPolicy = twp;

    containerContents = LinkedHashMultimap.create();
    containerContentsSize = newLinkedHashMap();
    containerCapacities = newLinkedHashMap();
    pendingVehicleActions = newLinkedHashMap();
    vehicleState = newLinkedHashMap();
    parcelState = CategoryMap.create();

    eventDispatcher = new EventDispatcher(PDPModelEventType.values());
    roadModel = rm;
  }

  @Override
  public ImmutableSet<Parcel> getContents(Container container) {
    synchronized (this) {
      checkArgument(containerCapacities.containsKey(container));
      return ImmutableSet.copyOf(containerContents.get(container));
    }
  }

  @Override
  public double getContentsSize(Container container) {
    synchronized (this) {
      return containerContentsSize.get(container);
    }
  }

  @Override
  public double getContainerCapacity(Container container) {
    synchronized (this) {
      return containerCapacities.get(container);
    }
  }

  @Override
  public void pickup(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    synchronized (this) {
      /* 1 */checkVehicleInRoadModel(vehicle);
      /* 2 */checkArgument(roadModel.containsObject(parcel),
        "parcel does not exist in RoadModel");
      final ParcelState ps = parcelState.getKeys(parcel);
      /* 3 */checkArgument(
        ps == ParcelState.AVAILABLE || ps == ParcelState.ANNOUNCED,
        "Parcel must be registered and must be either ANNOUNCED or AVAILABE, "
          + "it is: %s. Parcel: %s.",
        ps, parcel);
      /* 4 */checkArgument(vehicleState.get(vehicle) == VehicleState.IDLE,
        "vehicle must be registered and must be available");
      /* 5 */checkArgument(roadModel.equalPosition(vehicle, parcel),
        "vehicle must be at the same location as the parcel it wishes to "
          + "pickup");
      final double newSize = containerContentsSize.get(vehicle)
        + parcel.getNeededCapacity();
      /* 6 */checkArgument(
        newSize <= containerCapacities.get(vehicle),
        "parcel does not fit in vehicle. Parcel size: %s, current contents "
          + "size: %s, capacity: %s.",
        parcel.getNeededCapacity(), containerContentsSize.get(vehicle),
        containerCapacities.get(vehicle));

      checkArgument(
        timeWindowPolicy.canPickup(parcel.getPickupTimeWindow(),
          time.getTime(), parcel.getPickupDuration()),
        "parcel pickup is not allowed according to the time window policy: "
          + "%s, current time: %s, time window %s.",
        timeWindowPolicy, time.getTime(), parcel.getPickupTimeWindow());

      checkArgument(parcel.canBePickedUp(vehicle, time.getTime()),
        "the parcel does not allow pickup now");

      eventDispatcher
        .dispatchEvent(new PDPModelEvent(PDPModelEventType.START_PICKUP,
          self, time.getTime(), parcel, vehicle));

      LOGGER.debug("{} {} starts picking up {}", time, vehicle, parcel);

      // remove the parcel such that no other attempts to pickup can be made
      roadModel.removeObject(parcel);

      // in this case we know we cannot finish this action with the
      // available time. We must continue in the next tick.
      if (time.getTimeLeft() < parcel.getPickupDuration()) {
        vehicleState.put(vehicle, VehicleState.PICKING_UP);
        parcelState.put(ParcelState.PICKING_UP, parcel);

        pendingVehicleActions.put(vehicle, new PickupAction(this, vehicle,
          parcel, parcel.getPickupDuration() - time.getTimeLeft()));
        time.consumeAll();
      } else {
        time.consume(parcel.getPickupDuration());
        doPickup(vehicle, parcel, time.getTime());
      }
    }
  }

  /**
   * Checks whether the vehicle exists in the RoadModel.
   *
   * @param vehicle The vehicle to check.
   */
  protected void checkVehicleInRoadModel(Vehicle vehicle) {
    checkArgument(roadModel.containsObject(vehicle),
      "vehicle does not exist in RoadModel");
  }

  /**
   * Actual pickup, updates the {@link Vehicle} contents.
   *
   * @param vehicle The {@link Vehicle} that performs the pickup.
   * @param parcel The {@link Parcel} that is picked up.
   * @param time The current time.
   * @see #pickup(Vehicle, Parcel, TimeLapse)
   */
  protected void doPickup(Vehicle vehicle, Parcel parcel, long time) {
    synchronized (this) {
      containerContents.put(vehicle, parcel);
      containerContentsSize.put(vehicle, containerContentsSize.get(vehicle)
        + parcel.getNeededCapacity());

      parcelState.put(ParcelState.IN_CARGO, parcel);
      LOGGER.info("{} end pickup of {} by {}", time, parcel, vehicle);
      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.END_PICKUP, self, time, parcel, vehicle));
    }
  }

  void checkVehicleIdle(Vehicle vehicle) {
    checkArgument(vehicleState.get(vehicle).equals(VehicleState.IDLE),
      "Vehicle must be idle but is: %s ", vehicleState.get(vehicle));
  }

  void checkVehicleDoesNotContainParcel(Vehicle vehicle, Parcel parcel) {
    checkArgument(containerContents.get(vehicle).contains(parcel),
      "vehicle does not contain parcel");
  }

  @Override
  public void deliver(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    synchronized (this) {
      /* 1 */checkVehicleInRoadModel(vehicle);
      /* 2 */checkVehicleIdle(vehicle);
      /* 3 */checkVehicleDoesNotContainParcel(vehicle, parcel);
      /* 4 */checkArgument(
        parcel.getDeliveryLocation().equals(roadModel.getPosition(vehicle)),
        "parcel must be delivered at its destination, vehicle should move "
          + "there first");

      checkArgument(
        timeWindowPolicy.canDeliver(parcel.getDeliveryTimeWindow(),
          time.getTime(), parcel.getDeliveryDuration()),
        "parcel delivery is not allowed at this time (%s) according to the "
          + "time window policy: %s",
        time.getTime(), timeWindowPolicy);

      checkArgument(parcel.canBeDelivered(vehicle, time.getTime()),
        "the parcel does not allow a delivery now");

      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.START_DELIVERY, self, time.getTime(), parcel,
        vehicle));

      LOGGER.debug("{} {} starts delivering {}", time, vehicle, parcel);
      if (time.getTimeLeft() < parcel.getDeliveryDuration()) {
        vehicleState.put(vehicle, VehicleState.DELIVERING);
        parcelState.put(ParcelState.DELIVERING, parcel);
        pendingVehicleActions.put(vehicle, new DeliverAction(this, vehicle,
          parcel, parcel.getDeliveryDuration() - time.getTimeLeft()));
        time.consumeAll();
      } else {
        time.consume(parcel.getDeliveryDuration());
        doDeliver(vehicle, parcel, time.getTime());
      }
    }
  }

  /**
   * The actual delivery of the specified {@link Parcel} by the specified
   * {@link Vehicle}.
   *
   * @param vehicle The {@link Vehicle} that performs the delivery.
   * @param parcel The {@link Parcel} that is delivered.
   * @param time The current time.
   */
  protected void doDeliver(Vehicle vehicle, Parcel parcel, long time) {
    synchronized (this) {
      containerContents.remove(vehicle, parcel);
      containerContentsSize.put(vehicle, containerContentsSize.get(vehicle)
        - parcel.getNeededCapacity());

      parcelState.put(ParcelState.DELIVERED, parcel);
      LOGGER.info("{} end delivery of {} by {}", time, parcel, vehicle);
      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.END_DELIVERY, self, time, parcel, vehicle));
    }
  }

  @Override
  public void drop(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    synchronized (this) {
      /* 1 */checkVehicleInRoadModel(vehicle);
      /* 2 */checkVehicleIdle(vehicle);
      /* 3 */checkVehicleDoesNotContainParcel(vehicle, parcel);

      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.START_DELIVERY, self, time.getTime(), parcel,
        vehicle));
      if (time.getTimeLeft() < parcel.getDeliveryDuration()) {
        vehicleState.put(vehicle, VehicleState.DELIVERING);
        parcelState.put(ParcelState.DELIVERING, parcel);
        pendingVehicleActions.put(vehicle, new DropAction(this, vehicle,
          parcel, parcel.getDeliveryDuration() - time.getTimeLeft()));
        time.consumeAll();
      } else {
        time.consume(parcel.getDeliveryDuration());
        doDrop(vehicle, parcel, time.getTime());
      }
    }
  }

  /**
   * The actual dropping of the specified {@link Parcel} by the specified
   * {@link Vehicle}.
   *
   * @param vehicle The {@link Vehicle} that performs the dropping.
   * @param parcel The {@link Parcel} that is dropped.
   * @param time The current time.
   */
  protected void doDrop(Vehicle vehicle, Parcel parcel, long time) {
    synchronized (this) {
      containerContents.remove(vehicle, parcel);
      containerContentsSize.put(vehicle, containerContentsSize.get(vehicle)
        - parcel.getNeededCapacity());
      roadModel.addObjectAtSamePosition(parcel, vehicle);
      parcelState.put(ParcelState.AVAILABLE, parcel);
      LOGGER.info("{} dropped {} by {}", time, parcel, vehicle);
      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.PARCEL_AVAILABLE, self, time, parcel, null));
    }
  }

  @Override
  public void addParcelIn(Container container, Parcel parcel) {
    synchronized (this) {
      /* 1 */checkArgument(!roadModel.containsObject(parcel),
        "this parcel is already added to the roadmodel");
      /* 2 */checkArgument(
        parcelState.getKeys(parcel) == ParcelState.AVAILABLE,
        "parcel must be registered and in AVAILABLE state, current state: %s",
        parcelState.getKeys(parcel));
      /* 3 */checkArgument(containerCapacities.containsKey(container),
        "the parcel container is not registered");
      /* 4 */checkArgument(roadModel.containsObject(container),
        "the parcel container is not on the roadmodel");
      final double newSize = containerContentsSize.get(container)
        + parcel.getNeededCapacity();
      /* 5 */checkArgument(
        newSize <= containerCapacities.get(container),
        "parcel does not fit in container. Capacity is %s, current content "
          + "size is %s, new parcel size is %s",
        containerCapacities.get(container),
        containerContentsSize.get(container), parcel.getNeededCapacity());

      containerContents.put(container, parcel);
      containerContentsSize.put(container, newSize);
      parcelState.put(ParcelState.IN_CARGO, parcel);
    }
  }

  @Override
  public Collection<Parcel> getParcels(ParcelState state) {
    synchronized (this) {
      return parcelState.get(state);
    }
  }

  @Override
  public Collection<Parcel> getParcels(ParcelState... states) {
    synchronized (this) {
      return parcelState.getMultiple(states);
    }
  }

  @Override
  public Set<Vehicle> getVehicles() {
    synchronized (this) {
      return unmodifiableSet(vehicleState.keySet());
    }
  }

  @Override
  public ParcelState getParcelState(Parcel parcel) {
    synchronized (this) {
      return parcelState.getKeys(parcel);
    }
  }

  @Override
  public VehicleState getVehicleState(Vehicle vehicle) {
    synchronized (this) {
      checkArgument(vehicleState.containsKey(vehicle),
        "vehicle must be registered");
      return vehicleState.get(vehicle);
    }
  }

  // TODO create a similar method but with a parcel as key
  @Override
  public PDPModel.VehicleParcelActionInfo getVehicleActionInfo(
      Vehicle vehicle) {
    synchronized (this) {
      final VehicleState state = vehicleState.get(vehicle);
      checkArgument(
        state == VehicleState.DELIVERING || state == VehicleState.PICKING_UP,
        "the vehicle must be in either DELIVERING or PICKING_UP state, "
          + "but it is %s.",
        state);
      return (PDPModel.VehicleParcelActionInfo) pendingVehicleActions
        .get(vehicle);
    }
  }

  @Override
  protected boolean doRegister(PDPObject element) {
    LOGGER.info("{} register {}", currentTime, element);
    if (element.getType() == PDPType.PARCEL) {
      checkArgument(!parcelState.containsValue(element));
      final Parcel p = (Parcel) element;
      final ParcelState state = currentTime < p.getPickupTimeWindow().begin()
        ? ParcelState.ANNOUNCED
        : ParcelState.AVAILABLE;
      synchronized (this) {
        parcelState.put(state, (Parcel) element);
      }
      eventDispatcher.dispatchEvent(new PDPModelEvent(
        PDPModelEventType.NEW_PARCEL, self, currentTime, p, null));
      // if the parcel is immediately available, we send this event as well
      if (state == ParcelState.AVAILABLE) {
        eventDispatcher.dispatchEvent(new PDPModelEvent(
          PDPModelEventType.PARCEL_AVAILABLE, self, currentTime, p, null));
      }
    } else {
      // it is a vehicle or a depot
      final Container container = (Container) element;
      synchronized (this) {
        checkArgument(!containerCapacities.containsKey(container));
        containerCapacities.put(container, container.getCapacity());
        containerContentsSize.put(container, 0d);
        if (element.getType() == PDPType.VEHICLE) {
          final Vehicle v = (Vehicle) element;
          vehicleState.put(v, VehicleState.IDLE);
        }
      }

      if (element.getType() == PDPType.VEHICLE) {
        eventDispatcher.dispatchEvent(new PDPModelEvent(
          PDPModelEventType.NEW_VEHICLE, self, currentTime, null,
          (Vehicle) element));
      }
    }
    element.initPDPObject(self);

    return true;
  }

  @Override
  public boolean unregister(PDPObject element) {
    synchronized (this) {
      LOGGER.info("unregister {}", element);
      if (element instanceof Container) {
        containerCapacities.remove(element);
        containerContentsSize.remove(element);
        containerContents.removeAll(element);
      }

      if (element instanceof Parcel) {
        parcelState.removeValue((Parcel) element);
      }

      if (element instanceof Vehicle) {
        vehicleState.remove(element);
        pendingVehicleActions.remove(element);
      }
    }
    return true;
  }

  @Override
  public EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  @Override
  public boolean containerContains(Container container, Parcel parcel) {
    synchronized (this) {
      return containerContents.containsEntry(container, parcel);
    }
  }

  @Override
  protected void continuePreviousActions(Vehicle vehicle, TimeLapse time) {
    synchronized (this) {
      if (pendingVehicleActions.containsKey(vehicle)) {
        final Action action = pendingVehicleActions.get(vehicle);
        action.perform(time);
        if (action.isDone()) {
          pendingVehicleActions.remove(vehicle);
          checkState(!pendingVehicleActions.containsKey(vehicle));
          checkState(vehicleState.get(vehicle) == VehicleState.IDLE);
        }
      }
    }
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    synchronized (this) {
      // TODO this can be optimized by scheduling events upon registering
      currentTime = timeLapse.getStartTime();
      final Collection<Parcel> parcels = parcelState.get(ParcelState.ANNOUNCED);
      final List<Parcel> newAvailables = newArrayList();
      for (final Parcel p : parcels) {
        if (timeLapse.getStartTime() >= p.getPickupTimeWindow().begin()) {
          newAvailables.add(p);
        }
      }
      for (final Parcel p : newAvailables) {
        parcelState.put(ParcelState.AVAILABLE, p);
        eventDispatcher.dispatchEvent(new PDPModelEvent(
          PDPModelEventType.PARCEL_AVAILABLE, self, currentTime, p, null));
      }
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public TimeWindowPolicy getTimeWindowPolicy() {
    return timeWindowPolicy;
  }

  @Override
  public void service(Vehicle vehicle, Parcel parcel, TimeLapse time) {
    if (getContents(vehicle).contains(parcel)) {
      deliver(vehicle, parcel, time);
    } else {
      pickup(vehicle, parcel, time);
    }
  }

  @Override
  @Nonnull
  public <U> U get(Class<U> type) {
    synchronized (this) {
      return type.cast(self);
    }
  }

  /**
   * @return A new {@link Builder} instance for creating {@link DefaultPDPModel}
   *         s.
   */
  public static Builder builder() {
    return new AutoValue_DefaultPDPModel_Builder(TimeWindowPolicies.LIBERAL);
  }

  /**
   * Represents an action that takes time. This is used for actions that can not
   * be done at once (since there is not enough time available), using this
   * interface actions can be performed in steps.
   *
   * @author Rinde van Lon
   */
  interface Action {
    /**
     * Performs the action using the specified amount of time.
     *
     * @param time The time to use.
     */
    void perform(TimeLapse time);

    /**
     * @return <code>true</code> when this action is completed,
     *         <code>false</code> otherwise.
     */
    boolean isDone();
  }

  abstract static class VehicleParcelAction implements Action,
      PDPModel.VehicleParcelActionInfo {
    protected final DefaultPDPModel modelRef;
    protected final Vehicle vehicle;
    protected final Parcel parcel;
    protected long timeNeeded;

    VehicleParcelAction(DefaultPDPModel model, Vehicle v, Parcel p,
        long pTimeNeeded) {
      modelRef = model;
      vehicle = v;
      parcel = p;
      timeNeeded = pTimeNeeded;
    }

    @Override
    public void perform(TimeLapse time) {
      // there is enough time to finish action
      if (time.getTimeLeft() >= timeNeeded) {
        time.consume(timeNeeded);
        timeNeeded = 0;
        finish(time);
      } else {
        // there is not enough time to finish action in this step
        timeNeeded -= time.getTimeLeft();
        time.consumeAll();
      }
    }

    protected abstract void finish(TimeLapse time);

    @Override
    public boolean isDone() {
      return timeNeeded == 0;
    }

    @Override
    public long timeNeeded() {
      return timeNeeded;
    }

    @Override
    public Parcel getParcel() {
      return parcel;
    }

    @Override
    public Vehicle getVehicle() {
      return vehicle;
    }
  }

  /**
   * A builder for creating {@link DefaultPDPModel}s. Instances can be obtained
   * via {@link DefaultPDPModel#builder()}.
   *
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<DefaultPDPModel, PDPObject>
      implements Serializable {

    private static final long serialVersionUID = 165944940216903075L;

    Builder() {
      setProvidingTypes(PDPModel.class);
      setDependencies(RoadModel.class);
    }

    /**
     * @return The {@link TimeWindowPolicy}.
     */
    public abstract TimeWindowPolicy getPolicy();

    /**
     * Returns a copy of this builder with the specified
     * {@link TimeWindowPolicy}. The default policy is
     * {@link TimeWindowPolicies#LIBERAL}.
     * @param p The {@link TimeWindowPolicy} which will be used in the model.
     * @return A new {@link Builder} instance.
     */
    @CheckReturnValue
    public Builder withTimeWindowPolicy(TimeWindowPolicy p) {
      return new AutoValue_DefaultPDPModel_Builder(p);
    }

    @Override
    public DefaultPDPModel build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);
      return new DefaultPDPModel(rm, getPolicy());
    }
  }

  static class PickupAction extends VehicleParcelAction {
    PickupAction(DefaultPDPModel model, Vehicle v, Parcel p, long pTimeNeeded) {
      super(model, v, p, pTimeNeeded);
    }

    @Override
    public void finish(TimeLapse time) {
      modelRef.vehicleState.put(vehicle, VehicleState.IDLE);
      modelRef.doPickup(vehicle, parcel, time.getTime());
    }
  }

  static class DropAction extends VehicleParcelAction {
    DropAction(DefaultPDPModel model, Vehicle v, Parcel p, long pTimeNeeded) {
      super(model, v, p, pTimeNeeded);
    }

    @Override
    protected void finish(TimeLapse time) {
      modelRef.vehicleState.put(vehicle, VehicleState.IDLE);
      modelRef.doDrop(vehicle, parcel, time.getTime());
    }

  }

  static class DeliverAction extends VehicleParcelAction {
    DeliverAction(DefaultPDPModel model, Vehicle v, Parcel p,
        long pTimeNeeded) {
      super(model, v, p, pTimeNeeded);
    }

    @Override
    public void finish(TimeLapse time) {
      modelRef.vehicleState.put(vehicle, VehicleState.IDLE);
      modelRef.doDeliver(vehicle, parcel, time.getTime());
    }
  }
}
