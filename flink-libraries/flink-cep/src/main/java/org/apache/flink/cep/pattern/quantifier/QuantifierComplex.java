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

import org.apache.flink.cep.pattern.MalformedPatternException;

/**
 * Quantifier describing a Pattern that can consume multiple events. There are two parameters describing strategy
 * how the elements are consumed into this state.
 *
 * <ol>
 *     <li>Strict(default: false) - specifies that first event that does not fit into this state, stops trying to fit
 *     succeeding elements into this Pattern</li>
 *     <li>Eager(default: true) - specifies that for a sequence of matching events a[], if an event a[t] belongs to the
 *     match, all events a[v], where v&lt;t also belongs to this match, otherwise all possible combinations arereturned
 *     </li>
 * </ol>
 *
 * For basic configuration see also {@link Quantifier}.
 */
public abstract class QuantifierComplex extends Quantifier {

	private boolean isStrict = false;
	private boolean isEager = true;

	QuantifierComplex(Quantifier quantifier) {
		super(quantifier);
		if (!(quantifier instanceof QuantifierSingleton)) {
			throw new MalformedPatternException("Already applied quantifier to this Pattern.");
		}
	}

	public boolean isStrict() {
		return isStrict;
	}

	public boolean isEager() {
		return isEager;
	}

	public void setStrict(boolean strict) {
		isStrict = strict;
	}

	public void setEager(boolean eager) {
		isEager = eager;
	}
}
