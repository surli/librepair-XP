/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.data.redis.core;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.data.redis.DoubleObjectFactory;
import org.springframework.data.redis.LongObjectFactory;
import org.springframework.data.redis.ObjectFactory;
import org.springframework.data.redis.Person;
import org.springframework.data.redis.PersonObjectFactory;
import org.springframework.data.redis.RawObjectFactory;
import org.springframework.data.redis.SettingsUtils;
import org.springframework.data.redis.StringObjectFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.OxmSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.oxm.xstream.XStreamMarshaller;

/**
 * Parameters for testing implementations of {@link AbstractOperations}
 * 
 * @author Jennifer Hickey
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
abstract public class AbstractOperationsTestParams {

	// DATAREDIS-241
	public static Collection<Object[]> testParams() {

		ObjectFactory<String> stringFactory = new StringObjectFactory();
		ObjectFactory<Long> longFactory = new LongObjectFactory();
		ObjectFactory<Double> doubleFactory = new DoubleObjectFactory();
		ObjectFactory<byte[]> rawFactory = new RawObjectFactory();
		ObjectFactory<Person> personFactory = new PersonObjectFactory();

		// XStream serializer
		XStreamMarshaller xstream = new XStreamMarshaller();
		try {
			xstream.afterPropertiesSet();
		} catch (Exception ex) {
			throw new RuntimeException("Cannot init XStream", ex);
		}

		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
		jedisConnectionFactory.setPort(SettingsUtils.getPort());
		jedisConnectionFactory.setHostName(SettingsUtils.getHost());
		jedisConnectionFactory.afterPropertiesSet();

		RedisTemplate<String, String> stringTemplate = new StringRedisTemplate();
		stringTemplate.setConnectionFactory(jedisConnectionFactory);
		stringTemplate.afterPropertiesSet();

		RedisTemplate<String, Long> longTemplate = new RedisTemplate<String, Long>();
		longTemplate.setKeySerializer(new StringRedisSerializer());
		longTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
		longTemplate.setConnectionFactory(jedisConnectionFactory);
		longTemplate.afterPropertiesSet();

		RedisTemplate<String, Double> doubleTemplate = new RedisTemplate<String, Double>();
		doubleTemplate.setKeySerializer(new StringRedisSerializer());
		doubleTemplate.setValueSerializer(new GenericToStringSerializer<Double>(Double.class));
		doubleTemplate.setConnectionFactory(jedisConnectionFactory);
		doubleTemplate.afterPropertiesSet();

		RedisTemplate<byte[], byte[]> rawTemplate = new RedisTemplate<byte[], byte[]>();
		rawTemplate.setEnableDefaultSerializer(false);
		rawTemplate.setConnectionFactory(jedisConnectionFactory);
		rawTemplate.afterPropertiesSet();

		RedisTemplate<String, Person> personTemplate = new RedisTemplate<String, Person>();
		personTemplate.setConnectionFactory(jedisConnectionFactory);
		personTemplate.afterPropertiesSet();

		OxmSerializer serializer = new OxmSerializer(xstream, xstream);
		RedisTemplate<String, String> xstreamStringTemplate = new RedisTemplate<String, String>();
		xstreamStringTemplate.setConnectionFactory(jedisConnectionFactory);
		xstreamStringTemplate.setDefaultSerializer(serializer);
		xstreamStringTemplate.afterPropertiesSet();

		RedisTemplate<String, Person> xstreamPersonTemplate = new RedisTemplate<String, Person>();
		xstreamPersonTemplate.setConnectionFactory(jedisConnectionFactory);
		xstreamPersonTemplate.setValueSerializer(serializer);
		xstreamPersonTemplate.afterPropertiesSet();

		JacksonJsonRedisSerializer<Person> jacksonJsonSerializer = new JacksonJsonRedisSerializer<Person>(Person.class);
		RedisTemplate<String, Person> jsonPersonTemplate = new RedisTemplate<String, Person>();
		jsonPersonTemplate.setConnectionFactory(jedisConnectionFactory);
		jsonPersonTemplate.setValueSerializer(jacksonJsonSerializer);
		jsonPersonTemplate.afterPropertiesSet();

		Jackson2JsonRedisSerializer<Person> jackson2JsonSerializer = new Jackson2JsonRedisSerializer<Person>(Person.class);
		RedisTemplate<String, Person> jackson2JsonPersonTemplate = new RedisTemplate<String, Person>();
		jackson2JsonPersonTemplate.setConnectionFactory(jedisConnectionFactory);
		jackson2JsonPersonTemplate.setValueSerializer(jackson2JsonSerializer);
		jackson2JsonPersonTemplate.afterPropertiesSet();

		GenericJackson2JsonRedisSerializer genericJackson2JsonSerializer = new GenericJackson2JsonRedisSerializer();
		RedisTemplate<String, Person> genericJackson2JsonPersonTemplate = new RedisTemplate<String, Person>();
		genericJackson2JsonPersonTemplate.setConnectionFactory(jedisConnectionFactory);
		genericJackson2JsonPersonTemplate.setValueSerializer(genericJackson2JsonSerializer);
		genericJackson2JsonPersonTemplate.afterPropertiesSet();

		return Arrays.asList(new Object[][] { //
				{ stringTemplate, stringFactory, stringFactory }, //
						{ longTemplate, stringFactory, longFactory }, //
						{ doubleTemplate, stringFactory, doubleFactory }, //
						{ rawTemplate, rawFactory, rawFactory }, //
						{ personTemplate, stringFactory, personFactory }, //
						{ xstreamStringTemplate, stringFactory, stringFactory }, //
						{ xstreamPersonTemplate, stringFactory, personFactory }, //
						{ jsonPersonTemplate, stringFactory, personFactory }, //
						{ jackson2JsonPersonTemplate, stringFactory, personFactory }, //
						{ genericJackson2JsonPersonTemplate, stringFactory, personFactory } });
	}
}
