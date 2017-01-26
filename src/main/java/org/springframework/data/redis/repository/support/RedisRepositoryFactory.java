/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.redis.repository.support;

import java.io.Serializable;

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.redis.core.mapping.RedisPersistentEntity;
import org.springframework.data.redis.repository.core.MappingRedisEntityInformation;
import org.springframework.data.redis.repository.query.RedisQueryCreator;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

/**
 * {@link RepositoryFactorySupport} specific of handing Redis
 * {@link org.springframework.data.keyvalue.repository.KeyValueRepository}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.7
 */
public class RedisRepositoryFactory extends KeyValueRepositoryFactory {

	private final KeyValueOperations operations;

	/**
	 * @param keyValueOperations
	 * @see KeyValueRepositoryFactory#KeyValueRepositoryFactory(KeyValueOperations)
	 */
	public RedisRepositoryFactory(KeyValueOperations keyValueOperations) {
		this(keyValueOperations, RedisQueryCreator.class);
	}

	/**
	 * @param keyValueOperations
	 * @param queryCreator
	 * @see KeyValueRepositoryFactory#KeyValueRepositoryFactory(KeyValueOperations, Class)
	 */
	public RedisRepositoryFactory(KeyValueOperations keyValueOperations,
			Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {
		this(keyValueOperations, queryCreator, KeyValuePartTreeQuery.class);
	}

	/**
	 * @param keyValueOperations
	 * @param queryCreator
	 * @param repositoryQueryType
	 * @see KeyValueRepositoryFactory#KeyValueRepositoryFactory(KeyValueOperations, Class, Class)
	 */
	public RedisRepositoryFactory(KeyValueOperations keyValueOperations,
			Class<? extends AbstractQueryCreator<?, ?>> queryCreator, Class<? extends RepositoryQuery> repositoryQueryType) {
		super(keyValueOperations, queryCreator, repositoryQueryType);

		this.operations = keyValueOperations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory#getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		RedisPersistentEntity<T> entity = (RedisPersistentEntity<T>) operations.getMappingContext()
				.getPersistentEntity(domainClass);
		EntityInformation<T, ID> entityInformation = (EntityInformation<T, ID>) new MappingRedisEntityInformation<T, ID>(
				entity);

		return entityInformation;
	}
}
