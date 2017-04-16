/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.data.redis.connection.srp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.connection.ReturnType;

import redis.reply.Reply;

/**
 * Converts the value returned by SRP script eval to the expected {@link ReturnType}
 * 
 * @author Jennifer Hickey
 * @deprecated since 1.7. Will be removed in subsequent version.
 */
@Deprecated
public class SrpScriptReturnConverter implements Converter<Object, Object> {

	private ReturnType returnType;

	public SrpScriptReturnConverter(ReturnType returnType) {
		this.returnType = returnType;
	}

	public Object convert(Object source) {
		if (returnType == ReturnType.MULTI) {
			Reply<?>[] replies = (Reply[]) source;
			List<Object> results = new ArrayList<Object>();
			for (Reply<?> reply : replies) {
				results.add(reply.data());
			}
			return results;
		}
		if (returnType == ReturnType.BOOLEAN) {
			// Lua false comes back as a null bulk reply
			if (source == null) {
				return Boolean.FALSE;
			}
			return ((Long) source == 1);
		}
		return source;
	}
}
