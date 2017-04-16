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
package com.github.rinde.rinsim.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Basic event dispatcher for easily dispatching {@link Event}s to
 * {@link Listener}s. It provides methods for dispatching events and removing
 * and adding of listeners.
 * @author Rinde van Lon
 */
public final class EventDispatcher implements EventAPI {

  /**
   * A map of event types to registered {@link Listener}s.
   */
  final SetMultimap<Enum<?>, Listener> listeners;

  /**
   * The set of event types that this event dispatcher supports.
   */
  final ImmutableSet<Enum<?>> supportedTypes;

  /**
   * The 'public' api of this dispatcher. Public in this context means API that
   * is intended for <i>users</i> of the dispatcher, that is, classes that want
   * to be notified of events.
   */
  final PublicEventAPI publicAPI;

  private final AtomicInteger dispatching;
  private final SetMultimap<Enum<?>, Listener> toRemove;
  private final SetMultimap<Enum<?>, Listener> toAdd;

  /**
   * Creates a new {@link EventDispatcher} instance which is capable of
   * dispatching any {@link Event} with a <code>type</code> attribute that is
   * one of <code>eventTypes</code>.
   * @param supportedEventTypes The types of events this EventDispatcher
   *          supports.
   */
  public EventDispatcher(Set<Enum<?>> supportedEventTypes) {
    checkArgument(!supportedEventTypes.isEmpty(),
      "At least one event type must be supported.");
    listeners = Multimaps.synchronizedSetMultimap(
      LinkedHashMultimap.<Enum<?>, Listener>create());
    supportedTypes = ImmutableSet.copyOf(supportedEventTypes);
    publicAPI = new PublicEventAPI(this);
    dispatching = new AtomicInteger(0);
    toRemove = LinkedHashMultimap.create();
    toAdd = LinkedHashMultimap.create();
  }

  /**
   * Creates a new {@link EventDispatcher} instance which is capable of
   * dispatching any {@link Event} with a <code>type</code> attribute that is
   * one of <code>eventTypes</code>.
   * @param supportedEventTypes The types of events this EventDispatcher
   *          supports.
   */
  public EventDispatcher(Enum<?>... supportedEventTypes) {
    this(new HashSet<>(asList(supportedEventTypes)));
  }

  /**
   * Dispatch an event. Notifies all listeners that are listening for this type
   * of event.
   * @param e The event to be dispatched, only events with a supported type can
   *          be dispatched.
   */
  public void dispatchEvent(Event e) {

    synchronized (listeners) {
      dispatching.incrementAndGet();
      checkCanDispatchEventType(e.getEventType());
      for (final Listener l : listeners.get(e.getEventType())) {
        l.handleEvent(e);
      }
      dispatching.decrementAndGet();
    }

    update();
  }

  void update() {
    if (dispatching.get() == 0) {
      if (!toRemove.isEmpty()) {
        for (final Entry<Enum<?>, Listener> entry : toRemove.entries()) {
          removeListener(entry.getValue(), entry.getKey());
        }
        toRemove.clear();
      }
      if (!toAdd.isEmpty()) {
        for (final Entry<Enum<?>, Listener> entry : toAdd.entries()) {
          add(entry.getValue(),
            ImmutableSet.<Enum<?>>of(entry.getKey()), false);
        }
        toAdd.clear();
      }
    }
  }

  void checkCanDispatchEventType(Enum<?> eventType) {
    checkArgument(
      supportedTypes.contains(eventType),
      "Cannot dispatch an event of type %s since it was not registered at "
        + "this dispatcher.",
      eventType);
  }

  public void safeDispatchEvent(Event e) {
    dispatching.incrementAndGet();
    final Set<Listener> targetListeners;
    synchronized (listeners) {
      checkCanDispatchEventType(e.getEventType());
      targetListeners = ImmutableSet.copyOf(listeners.get(e.getEventType()));
    }

    for (final Listener l : targetListeners) {
      l.handleEvent(e);
    }
    dispatching.decrementAndGet();
    update();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener listener, Enum<?>... eventTypes) {
    add(listener, ImmutableSet.copyOf(eventTypes),
      eventTypes.length == 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener listener,
      Iterable<? extends Enum<?>> eventTypes) {
    add(listener, ImmutableSet.<Enum<?>>copyOf(eventTypes), false);
  }

  /**
   * Adds the specified listener. From now on, the specified listener will be
   * notified of events with one of the <code>eventTypes</code>. If
   * <code>eventTypes</code> is empty, the listener will be notified of no
   * events. If <code>all</code> is <code>true</code> the value for
   * <code>eventTypes</code> is ignored and the listener is registered for
   * <i>all</i> events. Otherwise, if <code>all</code> is <code>false</code> the
   * listener is only registered for the event types in <code>eventTypes</code>.
   * @param listener The listener to register.
   * @param eventTypes The event types to listen to.
   * @param all Indicates whether <code>eventTypes</code> is used or if the
   *          listener is registered to all event types.
   */
  void add(Listener listener, ImmutableSet<Enum<?>> eventTypes, boolean all) {
    synchronized (listeners) {
      final Set<Enum<?>> theTypes =
        all ? supportedTypes : ImmutableSet.copyOf(eventTypes);

      for (final Enum<?> eventType : theTypes) {
        checkArgument(supportedTypes.contains(eventType),
          "A listener for type %s is not allowed.", eventType);

        if (dispatching.get() == 0) {
          listeners.put(eventType, listener);
        } else {
          toAdd.put(eventType, listener);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Listener listener, Enum<?>... eventTypes) {
    removeListener(listener, ImmutableSet.copyOf(eventTypes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Listener listener,
      Iterable<? extends Enum<?>> eventTypes) {
    synchronized (listeners) {
      if (Iterables.isEmpty(eventTypes)) {
        // remove all store keys in intermediate set to avoid concurrent
        // modifications
        final Set<Enum<?>> keys = new HashSet<>(listeners.keySet());
        for (final Enum<?> eventType : keys) {
          if (listeners.containsEntry(eventType, listener)) {
            removeListener(listener, eventType);
          }
        }
      } else {
        for (final Enum<?> eventType : eventTypes) {
          checkNotNull(eventType, "event type to remove can not be null");
          checkArgument(
            containsListener(listener, eventType),
            "The listener %s for the type %s cannot be removed because it "
              + "does not exist.",
            listener, eventType);

          if (dispatching.get() == 0) {
            listeners.remove(eventType, listener);
          } else {
            toRemove.put(eventType, listener);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsListener(Listener listener, Enum<?> eventType) {
    return listeners.containsEntry(eventType, listener);
  }

  /**
   * Checks if the dispatcher has a listener for the specific event type.
   * @param eventType The event type.
   * @return <code>true</code> if there is a listener for the specific type,
   *         <code>false</code> otherwise.
   */
  public boolean hasListenerFor(Enum<?> eventType) {
    return listeners.containsKey(eventType);
  }

  /**
   * This method returns the public {@link EventAPI} instance associated to this
   * {@link EventDispatcher}. This instance can be made publicly available to
   * classes outside the scope of the events. Through this instance listeners
   * can be added and removed to this {@link EventDispatcher}.
   * @return A wrapper for {@link EventDispatcher}, only shows the methods which
   *         should be allowed to be called outside of the dispatcher's parent.
   */
  public EventAPI getPublicEventAPI() {
    return publicAPI;
  }

  static class PublicEventAPI implements EventAPI {
    private final EventDispatcher ref;

    PublicEventAPI(EventDispatcher ed) {
      ref = ed;
    }

    @Override
    public void addListener(Listener l, Enum<?>... eventTypes) {
      ref.addListener(l, eventTypes);
    }

    @Override
    public void addListener(Listener listener,
        Iterable<? extends Enum<?>> eventTypes) {
      ref.addListener(listener, eventTypes);
    }

    @Override
    public void removeListener(Listener l, Enum<?>... eventTypes) {
      ref.removeListener(l, eventTypes);
    }

    @Override
    public void removeListener(Listener listener,
        Iterable<? extends Enum<?>> eventTypes) {
      ref.removeListener(listener, eventTypes);
    }

    @Override
    public boolean containsListener(Listener l, Enum<?> eventType) {
      return ref.containsListener(l, eventType);
    }
  }
}
