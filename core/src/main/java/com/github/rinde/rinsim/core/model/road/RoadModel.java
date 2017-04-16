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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * RoadModel is a model that manages a fleet of vehicles ({@link RoadUser}s) on
 * top of a <i>space</i>. The space that is used depends on the specific
 * implementation of {@link RoadModel}. {@link RoadUser}s have a position which
 * is represented by a {@link Point}. Generally, RoadModels are responsible for:
 * <ul>
 * <li>adding and removing objects</li>
 * <li>moving objects around</li>
 * </ul>
 * On top of that the RoadModel provides several functions for retrieving
 * objects and finding the shortest path. More utilities for working with
 * {@link RoadModel}s are defined in {@link RoadModels}.
 * @author Rinde van Lon
 */
public interface RoadModel extends Model<RoadUser> {

  /**
   * Moves the specified {@link MovingRoadUser} towards the specified
   * <code>destination</code> using the path returned by
   * {@link #getShortestPathTo(RoadUser, Point)}. There must be time left in the
   * provided {@link TimeLapse}. The {@link #getDestination(MovingRoadUser)}
   * method will return the destination point as specified in the most recent
   * invocation of this method.
   * <p>
   * <b>Speed</b><br>
   * The {@link MovingRoadUser} has to define a speed with which it wants to
   * travel. This method uses the {@link MovingRoadUser}s speed as an
   * <i>upper</i> bound, it gives no guarantee about the lower bound (i.e. the
   * object could stand still). The actual speed of the object depends on the
   * model implementation. A model can define constraints such as speed limits
   * or traffic jams which can slow down a {@link MovingRoadUser}.
   * <p>
   * <b>Time</b><br>
   * The time that is specified as indicated by the {@link TimeLapse} object may
   * or may not be consumed completely. Normally, this method will try to
   * consume all time in the {@link TimeLapse} object. In case the destination
   * is reached before all time is consumed (which depends on the object's
   * <i>speed</i>, the distance to the <code>destination</code> and any speed
   * constraints if available) there will be some time left in the
   * {@link TimeLapse}.
   * @param object The object that is moved.
   * @param destination The destination position.
   * @param time The time that is available for travel.
   * @return A {@link MoveProgress} instance which details: the distance
   *         traveled, the actual time spent traveling and the nodes which where
   *         traveled.
   * @see #moveTo(MovingRoadUser, RoadUser, TimeLapse)
   * @see #followPath(MovingRoadUser, Queue, TimeLapse)
   */
  MoveProgress moveTo(MovingRoadUser object, Point destination, TimeLapse time);

  /**
   * Moves the specified {@link MovingRoadUser} towards the specified
   * <code>destination</code> using the path returned by
   * {@link #getShortestPathTo(RoadUser, RoadUser)}. There must be time left in
   * the provided {@link TimeLapse}. The {@link #getDestination(MovingRoadUser)}
   * method will return the destination point as specified in the most recent
   * invocation of this method.
   * <p>
   * <b>Speed</b><br>
   * The {@link MovingRoadUser} has to define a speed with which it wants to
   * travel. This method uses the {@link MovingRoadUser}s speed as an
   * <i>upper</i> bound, it gives no guarantee about the lower bound (i.e. the
   * object could stand still). The actual speed of the object depends on the
   * model implementation. A model can define constraints such as speed limits
   * or traffic jams which can slow down a {@link MovingRoadUser}.
   * <p>
   * <b>Time</b><br>
   * The time that is specified as indicated by the {@link TimeLapse} object may
   * or may not be consumed completely. Normally, this method will try to
   * consume all time in the {@link TimeLapse} object. In case the destination
   * is reached before all time is consumed (which depends on the object's
   * <i>speed</i>, the distance to the <code>destination</code> and any speed
   * constraints if available) there will be some time left in the
   * {@link TimeLapse}.
   * @param object The object that is moved.
   * @param destination The destination position.
   * @param time The time that is available for travel.
   * @return A {@link MoveProgress} instance which details: the distance
   *         traveled, the actual time spent traveling and the nodes which where
   *         traveled.
   * @see #moveTo(MovingRoadUser, Point, TimeLapse)
   * @see #followPath(MovingRoadUser, Queue, TimeLapse)
   */
  MoveProgress moveTo(MovingRoadUser object, RoadUser destination,
      TimeLapse time);

  /**
   * Moves the specified {@link MovingRoadUser} using the specified path and
   * with the specified time. The provided <code>path</code> can not be empty
   * and there must be time left in the provided {@link TimeLapse}.
   * <p>
   * <b>Speed</b><br>
   * The {@link MovingRoadUser} has to define a speed with which it wants to
   * travel. This method uses the {@link MovingRoadUser}s speed as an
   * <i>upper</i> bound, it gives no guarantee about the lower bound (i.e. the
   * object could stand still). The actual speed of the object depends on the
   * model implementation. A model can define constraints such as speed limits
   * or traffic jams which can slow down a {@link MovingRoadUser}.
   * <p>
   * <b>Path</b><br>
   * The {@link MovingRoadUser} follows the path that is specified by the
   * provided {@link Queue}. This path is composed of a number of {@link Point}
   * s, which will be traveled in order as they appear. For example: consider
   * that the path contains three points: <code>A, B, C</code>. The
   * {@link MovingRoadUser} will first travel to {@link Point} <code>A</code>,
   * once it has reached this point it will be <i>removed</i> out of the
   * {@link Queue}. This means that after this method is finished the provided
   * {@link Queue} will contain only <code>B, C</code>. By storing the reference
   * to the queue, users of this method can repeatedly call this method using
   * the same path object instance. The {@link #getDestination(MovingRoadUser)}
   * method will return the last point of the path specified in the most recent
   * invocation of this method.
   * <p>
   * Note that when an invalid path is supplied this method will throw an
   * {@link IllegalArgumentException}. This method guarantees that the road
   * model will not be in an invalid state when this happens. The path that is
   * supplied may not be in a valid state. This means that it is safe to put
   * invocations of this method in a try/catch block but that it is not safe to
   * reuse the supplied path.
   * <p>
   * <b>Time</b><br>
   * The time that is specified as indicated by the {@link TimeLapse} object may
   * or may not be consumed completely. Normally, this method will try to
   * consume all time in the {@link TimeLapse} object. In case the end of the
   * path is reached before all time is consumed (which depends on the object's
   * <i>speed</i>, the length of the <code>path</code> and any speed constraints
   * if available) there will be some time left in the {@link TimeLapse}.
   *
   * @param object The object that is moved.
   * @param path The path that is followed.
   * @param time The time that is available for travel.
   * @return A {@link MoveProgress} instance which details: the distance
   *         traveled, the actual time spent traveling and the nodes which were
   *         traveled.
   * @see #moveTo(MovingRoadUser, Point, TimeLapse)
   * @see #moveTo(MovingRoadUser, RoadUser, TimeLapse)
   */
  MoveProgress followPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time);

  /**
   * Adds a new object to the model at the specified position.
   * @param newObj The object to be added to the model. It can not be an already
   *          added object.
   * @param pos The position on which the object is to be added. This must be a
   *          node which already exists in the model.
   */
  void addObjectAt(RoadUser newObj, Point pos);

  /**
   * Adds an object at the same position as the existing object.
   * @param newObj The new object to be added to the model. It can not be an
   *          already added object.
   * @param existingObj The existing object which location is used for the
   *          target of the <code>newObj</code>. This object
   *          <strong>must</strong> already exist in the model.
   */
  void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj);

  /**
   * Removes the specified {@link RoadUser} from this model.
   * @param roadUser the object to be removed.
   */
  void removeObject(RoadUser roadUser);

  /**
   * Removes all objects on this RoadStructure instance.
   */
  void clear();

  /**
   * Checks if the specified {@link RoadUser} exists in the model.
   * @param obj The {@link RoadUser} to check for existence, may not be
   *          <code>null</code>.
   * @return <code>true</code> if <code>obj</code> exists in the model,
   *         <code>false</code> otherwise.
   */
  boolean containsObject(RoadUser obj);

  /**
   * Checks if the specified {@link RoadUser} exists at the specified position.
   * @param obj The {@link RoadUser} to check.
   * @param p The position to check.
   * @return <code>true</code> if <code>obj</code> exists at position
   *         <code>p</code>, <code>false</code> otherwise.
   */
  boolean containsObjectAt(RoadUser obj, Point p);

  /**
   * Checks if the positions of the <code>obj1</code> and <code>obj2</code> are
   * equal.
   * @param obj1 A {@link RoadUser}.
   * @param obj2 A {@link RoadUser}.
   * @return <code>true</code> if the positions are equal, <code>false</code>
   *         otherwise.
   */
  boolean equalPosition(RoadUser obj1, RoadUser obj2);

  /**
   * This method returns a mapping of {@link RoadUser} to {@link Point} objects
   * which exist in this model. The returned map is not a live view on this
   * model, but a new created copy.
   * @return A map of {@link RoadUser} to {@link Point} objects.
   */
  // TODO add tests to check that this map really is not a live view
  Map<RoadUser, Point> getObjectsAndPositions();

  /**
   * Method to retrieve the location of an object.
   * @param roadUser The object for which the position is examined.
   * @return The position (as a {@link Point} object) for the specified
   *         <code>obj</code> object.
   */
  Point getPosition(RoadUser roadUser);

  /**
   * Finds the destination of the road user if it has a destination. A road user
   * has a destination if it has moved previously using either of the following
   * methods:
   * <ul>
   * <li>{@link #followPath(MovingRoadUser, Queue, TimeLapse)}, in this case the
   * destination is the last point in the supplied path.</li>
   * <li>{@link #moveTo(MovingRoadUser, Point, TimeLapse)}, in this case the
   * destination is the supplied point.</li>
   * <li>{@link #moveTo(MovingRoadUser, RoadUser, TimeLapse)}, in this case the
   * destination is the position of the target road user.</li>
   * </ul>
   * @param roadUser The road user to look up.
   * @return The destination of the specified road user or <code>null</code> if
   *         the road user has no destination.
   */
  @Nullable
  Point getDestination(MovingRoadUser roadUser);

  /**
   * Searches a random position in the space which is defined by this model.
   * @param rnd The {@link RandomGenerator} which is used for obtaining a random
   *          number.
   * @return A random position in this model.
   */
  Point getRandomPosition(RandomGenerator rnd);

  /**
   * This method returns a collection of {@link Point} objects which are the
   * positions of the objects that exist in this model. The returned collection
   * is not a live view on the set, but a new created copy.
   * @return The collection of {@link Point} objects.
   */
  Collection<Point> getObjectPositions();

  /**
   * This method returns the set of {@link RoadUser} objects which exist in this
   * model. The returned set is not a live view on the set, but a new created
   * copy.
   * @return The set of {@link RoadUser} objects.
   */
  Set<RoadUser> getObjects();

  /**
   * This method returns a set of {@link RoadUser} objects which exist in this
   * model and satisfy the given {@link Predicate}. The returned set is not a
   * live view on this model, but a new created copy.
   * @param predicate The predicate that decides which objects to return.
   * @return A set of {@link RoadUser} objects.
   */
  Set<RoadUser> getObjects(Predicate<RoadUser> predicate);

  /**
   * Returns all objects of the given type located in the same position as the
   * given {@link RoadUser}.
   * @param roadUser The object which location is checked for other objects.
   * @param type The type of the objects to be returned.
   * @param <Y> The type of the objects in the returned set.
   * @return A set of objects of type <code>type</code>.
   */
  <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser, Class<Y> type);

  /**
   * This method returns a set of {@link RoadUser} objects which exist in this
   * model and are instances of the specified {@link Class}. The returned set is
   * not a live view on the set, but a new created copy.
   * @param type The type of returned objects.
   * @param <Y> The type of the objects in the returned set.
   * @return A set of {@link RoadUser} objects.
   */
  <Y extends RoadUser> Set<Y> getObjectsOfType(Class<Y> type);

  /**
   * Convenience method for {@link #getShortestPathTo(Point, Point)}.
   * @param fromObj The object which is used as the path origin
   * @param toObj The object which is used as the path destination
   * @return The shortest path from 'fromObj' to 'toObj'.
   */
  List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj);

  /**
   * Convenience method for {@link #getShortestPathTo(Point, Point)}.
   * @param fromObj The object which is used as the path origin
   * @param to The path destination
   * @return The shortest path from 'fromObj' to 'to'
   */
  List<Point> getShortestPathTo(RoadUser fromObj, Point to);

  /**
   * Finds the shortest between <code>from</code> and <code>to</code>. The
   * definition of a <i>shortest</i> path is defined by the specific
   * implementation, possibilities include the shortest travel time and the
   * shortest distance.
   * @param from The start point of the path.
   * @param to The end point of the path.
   * @return The shortest path.
   */
  List<Point> getShortestPathTo(Point from, Point to);

  /**
   * Finds a path that is optimal according to the given {@link GeomHeuristic}
   * between the points <code>from</code> and <code>to</code>.
   * @param from The starting point.
   * @param to The ending point.
   * @param timeUnit The time unit to use for the calculations.
   * @param maxSpeed The speed of the {@link RoadUser} that requests the path.
   * @param heuristic The heuristic to use to determine the optimal path.
   * @return The path following the heuristic decorated with the heuristic value
   *         for the path as well as its travel time with the given speed in the
   *         given time unit.
   */
  RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> maxSpeed, GeomHeuristic heuristic);

  /**
   * Determines the distance of the given path, indicated by a list of
   * connecting points.
   * @param path The path to find the distance of.
   * @return The length of the given path in the distance unit of the model.
   * @throws IllegalArgumentException If the path contains less than two points.
   */
  Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException;

  /**
   * @return The {@link EventAPI} for this road model.
   */
  EventAPI getEventAPI();

  /**
   * @return Should return exactly two points. The first point contains the
   *         minimum x and y value, the second point contains the maximum x and
   *         y value.
   */
  ImmutableList<Point> getBounds();

  /**
   * @return The distance unit as used in this model to represent distances.
   */
  Unit<Length> getDistanceUnit();

  /**
   * @return The speed unit as used in this model to represent speeds.
   */
  Unit<Velocity> getSpeedUnit();

  /**
   * @return A snapshot of the current state of this road model.
   */
  RoadModelSnapshot getSnapshot();
}
