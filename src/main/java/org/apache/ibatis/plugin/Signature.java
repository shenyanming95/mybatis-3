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
package org.apache.ibatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示方法签名, 用来定义对一个类的哪些方法进行拦截,
 * 一般配合{@link Intercepts}来使用.
 *
 * @see Intercepts
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Signature {

    /**
     * 指定类的Class类型, 表示要对哪一个类做拦截
     */
    Class<?> type();

    /**
     * 指定类的方法名, 表示要对哪一个类的哪一个方法做拦截
     */
    String method();

    /**
     * 指定方法的参数类型, 结合上面两个, 唯一确定一个方法
     */
    Class<?>[] args();
}
