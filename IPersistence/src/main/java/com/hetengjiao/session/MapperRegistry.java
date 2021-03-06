package com.hetengjiao.session;

import com.hetengjiao.io.ResolverUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapperRegistry {
	private final Configuration config;
	private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

	public MapperRegistry(Configuration config) {
		this.config = config;
	}

	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
		if (mapperProxyFactory == null) {
			throw new RuntimeException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new RuntimeException("Error getting mapper instance. Cause: " + e, e);
		}
	}

	public <T> void addMapper(Class<T> type) {
		if (type.isInterface()) {
			if (hasMapper(type)) {
				throw new RuntimeException("Type " + type + " is already known to the MapperRegistry.");
			}
			boolean loadCompleted = false;
			try {
				knownMappers.put(type, new MapperProxyFactory<>(type));
				// It's important that the type is added before the parser is run
				// otherwise the binding may automatically be attempted by the
				// mapper parser. If the type is already known, it won't try.
				//MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				//parser.parse();
				loadCompleted = true;
			} finally {
				if (!loadCompleted) {
					knownMappers.remove(type);
				}
			}
		}
	}

	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	/**
	 * @since 3.2.2
	 */
	public void addMappers(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		for (Class<?> mapperClass : mapperSet) {
			addMapper(mapperClass);
		}
	}

	/**
	 * @since 3.2.2
	 */
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}
}
