/*
 * Copyright 2017 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection.CommandResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.KeyCommand;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.ReactiveSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Central abstraction for reactive Redis data access.
 * <p/>
 * Performs automatic serialization/deserialization between the given objects and the underlying binary data in the
 * Redis store. By default, it uses Java serialization for its objects (through {@link JdkSerializationRedisSerializer}
 * ).
 * <p/>
 * Once configured, this class is thread-safe.
 * <p/>
 * Note that while the template is generified, it is up to the serializers/deserializers to properly convert the given
 * Objects to and from binary data.
 *
 * @author Mark Paluch
 * @since 2.0
 * @param <K> the Redis key type against which the template works (usually a String)
 * @param <V> the Redis value type against which the template works
 */
public class ReactiveRedisTemplate<K, V>
		implements BeanClassLoaderAware, InitializingBean, ReactiveRedisOperations<K, V> {

	private ReactiveRedisConnectionFactory connectionFactory;
	private boolean exposeConnection = true;
	private boolean initialized = false;
	private boolean enableDefaultSerializer = true;
	private RedisSerializer<?> defaultSerializer;
	private ClassLoader classLoader;
	private ReactiveSerializationContextSupport<K, V> serializationContext = new MutableReactiveSerializationContext<>();

	// cache singleton objects (where possible)
	private ReactiveValueOperations<K, V> valueOps;
	private ReactiveListOperations<K, V> listOps;
	private ReactiveSetOperations<K, V> setOps;
	private ReactiveZSetOperations<K, V> zSetOps;
	private ReactiveHyperLogLogOperations<K, V> hyperLogLogOps;
	private ReactiveGeoOperations<K, V> geoOps;

	/**
	 * Construct a new {@link ReactiveRedisTemplate} instance.
	 */
	public ReactiveRedisTemplate() {}

	/**
	 * Returns the connectionFactory.
	 *
	 * @return Returns the connectionFactory
	 */
	public ReactiveRedisConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * Sets the connection factory.
	 *
	 * @param connectionFactory The connectionFactory to set.
	 */
	public void setConnectionFactory(ReactiveRedisConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * @param enableDefaultSerializer Whether or not the default serializer should be used. If not, any serializers not
	 *          explicilty set will remain null and values will not be serialized or deserialized.
	 */
	public void setEnableDefaultSerializer(boolean enableDefaultSerializer) {
		this.enableDefaultSerializer = enableDefaultSerializer;
	}

	/**
	 * Returns the default serializer used by this template.
	 *
	 * @return template default serializer
	 */
	public RedisSerializer<?> getDefaultSerializer() {
		return defaultSerializer;
	}

	/**
	 * Sets the default serializer to use for this template. All serializers (expect the
	 * {@link #setStringSerializer(RedisSerializer)}) are initialized to this value unless explicitly set. Defaults to
	 * {@link JdkSerializationRedisSerializer}.
	 *
	 * @param serializer default serializer to use
	 */
	public void setDefaultSerializer(RedisSerializer<?> serializer) {
		this.defaultSerializer = serializer;
	}

	/**
	 * Returns the key serializer used by this template.
	 *
	 * @return the key serializer used by this template.
	 */
	public RedisSerializer<K> getKeySerializer() {
		return serializationContext.getKeySerializer();
	}

	/**
	 * Sets the key serializer to be used by this template. Defaults to {@link #getDefaultSerializer()}.
	 *
	 * @param serializer the key serializer to be used by this template.
	 */
	@SuppressWarnings("unchecked")
	public void setKeySerializer(RedisSerializer<?> serializer) {
		getMutableSerializationContext().setKeySerializer((RedisSerializer) serializer);
	}

	/**
	 * Returns the value serializer used by this template.
	 *
	 * @return the value serializer used by this template.
	 */
	public RedisSerializer<V> getValueSerializer() {
		return serializationContext.getValueSerializer();
	}

	/**
	 * Sets the value serializer to be used by this template. Defaults to {@link #getDefaultSerializer()}.
	 *
	 * @param serializer the value serializer to be used by this template.
	 */
	@SuppressWarnings("unchecked")
	public void setValueSerializer(RedisSerializer<?> serializer) {
		getMutableSerializationContext().setValueSerializer((RedisSerializer) serializer);
	}

	/**
	 * Returns the hashKeySerializer.
	 *
	 * @return Returns the hashKeySerializer
	 */
	public RedisSerializer<?> getHashKeySerializer() {
		return serializationContext.getHashKeySerializer();
	}

	/**
	 * Sets the hash key (or field) serializer to be used by this template. Defaults to {@link #getDefaultSerializer()}.
	 *
	 * @param hashKeySerializer The hashKeySerializer to set.
	 */
	public void setHashKeySerializer(RedisSerializer<?> hashKeySerializer) {
		getMutableSerializationContext().setHashKeySerializer(hashKeySerializer);
	}

	/**
	 * Returns the hashValueSerializer.
	 *
	 * @return Returns the hashValueSerializer
	 */
	public RedisSerializer<?> getHashValueSerializer() {
		return serializationContext.getHashValueSerializer();
	}

	/**
	 * Sets the hash value serializer to be used by this template. Defaults to {@link #getDefaultSerializer()}.
	 *
	 * @param hashValueSerializer The hashValueSerializer to set.
	 */
	public void setHashValueSerializer(RedisSerializer<?> hashValueSerializer) {
		getMutableSerializationContext().setHashValueSerializer(hashValueSerializer);
	}

	/**
	 * Returns the stringSerializer.
	 *
	 * @return Returns the stringSerializer
	 */
	public RedisSerializer<String> getStringSerializer() {
		return serializationContext.getStringSerializer();
	}

	/**
	 * Sets the string value serializer to be used by this template (when the arguments or return types are always
	 * strings). Defaults to {@link StringRedisSerializer}.
	 *
	 * @param stringSerializer The stringValueSerializer to set.
	 * @see ValueOperations#get(Object, long, long)
	 */
	public void setStringSerializer(RedisSerializer<String> stringSerializer) {
		getMutableSerializationContext().setStringSerializer(stringSerializer);
	}

	/**
	 * Set the {@link ClassLoader} to be used for the default {@link JdkSerializationRedisSerializer} in case no other
	 * {@link RedisSerializer} is explicitly set as the default one.
	 *
	 * @param classLoader can be {@literal null}.
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Initializes the properties and creates and sets an immutable {@link ReactiveSerializationContext}. Serializers
	 * cannot be changed after properties are initialized.
	 */
	@Override
	public void afterPropertiesSet() {

		boolean defaultUsed = false;

		if (this.defaultSerializer == null) {

			this.defaultSerializer = new JdkSerializationRedisSerializer(
					this.classLoader != null ? this.classLoader : this.getClass().getClassLoader());
		}

		if (this.enableDefaultSerializer) {

			if (this.serializationContext.getKeySerializer() == null) {
				setKeySerializer(this.defaultSerializer);
				defaultUsed = true;
			}

			if (this.serializationContext.getValueSerializer() == null) {
				setValueSerializer(this.defaultSerializer);
				defaultUsed = true;
			}

			if (this.serializationContext.getHashKeySerializer() == null) {
				setHashKeySerializer(this.defaultSerializer);
				defaultUsed = true;
			}

			if (this.serializationContext.getHashValueSerializer() == null) {
				setHashValueSerializer(this.defaultSerializer);
				defaultUsed = true;
			}
		}

		if (this.enableDefaultSerializer && defaultUsed) {
			Assert.notNull(this.defaultSerializer, "Default serializer is null and not all serializers initialized");
		}

		this.serializationContext = new ImmutableReactiveSerializationContext<K, V>(serializationContext);
		this.initialized = true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForValue()
	 */
	@Override
	public ReactiveValueOperations<K, V> opsForValue() {

		if (valueOps == null) {
			valueOps = opsForValue(serializationContext);
		}

		return valueOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForValue(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveValueOperations<K1, V1> opsForValue(
			ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveValueOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForList()
	 */
	@Override
	public ReactiveListOperations<K, V> opsForList() {

		if (listOps == null) {
			listOps = opsForList(serializationContext);
		}

		return listOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForList(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveListOperations<K1, V1> opsForList(ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveListOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForSet()
	 */
	public ReactiveSetOperations<K, V> opsForSet() {

		if (setOps == null) {
			setOps = opsForSet(serializationContext);
		}

		return setOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForSet(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveSetOperations<K1, V1> opsForSet(ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveSetOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForZSet()
	 */
	public ReactiveZSetOperations<K, V> opsForZSet() {

		if (zSetOps == null) {
			zSetOps = opsForZSet(serializationContext);
		}

		return zSetOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForZSet(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveZSetOperations<K1, V1> opsForZSet(ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveZSetOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForHyperLogLog()
	 */
	@Override
	public ReactiveHyperLogLogOperations<K, V> opsForHyperLogLog() {

		if (hyperLogLogOps == null) {
			hyperLogLogOps = opsForHyperLogLog(serializationContext);
		}

		return hyperLogLogOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForHyperLogLog(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveHyperLogLogOperations<K1, V1> opsForHyperLogLog(
			ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveHyperLogLogOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForHash()
	 */
	@Override
	public <HK, HV> ReactiveHashOperations<K, HK, HV> opsForHash() {
		return opsForHash(serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForHash(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, HK, HV> ReactiveHashOperations<K1, HK, HV> opsForHash(
			ReactiveSerializationContext<K1, ?> serializationContext) {
		return new DefaultReactiveHashOperations<>(this, serializationContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForGeo()
	 */
	@Override
	public ReactiveGeoOperations<K, V> opsForGeo() {

		if (geoOps == null) {
			geoOps = opsForGeo(serializationContext);
		}

		return geoOps;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#opsForGeo(org.springframework.data.redis.serializer.ReactiveSerializationContext)
	 */
	@Override
	public <K1, V1> ReactiveGeoOperations<K1, V1> opsForGeo(ReactiveSerializationContext<K1, V1> serializationContext) {
		return new DefaultReactiveGeoOperations<>(this, serializationContext);
	}

	// -------------------------------------------------------------------------
	// Execution methods
	// -------------------------------------------------------------------------

	public <T> Flux<T> execute(ReactiveRedisCallback<T> action) {
		return execute(action, exposeConnection);
	}

	/**
	 * Executes the given action object within a connection that can be exposed or not. Additionally, the connection can
	 * be pipelined. Note the results of the pipeline are discarded (making it suitable for write-only scenarios).
	 *
	 * @param <T> return type
	 * @param action callback object to execute
	 * @param exposeConnection whether to enforce exposure of the native Redis Connection to callback code
	 * @return object returned by the action
	 */
	public <T> Flux<T> execute(ReactiveRedisCallback<T> action, boolean exposeConnection) {

		Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
		Assert.notNull(action, "Callback object must not be null");

		ReactiveRedisConnectionFactory factory = getConnectionFactory();
		ReactiveRedisConnection conn = factory.getReactiveConnection();

		try {

			ReactiveRedisConnection connToUse = preProcessConnection(conn, false);

			ReactiveRedisConnection connToExpose = (exposeConnection ? connToUse : createRedisConnectionProxy(connToUse));
			Publisher<T> result = action.doInRedis(connToExpose);

			return Flux.from(postProcessResult(result, connToUse, false));
		} finally {
			conn.close();
		}
	}

	/**
	 * Create a reusable Flux for a {@link ReactiveRedisCallback}. Callback is executed within a connection context. The
	 * connection is released outside the callback.
	 *
	 * @param callback must not be {@literal null}
	 * @return a {@link Flux} wrapping the {@link ReactiveRedisCallback}.
	 */
	public <T> Flux<T> createFlux(ReactiveRedisCallback<T> callback) {

		Assert.notNull(callback, "ReactiveRedisCallback must not be null!");

		return Flux.defer(() -> doInConnection(callback, exposeConnection));
	}

	/**
	 * Create a reusable Mono for a {@link ReactiveRedisCallback}. Callback is executed within a connection context. The
	 * connection is released outside the callback.
	 *
	 * @param callback must not be {@literal null}
	 * @return a {@link Mono} wrapping the {@link ReactiveRedisCallback}.
	 */
	public <T> Mono<T> createMono(final ReactiveRedisCallback<T> callback) {

		Assert.notNull(callback, "ReactiveRedisCallback must not be null!");

		return Mono.defer(() -> Mono.from(doInConnection(callback, exposeConnection)));
	}

	/**
	 * Executes the given action object within a connection that can be exposed or not. Additionally, the connection can
	 * be pipelined. Note the results of the pipeline are discarded (making it suitable for write-only scenarios).
	 *
	 * @param <T> return type
	 * @param action callback object to execute
	 * @param exposeConnection whether to enforce exposure of the native Redis Connection to callback code
	 * @return object returned by the action
	 */
	private <T> Publisher<T> doInConnection(ReactiveRedisCallback<T> action, boolean exposeConnection) {

		Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
		Assert.notNull(action, "Callback object must not be null");

		ReactiveRedisConnectionFactory factory = getConnectionFactory();
		ReactiveRedisConnection conn = factory.getReactiveConnection();

		ReactiveRedisConnection connToUse = preProcessConnection(conn, false);

		ReactiveRedisConnection connToExpose = (exposeConnection ? connToUse : createRedisConnectionProxy(connToUse));
		Publisher<T> result = action.doInRedis(connToExpose);

		return Flux.from(postProcessResult(result, connToUse, false)).doAfterTerminate(conn::close);
	}

	// -------------------------------------------------------------------------
	// Methods dealing with Redis keys
	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#hasKey(java.lang.Object)
	 */
	public Mono<Boolean> hasKey(K key) {

		Assert.notNull(key, "Key must not be null!");

		return createMono(connection -> connection.keyCommands().exists(rawKey(key)));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#type(java.lang.Object)
	 */
	public Mono<DataType> type(K key) {

		Assert.notNull(key, "Key must not be null!");

		return createMono(connection -> connection.keyCommands().type(rawKey(key)));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#keys(java.lang.Object)
	 */
	public Flux<K> keys(K pattern) {

		Assert.notNull(pattern, "Pattern must not be null!");

		return createFlux(connection -> connection.keyCommands().keys(rawKey(pattern))) //
				.flatMap(Flux::fromIterable) //
				.map(this::readKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#randomKey()
	 */
	public Mono<K> randomKey() {
		return createMono(connection -> connection.keyCommands().randomKey()).map(this::readKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#rename(java.lang.Object, java.lang.Object)
	 */
	public Mono<Boolean> rename(K oldKey, K newKey) {

		Assert.notNull(oldKey, "Old key must not be null!");
		Assert.notNull(newKey, "New Key must not be null!");

		return createMono(connection -> connection.keyCommands().rename(rawKey(oldKey), rawKey(newKey)));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#renameIfAbsent(java.lang.Object, java.lang.Object)
	 */
	public Mono<Boolean> renameIfAbsent(K oldKey, K newKey) {

		Assert.notNull(oldKey, "Old key must not be null!");
		Assert.notNull(newKey, "New Key must not be null!");

		return createMono(connection -> connection.keyCommands().renameNX(rawKey(oldKey), rawKey(newKey)));

	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#delete(java.lang.Object[])
	 */
	@SafeVarargs
	public final Mono<Long> delete(K... keys) {

		Assert.notNull(keys, "Keys must not be null!");
		Assert.notEmpty(keys, "Keys must not be empty!");
		Assert.noNullElements(keys, "Keys must not contain null elements!");

		if (keys.length == 1) {
			return createMono(connection -> connection.keyCommands().del(rawKey(keys[0])));
		}

		Mono<List<ByteBuffer>> listOfKeys = Flux.fromArray(keys).map(this::rawKey).collectList();
		return createMono(connection -> listOfKeys.flatMap(rawKeys -> connection.keyCommands().mDel(rawKeys)));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#delete(org.reactivestreams.Publisher)
	 */
	public Mono<Long> delete(Publisher<K> keys) {

		Assert.notNull(keys, "Keys must not be null!");

		return createMono(connection -> connection.keyCommands() //
				.del(Flux.from(keys).map(this::rawKey).map(KeyCommand::new)) //
				.map(CommandResponse::getOutput));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#expire(java.lang.Object, java.time.Duration)
	 */
	@Override
	public Mono<Boolean> expire(K key, Duration timeout) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(timeout, "Timeout must not be null!");

		if (timeout.getNano() == 0) {
			return createMono(connection -> connection.keyCommands() //
					.expire(rawKey(key), timeout));
		}

		return createMono(connection -> connection.keyCommands().pExpire(rawKey(key), timeout));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#expireAt(java.lang.Object, java.time.Instant)
	 */
	@Override
	public Mono<Boolean> expireAt(K key, Instant expireAt) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(expireAt, "Expire at must not be null!");

		if (expireAt.getNano() == 0) {
			return createMono(connection -> connection.keyCommands() //
					.expireAt(rawKey(key), expireAt));
		}

		return createMono(connection -> connection.keyCommands().pExpireAt(rawKey(key), expireAt));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#persist(java.lang.Object)
	 */
	@Override
	public Mono<Boolean> persist(K key) {

		Assert.notNull(key, "Key must not be null!");

		return createMono(connection -> connection.keyCommands().persist(rawKey(key)));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#getExpire(java.lang.Object)
	 */
	@Override
	public Mono<Duration> getExpire(K key) {

		Assert.notNull(key, "Key must not be null!");

		return createMono(connection -> connection.keyCommands().pTtl(rawKey(key)).flatMap(expiry -> {

			if (expiry == -1) {
				return Mono.just(Duration.ZERO);
			}

			if (expiry == -2) {
				return Mono.empty();
			}

			return Mono.just(Duration.ofMillis(expiry));
		}));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#move(java.lang.Object, int)
	 */
	@Override
	public Mono<Boolean> move(K key, int dbIndex) {

		Assert.notNull(key, "Key must not be null!");

		return createMono(connection -> connection.keyCommands().move(rawKey(key), dbIndex));
	}

	// -------------------------------------------------------------------------
	// Implementation hooks and helper methods
	// -------------------------------------------------------------------------

	/**
	 * Processes the connection (before any settings are executed on it). Default implementation returns the connection as
	 * is.
	 *
	 * @param connection must not be {@literal null}.
	 * @param existingConnection
	 */
	protected ReactiveRedisConnection preProcessConnection(ReactiveRedisConnection connection,
			boolean existingConnection) {
		return connection;
	}

	/**
	 * Processes the result before returning the {@link Publisher}. Default implementation returns the result as is.
	 *
	 * @param result must not be {@literal null}.
	 * @param connection must not be {@literal null}.
	 * @param existingConnection
	 * @return
	 */
	protected <T> Publisher<T> postProcessResult(Publisher<T> result, ReactiveRedisConnection connection,
			boolean existingConnection) {
		return result;
	}

	protected ReactiveRedisConnection createRedisConnectionProxy(ReactiveRedisConnection reactiveRedisConnection) {

		Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(reactiveRedisConnection.getClass(),
				getClass().getClassLoader());
		return (ReactiveRedisConnection) Proxy.newProxyInstance(reactiveRedisConnection.getClass().getClassLoader(), ifcs,
				new CloseSuppressingInvocationHandler(reactiveRedisConnection));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.ReactiveRedisOperations#serialization()
	 */
	@Override
	public ReactiveSerializationContext<K, V> getSerializationContext() {
		return serializationContext;
	}

	@SuppressWarnings("unchecked")
	private MutableReactiveSerializationContext<K, V> getMutableSerializationContext() {

		Assert.state(serializationContext instanceof MutableReactiveSerializationContext,
				() -> String.format("Client configuration must be instance of MutableReactiveSerializationContext but is %s",
						ClassUtils.getShortName(serializationContext.getClass())));

		return (MutableReactiveSerializationContext) serializationContext;
	}

	private ByteBuffer rawKey(K key) {
		return getSerializationContext().key().getWriter().write(key);
	}

	private K readKey(ByteBuffer buffer) {
		return getSerializationContext().key().getReader().read(buffer);
	}

	/**
	 * @author Mark Paluch
	 */
	static abstract class ReactiveSerializationContextSupport<K, V> implements ReactiveSerializationContext<K, V> {

		@Override
		public abstract SerializationTuple<K> key();

		@Override
		public abstract SerializationTuple<V> value();

		@Override
		public abstract SerializationTuple<String> string();

		@Override
		public abstract <HK> SerializationTuple<HK> hashKey();

		@Override
		public abstract <HV> SerializationTuple<HV> hashValue();

		public abstract RedisSerializer<K> getKeySerializer();

		public abstract RedisSerializer<V> getValueSerializer();

		public abstract RedisSerializer<?> getHashKeySerializer();

		public abstract RedisSerializer<?> getHashValueSerializer();

		public abstract RedisSerializer<String> getStringSerializer();
	}

	/**
	 * @author Mark Paluch
	 */
	static class ImmutableReactiveSerializationContext<K, V> extends ReactiveSerializationContextSupport<K, V> {

		private RedisSerializer<K> keySerializer;
		private final SerializationTuple<K> keyTuple;

		private RedisSerializer<V> valueSerializer;
		private final SerializationTuple<V> valueTuple;

		private RedisSerializer<?> hashKeySerializer;
		private final SerializationTuple<?> hashKeyTuple;

		private RedisSerializer<?> hashValueSerializer;
		private final SerializationTuple<?> hashValueTuple;

		private RedisSerializer<String> stringSerializer;
		private final SerializationTuple<String> stringTuple;

		public ImmutableReactiveSerializationContext(ReactiveSerializationContextSupport<K, V> context) {

			keySerializer = context.getKeySerializer();
			keyTuple = context.key();
			valueSerializer = context.getValueSerializer();
			valueTuple = context.value();
			hashKeySerializer = context.getHashKeySerializer();
			hashKeyTuple = context.hashKey();
			hashValueSerializer = context.getHashValueSerializer();
			hashValueTuple = context.hashValue();
			stringSerializer = context.getStringSerializer();
			stringTuple = context.string();
		}

		@Override
		public SerializationTuple<K> key() {
			return keyTuple;
		}

		@Override
		public SerializationTuple<V> value() {
			return valueTuple;
		}

		@Override
		public SerializationTuple<String> string() {
			return stringTuple;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <HK> SerializationTuple<HK> hashKey() {
			return (SerializationTuple) hashKeyTuple;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <HV> SerializationTuple<HV> hashValue() {
			return (SerializationTuple) hashValueTuple;
		}

		public RedisSerializer<K> getKeySerializer() {
			return keySerializer;
		}

		public RedisSerializer<V> getValueSerializer() {
			return valueSerializer;
		}

		public RedisSerializer<?> getHashKeySerializer() {
			return hashKeySerializer;
		}

		public RedisSerializer<?> getHashValueSerializer() {
			return hashValueSerializer;
		}

		public RedisSerializer<String> getStringSerializer() {
			return stringSerializer;
		}
	}

	/**
	 * @author Mark Paluch
	 */
	static class MutableReactiveSerializationContext<K, V> extends ReactiveSerializationContextSupport<K, V> {

		private RedisSerializer<K> keySerializer;
		private SerializationTuple<K> keyTuple = SerializationTuple.raw();

		private RedisSerializer<V> valueSerializer;
		private SerializationTuple<V> valueTuple = SerializationTuple.raw();

		private RedisSerializer<?> hashKeySerializer;
		private SerializationTuple<?> hashKeyTuple = SerializationTuple.raw();

		private RedisSerializer<?> hashValueSerializer;
		private SerializationTuple<?> hashValueTuple = SerializationTuple.raw();

		private RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		private SerializationTuple<String> stringTuple = SerializationTuple.fromSerializer(stringSerializer);

		@Override
		public SerializationTuple<K> key() {
			return keyTuple;
		}

		@Override
		public SerializationTuple<V> value() {
			return valueTuple;
		}

		@Override
		public SerializationTuple<String> string() {
			return stringTuple;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <HK> SerializationTuple<HK> hashKey() {
			return (SerializationTuple) hashKeyTuple;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <HV> SerializationTuple<HV> hashValue() {
			return (SerializationTuple) hashValueTuple;
		}

		public RedisSerializer<K> getKeySerializer() {
			return keySerializer;
		}

		public void setKeySerializer(RedisSerializer<K> keySerializer) {
			this.keySerializer = keySerializer;
			this.keyTuple = SerializationTuple.fromSerializer(keySerializer);
		}

		public RedisSerializer<V> getValueSerializer() {
			return valueSerializer;
		}

		public void setValueSerializer(RedisSerializer<V> valueSerializer) {
			this.valueSerializer = valueSerializer;
			this.valueTuple = SerializationTuple.fromSerializer(valueSerializer);
		}

		public RedisSerializer<?> getHashKeySerializer() {
			return hashKeySerializer;
		}

		public void setHashKeySerializer(RedisSerializer<?> hashKeySerializer) {
			this.hashKeySerializer = hashKeySerializer;
			this.hashKeyTuple = SerializationTuple.fromSerializer(hashKeySerializer);
		}

		public RedisSerializer<?> getHashValueSerializer() {
			return hashValueSerializer;
		}

		public void setHashValueSerializer(RedisSerializer<?> hashValueSerializer) {
			this.hashValueSerializer = hashValueSerializer;
			this.hashValueTuple = SerializationTuple.fromSerializer(hashValueSerializer);
		}

		public RedisSerializer<String> getStringSerializer() {
			return stringSerializer;
		}

		public void setStringSerializer(RedisSerializer<String> stringSerializer) {
			this.stringSerializer = stringSerializer;
			this.stringTuple = SerializationTuple.fromSerializer(stringSerializer);
		}
	}
}
