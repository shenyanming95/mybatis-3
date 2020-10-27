/**
 * Copyright 2009-2020 the original author or authors.
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

import java.util.List;
import java.util.Properties;

/**
 * MyBatis每次创建结果对象的新实例时，都会使用ObjectFactory实例来执行此操作。
 * 默认的ObjectFactory除了使用默认的构造函数实例化目标类外，或者如果存在参数映射，则使用参数化的构造函数实例化目标类
 *
 * @author Clinton Begin
 */
public interface ObjectFactory {

    /**
     * Sets configuration properties.
     *
     * @param properties configuration properties
     */
    default void setProperties(Properties properties) {
        // NOP
    }

    /**
     * 通过无参构造方法创建一个新对象
     *
     * @param type 类型
     * @return 新对象
     */
    <T> T create(Class<T> type);

    /**
     * 通过指定的带参的构造方法创建一个对象
     *
     * @param type                对象类型
     * @param constructorArgTypes 构造方法参数类型集合
     * @param constructorArgs     构造方法参数值集合
     * @return 新对象
     */
    <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);


    /**
     * 判断是否为{@link java.util.Collection}类型
     *
     * @param type 待判断的类型
     * @return true-表示{@link java.util.Collection}类型
     */
    <T> boolean isCollection(Class<T> type);

}
