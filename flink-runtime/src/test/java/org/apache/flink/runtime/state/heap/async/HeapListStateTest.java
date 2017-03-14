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

package org.apache.flink.runtime.state.heap.async;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeutils.base.IntSerializer;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the simple Java heap objects implementation of the {@link ListState}.
 */
@SuppressWarnings("unchecked")
public class HeapListStateTest extends HeapStateBackendTestBase {

	@Test
	public void testAddAndGet() throws Exception {

		final ListStateDescriptor<Long> stateDescr = new ListStateDescriptor<>("my-state", Long.class);
		stateDescr.initializeSerializerUnlessSet(new ExecutionConfig());

		final AsyncHeapKeyedStateBackend<String> keyedBackend = createKeyedBackend();

		try {
			ListState<Long> state =
					keyedBackend.createListState(VoidNamespaceSerializer.INSTANCE, stateDescr);

			AbstractHeapMergingState<Long, VoidNamespace, ?, ?, ?, ?, ?> mergingState =
				(AbstractHeapMergingState<Long, VoidNamespace, ?, ?, ?, ?, ?>) state;

			mergingState.setCurrentNamespace(VoidNamespace.INSTANCE);

			keyedBackend.setCurrentKey("abc");
			assertNull(state.get());

			keyedBackend.setCurrentKey("def");
			assertNull(state.get());
			state.add(17L);
			state.add(11L);
			assertEquals(asList(17L, 11L), state.get());

			keyedBackend.setCurrentKey("abc");
			assertNull(state.get());

			keyedBackend.setCurrentKey("g");
			assertNull(state.get());
			state.add(1L);
			state.add(2L);

			keyedBackend.setCurrentKey("def");
			assertEquals(asList(17L, 11L), state.get());
			state.clear();
			assertNull(state.get());

			keyedBackend.setCurrentKey("g");
			state.add(3L);
			state.add(2L);
			state.add(1L);

			keyedBackend.setCurrentKey("def");
			assertNull(state.get());

			keyedBackend.setCurrentKey("g");
			assertEquals(asList(1L, 2L, 3L, 2L, 1L), state.get());
			state.clear();

			// make sure all lists / maps are cleared

			StateTable<String, VoidNamespace, ArrayList<Long>> stateTable =
					((HeapListState<String, VoidNamespace, Long>) state).getStateTable();

			assertTrue(mergingState.getStateTable().isEmpty());
		}
		finally {
			keyedBackend.close();
			keyedBackend.dispose();
		}
	}

	@Test
	public void testMerging() throws Exception {

		final ListStateDescriptor<Long> stateDescr = new ListStateDescriptor<>("my-state", Long.class);
		stateDescr.initializeSerializerUnlessSet(new ExecutionConfig());

		final Integer namespace1 = 1;
		final Integer namespace2 = 2;
		final Integer namespace3 = 3;

		final Set<Long> expectedResult = new HashSet<>(asList(11L, 22L, 33L, 44L, 55L));

		final AsyncHeapKeyedStateBackend<String> keyedBackend = createKeyedBackend();

		try {
			ListState<Long> state = keyedBackend.createListState(IntSerializer.INSTANCE, stateDescr);

			AbstractHeapMergingState<Long, Integer, ?, ?, ?, ?, ?> mergingState =
				(AbstractHeapMergingState<Long, Integer, ?, ?, ?, ?, ?>) state;

			// populate the different namespaces
			//  - abc spreads the values over three namespaces
			//  - def spreads teh values over two namespaces (one empty)
			//  - ghi is empty
			//  - jkl has all elements already in the target namespace
			//  - mno has all elements already in one source namespace

			keyedBackend.setCurrentKey("abc");
			mergingState.setCurrentNamespace(namespace1);
			state.add(33L);
			state.add(55L);

			mergingState.setCurrentNamespace(namespace2);
			state.add(22L);
			state.add(11L);

			mergingState.setCurrentNamespace(namespace3);
			state.add(44L);

			keyedBackend.setCurrentKey("def");
			mergingState.setCurrentNamespace(namespace1);
			state.add(11L);
			state.add(44L);

			mergingState.setCurrentNamespace(namespace3);
			state.add(22L);
			state.add(55L);
			state.add(33L);

			keyedBackend.setCurrentKey("jkl");
			mergingState.setCurrentNamespace(namespace1);
			state.add(11L);
			state.add(22L);
			state.add(33L);
			state.add(44L);
			state.add(55L);

			keyedBackend.setCurrentKey("mno");
			mergingState.setCurrentNamespace(namespace3);
			state.add(11L);
			state.add(22L);
			state.add(33L);
			state.add(44L);
			state.add(55L);

			keyedBackend.setCurrentKey("abc");
			//TODO
			mergingState.mergeNamespaces(namespace1, asList(namespace2, namespace3));
			mergingState.setCurrentNamespace(namespace1);
			validateResult(state.get(), expectedResult);

			keyedBackend.setCurrentKey("def");
			mergingState.mergeNamespaces(namespace1, asList(namespace2, namespace3));
			mergingState.setCurrentNamespace(namespace1);
			validateResult(state.get(), expectedResult);

			keyedBackend.setCurrentKey("ghi");
			mergingState.mergeNamespaces(namespace1, asList(namespace2, namespace3));
			mergingState.setCurrentNamespace(namespace1);
			assertNull(state.get());

			keyedBackend.setCurrentKey("jkl");
			mergingState.mergeNamespaces(namespace1, asList(namespace2, namespace3));
			mergingState.setCurrentNamespace(namespace1);
			validateResult(state.get(), expectedResult);

			keyedBackend.setCurrentKey("mno");
			mergingState.mergeNamespaces(namespace1, asList(namespace2, namespace3));
			mergingState.setCurrentNamespace(namespace1);
			validateResult(state.get(), expectedResult);

			// make sure all lists / maps are cleared

			keyedBackend.setCurrentKey("abc");
			mergingState.setCurrentNamespace(namespace1);
			state.clear();

			keyedBackend.setCurrentKey("def");
			mergingState.setCurrentNamespace(namespace1);
			state.clear();

			keyedBackend.setCurrentKey("ghi");
			mergingState.setCurrentNamespace(namespace1);
			state.clear();

			keyedBackend.setCurrentKey("jkl");
			mergingState.setCurrentNamespace(namespace1);
			state.clear();

			keyedBackend.setCurrentKey("mno");
			mergingState.setCurrentNamespace(namespace1);
			state.clear();

			assertTrue(mergingState.getStateTable().isEmpty());
		}
		finally {
			keyedBackend.close();
			keyedBackend.dispose();
		}
	}
	
	private static <T> void validateResult(Iterable<T> values, Set<T> expected) {
		int num = 0;
		for (T v : values) {
			num++;
			assertTrue(expected.contains(v));
		}

		assertEquals(expected.size(), num);
	}
}
