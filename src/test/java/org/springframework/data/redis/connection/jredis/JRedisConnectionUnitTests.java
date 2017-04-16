/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.redis.connection.jredis;

import org.jredis.JRedis;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.AbstractConnectionUnitTestBase;
import org.springframework.data.redis.connection.RedisServerCommands.ShutdownOption;

/**
 * @author Christoph Strobl
 */
public class JRedisConnectionUnitTests extends AbstractConnectionUnitTestBase<JRedis> {

	private JredisConnection connection;

	@Before
	public void setUp() {
		connection = new JredisConnection(getNativeRedisConnectionMock());
	}

	@Test(expected = UnsupportedOperationException.class) // DATAREDIS-184
	public void shutdownSaveShouldThrowUnsupportedOperationException() {
		connection.shutdown(ShutdownOption.SAVE);
	}

	@Test(expected = UnsupportedOperationException.class) // DATAREDIS-184
	public void shutdownNosaveShouldThrowUnsupportedOperationException() {
		connection.shutdown(ShutdownOption.NOSAVE);
	}

	@Test(expected = UnsupportedOperationException.class) // DATAREDIS-184
	public void shutdownWithNullShouldThrowUnsupportedOperationException() {
		connection.shutdown(null);
	}

	@Test(expected = UnsupportedOperationException.class) // DATAREDIS-270
	public void getClientNameShouldSendRequestCorrectly() {
		connection.getClientName();
	}

}
