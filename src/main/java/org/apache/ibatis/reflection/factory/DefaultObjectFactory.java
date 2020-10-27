/**
 * Copyright 2009-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection.factory;

import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.Reflector;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 默认的, 通用的对象工厂实现
 *
 * @author Clinton Begin
 */
public class DefaultObjectFactory implements ObjectFactory, Serializable {

    private static final long serialVersionUID = -8855120656740914948L;

    @Override
    public <T> T create(Class<T> type) {
        return create(type, null, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        // 解析Type, 如果是接口, mybatis会默认创建接口的实现类; 如果是其它类型, 它就保持不变.
        Class<?> classToCreate = resolveInterface(type);
        // we know types are assignable
        return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
    }

    private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        try {
            Constructor<T> constructor;
            if (constructorArgTypes == null || constructorArgs == null) {
                // 获取无参的构造方法
                constructor = type.getDeclaredConstructor();
                try {
                    // 调用构造方法, 获取一个实例.
                    return constructor.newInstance();
                } catch (IllegalAccessException e) {
                    if (Reflector.canControlMemberAccessible()) {
                        // 如果允许改变成员的访问权限, 设置private修饰的访问权限.
                        constructor.setAccessible(true);
                        return constructor.newInstance();
                    } else {
                        throw e;
                    }
                }
            }
            // 获取指定参数类型的构造方法
            constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[0]));
            try {
                // 传入构造方法需要的参数值, 反射调用它, 获取对象.
                return constructor.newInstance(constructorArgs.toArray(new Object[0]));
            } catch (IllegalAccessException e) {
                if (Reflector.canControlMemberAccessible()) {
                    // 如果允许改变成员的访问权限, 设置private修饰的访问权限.
                    constructor.setAccessible(true);
                    return constructor.newInstance(constructorArgs.toArray(new Object[0]));
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            String argTypes = Optional.ofNullable(constructorArgTypes).orElseGet(Collections::emptyList)
                    .stream().map(Class::getSimpleName).collect(Collectors.joining(","));
            String argValues = Optional.ofNullable(constructorArgs).orElseGet(Collections::emptyList)
                    .stream().map(String::valueOf).collect(Collectors.joining(","));
            throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
        }
    }


    protected Class<?> resolveInterface(Class<?> type) {
        Class<?> classToCreate;
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            // 如果是集合类型, 就创建ArrayList
            classToCreate = ArrayList.class;
        } else if (type == Map.class) {
            // 如果是Map类型, 就创建HashMap
            classToCreate = HashMap.class;
        } else if (type == SortedSet.class) {
            // issue #510 Collections Support
            // 如果是SortedSet类型, 就创建TreeSet
            classToCreate = TreeSet.class;
        } else if (type == Set.class) {
            // 如果是Set类型, 就创建HashSet
            classToCreate = HashSet.class;
        } else {
            // 其它类型, 就原样创建
            classToCreate = type;
        }
        return classToCreate;
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return Collection.class.isAssignableFrom(type);
    }

}
