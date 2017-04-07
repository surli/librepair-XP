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
package org.apache.flink.cep.pattern.quantifier;

/**
 * Configuration class that describes properties of a Pattern.
 *
 * <p>There are three main kinds of a Pattern:
 * <ol>
 *     <li>{@link QuantifierSingleton}</li>
 *     <li>{@link QuantifierLooping}</li>
 *     <li>{@link QuantifierTimes}</li>
 * </ol>
 *
 * <p>Each of those Patterns can be optional. For times it means that the Pattern is either
 * omitted or matches exact number of events, for oneOrMore that changes into zeroOrMore.
 *
 * <p>For every pattern a ConsumingStrategy is set. There are three possibilities:
 * <ol>
 *     <li>STRICT(default) - it means that the very next element has to match the Pattern, otherwise it is discarded</li>
 *     <li>SKIP_TILL_NEXT - there can be some not matching events in between, but only the first matching event will
 *     be consumed into this Pattern</li>
 *     <li>SKIP_TILL_ANY - all matching events will be consumed into this Pattern until a Pattern will be timeouted
 *     (without timeout started Pattern will never be cleared)</li>
 * </ol>
 *
 * <p>In case of both {@link QuantifierTimes} and {@link QuantifierLooping} the above continuity is applied to
 * the first state of a matching sequence. One can also apply strategy for sequence of events fitting into one of those.
 * For more info see {@link QuantifierComplex}.
 */
public abstract class Quantifier {

	private boolean isOptional = false;

	private ConsumingStrategy consumingStrategy = ConsumingStrategy.STRICT;

	public enum ConsumingStrategy {
		STRICT,
		SKIP_TILL_NEXT,
		SKIP_TILL_ANY
	}

	Quantifier(Quantifier other) {
		this.isOptional = other.isOptional;
		this.consumingStrategy = other.consumingStrategy;
	}

	Quantifier() {
	}

	public void setOptional(boolean optional) {
		this.isOptional = optional;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public ConsumingStrategy getConsumingStrategy() {
		return consumingStrategy;
	}

	public void setConsumingStrategy(ConsumingStrategy consumingStrategy) {
		this.consumingStrategy = consumingStrategy;
	}
}
