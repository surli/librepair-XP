/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection.convert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.FutureResult;

/**
 * Converts the results of transaction exec using a supplied Queue of {@link FutureResult}s. Converts any Exception
 * objects returned in the list as well, using the supplied Exception {@link Converter}
 * 
 * @author Jennifer Hickey
 * @param <T> The type of {@link FutureResult} of the individual tx operations
 */
public class TransactionResultConverter<T> implements Converter<List<Object>, List<Object>> {

	private Queue<FutureResult<T>> txResults = new LinkedList<FutureResult<T>>();

	private Converter<Exception, DataAccessException> exceptionConverter;

	public TransactionResultConverter(Queue<FutureResult<T>> txResults,
			Converter<Exception, DataAccessException> exceptionConverter) {
		this.txResults = txResults;
		this.exceptionConverter = exceptionConverter;
	}

	public List<Object> convert(List<Object> execResults) {
		if (execResults == null) {
			return null;
		}
		if (execResults.size() != txResults.size()) {
			throw new IllegalArgumentException("Incorrect number of transaction results. Expected: " + txResults.size()
					+ " Actual: " + execResults.size());
		}
		List<Object> convertedResults = new ArrayList<Object>();
		for (Object result : execResults) {
			FutureResult<T> futureResult = txResults.remove();
			if (result instanceof Exception) {
				throw exceptionConverter.convert((Exception) result);
			}
			if (!(futureResult.isStatus())) {
				convertedResults.add(futureResult.convert(result));
			}
		}
		return convertedResults;
	}
}
