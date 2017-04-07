/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.cep.pattern;

import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.cep.nfa.NFA;
import org.apache.flink.cep.pattern.conditions.AndCondition;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.OrCondition;
import org.apache.flink.cep.pattern.conditions.SubtypeCondition;
import org.apache.flink.cep.pattern.quantifier.Quantifier;
import org.apache.flink.cep.pattern.quantifier.QuantifierComplex;
import org.apache.flink.cep.pattern.quantifier.QuantifierLooping;
import org.apache.flink.cep.pattern.quantifier.QuantifierSingleton;
import org.apache.flink.cep.pattern.quantifier.QuantifierTimes;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Preconditions;

/**
 * Base class for a pattern definition.
 * <p>
 * A pattern definition is used by {@link org.apache.flink.cep.nfa.compiler.NFACompiler} to create
 * a {@link NFA}.
 *
 * <pre>{@code
 * Pattern<T, F> pattern = Pattern.<T>begin("start")
 *   .next("middle").subtype(F.class)
 *   .followedBy("end").where(new MyFilterFunction());
 * }
 * </pre>
 *
 * @param <T> Base type of the elements appearing in the pattern
 * @param <F> Subtype of T to which the current pattern operator is constrained
 */
public class Pattern<T, F extends T> {

	// name of the pattern operator
	private final String name;

	// previous pattern operator
	private final Pattern<T, ? extends T> previous;

	// filter condition for an event to be matched
	private IterativeCondition<F> condition;

	// window length in which the pattern match has to occur
	private Time windowTime;

	private Quantifier quantifier = new QuantifierSingleton();

	protected Pattern(final String name, final Pattern<T, ? extends T> previous) {
		this.name = name;
		this.previous = previous;
	}

	public String getName() {
		return name;
	}

	public Pattern<T, ? extends T> getPrevious() {
		return previous;
	}

	public IterativeCondition<F> getCondition() {
		return condition;
	}

	public Time getWindowTime() {
		return windowTime;
	}

	public Quantifier getQuantifier() {
		return quantifier;
	}

	/**
	 * Specifies a filter condition which has to be fulfilled by an event in order to be matched.
	 *
	 * @param condition Filter condition
	 * @return The same pattern operator where the new filter condition is set
	 */
	public Pattern<T, F> where(IterativeCondition<F> condition) {
		ClosureCleaner.clean(condition, true);

		if (this.condition == null) {
			this.condition = condition;
		} else {
			this.condition = new AndCondition<>(this.condition, condition);
		}
		return this;
	}

	/**
	 * Specifies a filter condition which is OR'ed with an existing filter function.
	 *
	 * @param condition OR filter condition
	 * @return The same pattern operator where the new filter condition is set
	 */
	public Pattern<T, F> or(IterativeCondition<F> condition) {
		ClosureCleaner.clean(condition, true);

		if (this.condition == null) {
			this.condition = condition;
		} else {
			this.condition = new OrCondition<>(this.condition, condition);
		}
		return this;
	}

	/**
	 * Applies a subtype constraint on the current pattern operator. This means that an event has
	 * to be of the given subtype in order to be matched.
	 *
	 * @param subtypeClass Class of the subtype
	 * @param <S> Type of the subtype
	 * @return The same pattern operator with the new subtype constraint
	 */
	public <S extends F> Pattern<T, S> subtype(final Class<S> subtypeClass) {
		if (condition == null) {
			this.condition = new SubtypeCondition<F>(subtypeClass);
		} else {
			this.condition = new AndCondition<>(this.condition,
					new SubtypeCondition<F>(subtypeClass));
		}

		@SuppressWarnings("unchecked")
		Pattern<T, S> result = (Pattern<T, S>) this;

		return result;
	}

	/**
	 * Defines the maximum time interval for a matching pattern. This means that the time gap
	 * between first and the last event must not be longer than the window time.
	 *
	 * @param windowTime Time of the matching window
	 * @return The same pattenr operator with the new window length
	 */
	public Pattern<T, F> within(Time windowTime) {
		if (windowTime != null) {
			this.windowTime = windowTime;
		}

		return this;
	}

	/**
	 * Appends a new pattern operator to the existing one. The new pattern operator enforces strict
	 * temporal contiguity. This means that the whole pattern only matches if an event which matches
	 * this operator directly follows the preceding matching event. Thus, there cannot be any
	 * events in between two matching events.
	 *
	 * @param name Name of the new pattern operator
	 * @return A new pattern operator which is appended to this pattern operator
	 */
	public Pattern<T, T> next(final String name) {
		return new Pattern<T, T>(name, this);
	}

	/**
	 * Appends a new pattern operator to the existing one. The new pattern operator enforces
	 * non-strict temporal contiguity. This means that a matching event of this operator and the
	 * preceding matching event might be interleaved with other events which are ignored.
	 *
	 * @param name Name of the new pattern operator
	 * @return A new pattern operator which is appended to this pattern operator
	 */
	public Pattern<T, T> followedBy(final String name) {
		return followedBy(name, false);
	}

	/**
	 * Appends a new pattern operator to the existing one. The new pattern operator enforces
	 * non-strict temporal contiguity. This means that a matching event of this operator and the
	 * preceding matching event might be interleaved with other events which are ignored.
	 *
	 * @param name       Name of the new pattern operator
	 * @param allMatches if false only the first matching event will be consumed
	 * @return A new pattern operator which is appended to this pattern operator
	 */
	public Pattern<T, T> followedBy(final String name, boolean allMatches) {
		final Pattern<T, T> pattern = new Pattern<>(name, this);
		if (allMatches) {
			pattern.quantifier.setConsumingStrategy(Quantifier.ConsumingStrategy.SKIP_TILL_ANY);
		} else {
			pattern.quantifier.setConsumingStrategy(Quantifier.ConsumingStrategy.SKIP_TILL_NEXT);
		}
		return pattern;
	}

	/**
	 * Starts a new pattern with the initial pattern operator whose name is provided. Furthermore,
	 * the base type of the event sequence is set.
	 *
	 * @param name Name of the new pattern operator
	 * @param <X> Base type of the event pattern
	 * @return The first pattern operator of a pattern
	 */
	public static <X> Pattern<X, X> begin(final String name) {
		return new Pattern<>(name, null);
	}

	/**
	 * Specifies that this pattern can occur zero or more times(kleene star).
	 * This means any number of events can be matched in this state.
	 *
	 * @return The same pattern with applied Kleene star operator
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> zeroOrMore() {
		return zeroOrMore(true);
	}

	/**
	 * Specifies that this pattern can occur zero or more times(kleene star).
	 * This means any number of events can be matched in this state.
	 *
	 * If eagerness is enabled for a pattern A*B and sequence A1 A2 B will generate patterns:
	 * B, A1 B and A1 A2 B. If disabled B, A1 B, A2 B and A1 A2 B.
	 *
	 * @param eager if true the pattern always consumes earlier events
	 * @return The same pattern with applied Kleene star operator
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> zeroOrMore(final boolean eager) {
		final QuantifierLooping quantifierLooping = new QuantifierLooping(this.quantifier);
		quantifierLooping.setOptional(true);
		quantifierLooping.setEager(eager);
		this.quantifier = quantifierLooping;
		return this;
	}

	/**
	 * Specifies that this pattern can occur one or more times(kleene star).
	 * This means at least one and at most infinite number of events can be matched in this state.
	 *
	 * @return The same pattern with applied Kleene plus operator
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> oneOrMore() {
		return oneOrMore(true);
	}

	/**
	 * Specifies that this pattern can occur one or more times(kleene star).
	 * This means at least one and at most infinite number of events can be matched in this state.
	 *
	 * If eagerness is enabled for a pattern A+B and sequence A1 A2 B will generate patterns:
	 * A1 B and A1 A2 B. If disabled A1 B, A2 B and A1 A2 B.
	 *
	 * @param eager if true the pattern always consumes earlier events
	 * @return The same pattern with applied Kleene plus operator
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> oneOrMore(final boolean eager) {
		final QuantifierLooping quantifierLooping = new QuantifierLooping(this.quantifier);
		quantifierLooping.setEager(eager);
		this.quantifier = quantifierLooping;
		return this;
	}

	/**
	 * Works in conjunction with {@link Pattern#zeroOrMore()}, {@link Pattern#oneOrMore()} or {@link Pattern#times(int)}.
	 * Specifies that any not matching element breaks the loop.
	 *
	 * <p>E.g. a pattern like:
	 * <pre>{@code
	 * Pattern.<Event>begin("start").where(new FilterFunction<Event>() {
	 *      @Override
	 *      public boolean filter(Event value) throws Exception {
	 *          return value.getName().equals("c");
	 *      }
	 * })
	 * .followedBy("middle").where(new FilterFunction<Event>() {
	 *      @Override
	 *      public boolean filter(Event value) throws Exception {
	 *          return value.getName().equals("a");
	 *      }
	 * })
	 * }<b>.oneOrMore(true).consecutive()</b>{@code
	 * .followedBy("end1").where(new FilterFunction<Event>() {
	 *      @Override
	 *      public boolean filter(Event value) throws Exception {
	 *          return value.getName().equals("b");
	 *      }
	 * });
	 * }</pre>
	 *
	 * <p>for a sequence: C D A1 A2 A3 D A4 B
	 *
	 * <p>will generate matches: {C A1 B}, {C A1 A2 B}, {C A1 A2 A3 B}
	 *
	 * <p><b>NOTICE:</b> This operator can be applied only when either zeroOrMore,
	 * oneOrMore or times was previously applied!
	 *
	 * <p>By default a relaxed continuity is applied.
	 *
	 * @return pattern with continuity changed to strict
	 */
	public Pattern<T, F> consecutive() {
		if (this.quantifier instanceof QuantifierComplex) {
			((QuantifierComplex)this.quantifier).setStrict(true);
		} else {
			throw new MalformedPatternException("Strict continuity cannot be applied to " + this.quantifier);
		}

		return this;
	}

	/**
	 * Specifies that this pattern can occur zero or once.
	 *
	 * @return The same pattern with applied Kleene ? operator
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> optional() {
		this.quantifier.setOptional(true);
		return this;
	}

	/**
	 * Specifies exact number of times that this pattern should be matched.
	 *
	 * @param times number of times matching event must appear
	 * @return The same pattern with number of times applied
	 *
	 * @throws MalformedPatternException if quantifier already applied
	 */
	public Pattern<T, F> times(int times) {
		Preconditions.checkArgument(times > 0, "You should give a positive number greater than 0.");
		final QuantifierTimes quantifierTimes = new QuantifierTimes(this.quantifier, times);
		quantifierTimes.setEager(true);
		this.quantifier = quantifierTimes;
		return this;
	}

}
