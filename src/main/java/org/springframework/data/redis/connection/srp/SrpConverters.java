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
package org.springframework.data.redis.connection.srp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisListCommands.Position;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.connection.RedisZSetCommands.Range.Boundary;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.connection.convert.Converters;
import org.springframework.data.redis.connection.convert.LongToBooleanConverter;
import org.springframework.data.redis.connection.convert.StringToRedisClientInfoConverter;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.util.Assert;

import redis.client.RedisException;
import redis.reply.IntegerReply;
import redis.reply.Reply;

import com.google.common.base.Charsets;

/**
 * SRP type converters
 * 
 * @author Jennifer Hickey
 * @author Christoph Strobl
 * @author Thomas Darimont
 * @deprecated since 1.7. Will be removed in subsequent version.
 */
@Deprecated
@SuppressWarnings("rawtypes")
abstract public class SrpConverters extends Converters {

	private static final byte[] BEFORE = "BEFORE".getBytes(Charsets.UTF_8);
	private static final byte[] AFTER = "AFTER".getBytes(Charsets.UTF_8);
	private static final Converter<Reply[], List<byte[]>> REPLIES_TO_BYTES_LIST;
	private static final Converter<Reply[], Set<byte[]>> REPLIES_TO_BYTES_SET;
	private static final Converter<Reply[], Set<Tuple>> REPLIES_TO_TUPLE_SET;
	private static final Converter<Reply[], Map<byte[], byte[]>> REPLIES_TO_BYTES_MAP;
	private static final Converter<Reply[], List<Boolean>> REPLIES_TO_BOOLEAN_LIST;
	private static final Converter<Reply[], List<String>> REPLIES_TO_STRING_LIST;
	private static final Converter<Reply, String> REPLY_TO_STRING;
	private static final Converter<byte[], Properties> BYTES_TO_PROPERTIES;
	private static final Converter<byte[], String> BYTES_TO_STRING;
	private static final Converter<byte[], Double> BYTES_TO_DOUBLE;
	private static final Converter<Reply[], Long> REPLIES_TO_TIME_AS_LONG;
	private static final Converter<Reply, List<RedisClientInfo>> REPLY_T0_LIST_OF_CLIENT_INFO;
	private static final Converter<String[], List<RedisClientInfo>> STRING_TO_LIST_OF_CLIENT_INFO = new StringToRedisClientInfoConverter();
	private static final Converter<byte[], List<RedisClientInfo>> BYTEARRAY_T0_LIST_OF_CLIENT_INFO;
	private static final Converter<IntegerReply, Boolean> INTEGER_REPLY_TO_BOOLEAN;
	private static final Converter<Long, Boolean> LONG_TO_BOOLEAN = new LongToBooleanConverter();
	private static final Converter<Exception, DataAccessException> EXCEPTION_CONVERTER;

	static {

		REPLIES_TO_BYTES_LIST = new Converter<Reply[], List<byte[]>>() {
			public List<byte[]> convert(Reply[] replies) {
				if (replies == null) {
					return null;
				}
				List<byte[]> list = new ArrayList<byte[]>(replies.length);
				for (Reply reply : replies) {
					Object data = reply.data();
					if (data == null) {
						list.add(null);
					} else if (data instanceof byte[])
						list.add((byte[]) data);
					else
						throw new IllegalArgumentException("array contains more then just nulls and bytes -> " + data);
				}
				return list;
			}
		};

		REPLIES_TO_BYTES_SET = new Converter<Reply[], Set<byte[]>>() {
			public Set<byte[]> convert(Reply[] source) {
				return source != null ? new LinkedHashSet<byte[]>(SrpConverters.toBytesList(source)) : null;
			}
		};

		BYTES_TO_PROPERTIES = new Converter<byte[], Properties>() {
			public Properties convert(byte[] source) {
				return source != null ? SrpConverters.toProperties(new String(source, Charsets.UTF_8)) : null;
			}
		};

		BYTES_TO_DOUBLE = new Converter<byte[], Double>() {
			public Double convert(byte[] bytes) {
				return (bytes == null || bytes.length == 0 ? null : Double.valueOf(new String(bytes, Charsets.UTF_8)));
			}
		};

		REPLIES_TO_TIME_AS_LONG = new Converter<Reply[], Long>() {

			@Override
			public Long convert(Reply[] reply) {

				Assert.notEmpty(reply, "Received invalid result from server. Expected 2 items in collection.");
				Assert.isTrue(reply.length == 2, "Received invalid nr of arguments from redis server. Expected 2 received "
						+ reply.length);

				List<String> serverTimeInformation = REPLIES_TO_STRING_LIST.convert(reply);

				return Converters.toTimeMillis(serverTimeInformation.get(0), serverTimeInformation.get(1));
			}

		};

		REPLIES_TO_TUPLE_SET = new Converter<Reply[], Set<Tuple>>() {
			public Set<Tuple> convert(Reply[] byteArrays) {
				if (byteArrays == null) {
					return null;
				}
				Set<Tuple> tuples = new LinkedHashSet<Tuple>(byteArrays.length / 2 + 1);
				for (int i = 0; i < byteArrays.length; i++) {
					byte[] value = (byte[]) byteArrays[i].data();
					i++;
					Double score = SrpConverters.toDouble((byte[]) byteArrays[i].data());
					tuples.add(new DefaultTuple(value, score));
				}
				return tuples;
			}
		};

		REPLIES_TO_BYTES_MAP = new Converter<Reply[], Map<byte[], byte[]>>() {
			public Map<byte[], byte[]> convert(Reply[] byteArrays) {
				if (byteArrays == null) {
					return null;
				}
				Map<byte[], byte[]> map = new LinkedHashMap<byte[], byte[]>(byteArrays.length / 2);
				for (int i = 0; i < byteArrays.length; i++) {
					map.put((byte[]) byteArrays[i++].data(), (byte[]) byteArrays[i].data());
				}
				return map;
			}
		};

		BYTES_TO_STRING = new Converter<byte[], String>() {
			public String convert(byte[] data) {
				return data != null ? new String((byte[]) data, Charsets.UTF_8) : null;
			}
		};

		REPLIES_TO_BOOLEAN_LIST = new Converter<Reply[], List<Boolean>>() {
			public List<Boolean> convert(Reply[] source) {
				if (source == null) {
					return null;
				}
				List<Boolean> results = new ArrayList<Boolean>();
				for (Reply r : source) {
					results.add(SrpConverters.toBoolean(((IntegerReply) r).data()));
				}
				return results;
			}
		};

		REPLIES_TO_STRING_LIST = new Converter<Reply[], List<String>>() {
			public List<String> convert(Reply[] source) {
				if (source == null) {
					return null;
				}
				List<String> results = new ArrayList<String>();
				for (Reply r : source) {
					results.add(SrpConverters.toString((byte[]) r.data()));
				}
				return results;
			}
		};

		REPLY_TO_STRING = new Converter<Reply, String>() {

			@Override
			public String convert(Reply source) {
				if (source == null) {
					return null;
				}
				return SrpConverters.toString((byte[]) source.data());
			}
		};

		REPLY_T0_LIST_OF_CLIENT_INFO = new Converter<Reply, List<RedisClientInfo>>() {

			@Override
			public List<RedisClientInfo> convert(Reply source) {

				if (source == null || source.data() == null) {
					return Collections.emptyList();
				}
				Assert.isInstanceOf(byte[].class, source.data(), "Expected data to be an instace of byte [].");
				return BYTEARRAY_T0_LIST_OF_CLIENT_INFO.convert((byte[]) source.data());
			}
		};

		BYTEARRAY_T0_LIST_OF_CLIENT_INFO = new Converter<byte[], List<RedisClientInfo>>() {

			@Override
			public List<RedisClientInfo> convert(byte[] source) {
				if (source == null || source.length == 0) {
					return Collections.emptyList();
				}

				String s = SrpConverters.toString(source);
				return STRING_TO_LIST_OF_CLIENT_INFO.convert(s.split("\\r?\\n"));
			}
		};

		INTEGER_REPLY_TO_BOOLEAN = new Converter<IntegerReply, Boolean>() {

			@Override
			public Boolean convert(IntegerReply source) {
				if (source == null || source.data() == null) {
					return false;
				}
				return source.data() == 1;
			}
		};

		EXCEPTION_CONVERTER = new Converter<Exception, DataAccessException>() {

			@Override
			public DataAccessException convert(Exception ex) {

				if (ex instanceof RedisException) {
					return new RedisSystemException("redis exception", ex);
				}

				if (ex instanceof IOException) {
					return new RedisConnectionFailureException("Redis connection failed", (IOException) ex);
				}

				return null;
			}
		};
	}

	public static Converter<Reply[], List<byte[]>> repliesToBytesList() {
		return REPLIES_TO_BYTES_LIST;
	}

	public static Converter<Reply[], Set<byte[]>> repliesToBytesSet() {
		return REPLIES_TO_BYTES_SET;
	}

	public static Converter<byte[], Properties> bytesToProperties() {
		return BYTES_TO_PROPERTIES;
	}

	public static Converter<byte[], Double> bytesToDouble() {
		return BYTES_TO_DOUBLE;
	}

	public static Converter<Reply[], Set<Tuple>> repliesToTupleSet() {
		return REPLIES_TO_TUPLE_SET;
	}

	public static Converter<Reply[], Map<byte[], byte[]>> repliesToBytesMap() {
		return REPLIES_TO_BYTES_MAP;
	}

	public static Converter<byte[], String> bytesToString() {
		return BYTES_TO_STRING;
	}

	public static Converter<Reply, String> replyToString() {
		return REPLY_TO_STRING;
	}

	public static Converter<Reply[], List<Boolean>> repliesToBooleanList() {
		return REPLIES_TO_BOOLEAN_LIST;
	}

	public static Converter<Reply[], List<String>> repliesToStringList() {
		return REPLIES_TO_STRING_LIST;
	}

	public static Converter<Reply[], Long> repliesToTimeAsLong() {
		return REPLIES_TO_TIME_AS_LONG;
	}

	/**
	 * @return
	 * @since 1.3
	 */
	public static List<RedisClientInfo> toListOfRedisClientInformation(Reply reply) {
		return REPLY_T0_LIST_OF_CLIENT_INFO.convert(reply);
	}

	/**
	 * @return
	 * @since 1.3
	 */
	public static Converter<Long, Boolean> longToBooleanConverter() {
		return LONG_TO_BOOLEAN;
	}

	public static List<byte[]> toBytesList(Reply[] source) {
		return REPLIES_TO_BYTES_LIST.convert(source);
	}

	public static Set<byte[]> toBytesSet(Reply[] source) {
		return REPLIES_TO_BYTES_SET.convert(source);
	}

	public static Properties toProperties(byte[] source) {
		return BYTES_TO_PROPERTIES.convert(source);
	}

	public static Double toDouble(byte[] source) {
		return BYTES_TO_DOUBLE.convert(source);
	}

	public static Set<Tuple> toTupleSet(Reply[] source) {
		return REPLIES_TO_TUPLE_SET.convert(source);
	}

	public static Map<byte[], byte[]> toBytesMap(Reply[] source) {
		return REPLIES_TO_BYTES_MAP.convert(source);
	}

	public static String toString(Reply source) {
		return REPLY_TO_STRING.convert(source);
	}

	public static String toString(byte[] source) {
		return BYTES_TO_STRING.convert(source);
	}

	public static List<Boolean> toBooleanList(Reply[] source) {
		return REPLIES_TO_BOOLEAN_LIST.convert(source);
	}

	public static List<String> toStringList(Reply[] source) {
		return REPLIES_TO_STRING_LIST.convert(source);
	}

	/**
	 * Converts given {@link Reply}s to {@link Long}.
	 * 
	 * @param source Array holding time values in seconds and microseconds.
	 * @return
	 */
	public static Long toTimeAsLong(Reply[] source) {
		return REPLIES_TO_TIME_AS_LONG.convert(source);
	}

	public static byte[] toBytes(BitOperation op) {
		Assert.notNull(op, "The bit operation is required");
		return op.name().toUpperCase().getBytes(Charsets.UTF_8);
	}

	public static byte[][] toByteArrays(Map<byte[], byte[]> source) {
		byte[][] result = new byte[source.size() * 2][];
		int index = 0;
		for (Map.Entry<byte[], byte[]> entry : source.entrySet()) {
			result[index++] = entry.getKey();
			result[index++] = entry.getValue();
		}
		return result;
	}

	public static byte[] toBytes(Position source) {
		Assert.notNull(source, "list positions are mandatory");
		return (Position.AFTER.equals(source) ? AFTER : BEFORE);
	}

	public static List<String> toStringList(String source) {
		return Collections.singletonList(source);
	}

	/**
	 * @since 1.3
	 * @return
	 */
	public static Converter<byte[], List<RedisClientInfo>> replyToListOfRedisClientInfo() {
		return BYTEARRAY_T0_LIST_OF_CLIENT_INFO;
	}

	/**
	 * Convert an {@link IntegerReply} to a {@link Boolean} by inspecting {@link IntegerReply#data()}.
	 * 
	 * @since 1.3
	 * @param reply
	 * @return
	 */
	public static Boolean toBoolean(IntegerReply reply) {
		return INTEGER_REPLY_TO_BOOLEAN.convert(reply);
	}

	public static Converter<Exception, DataAccessException> exceptionConverter() {
		return EXCEPTION_CONVERTER;
	}

	/**
	 * Converts a given {@link Boundary} to its binary representation suitable for {@literal ZRANGEBY*} commands, despite
	 * {@literal ZRANGEBYLEX}.
	 * 
	 * @param boundary
	 * @param defaultValue
	 * @return
	 * @since 1.6
	 */
	public static byte[] boundaryToBytesForZRange(Boundary boundary, byte[] defaultValue) {

		if (boundary == null || boundary.getValue() == null) {
			return defaultValue;
		}
		return boundaryToBytes(boundary, new byte[] {}, "(".getBytes(Charsets.UTF_8));
	}

	public static byte[] toBytes(String source) {
		return source.getBytes(Charsets.UTF_8);
	}

	private static byte[] boundaryToBytes(Boundary boundary, byte[] inclPrefix, byte[] exclPrefix) {

		byte[] prefix = boundary.isIncluding() ? inclPrefix : exclPrefix;
		byte[] value = null;
		if (boundary.getValue() instanceof byte[]) {
			value = (byte[]) boundary.getValue();
		} else {
			value = toBytes(boundary.getValue().toString());
		}

		ByteBuffer buffer = ByteBuffer.allocate(prefix.length + value.length);
		buffer.put(prefix);
		buffer.put(value);
		return buffer.array();

	}
}
