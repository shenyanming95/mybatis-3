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
package org.apache.ibatis.plugin;

import org.apache.ibatis.reflection.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 插件类, 实现了JDK的动态代理
 *
 * @author Clinton Begin
 */
public class Plugin implements InvocationHandler {

    private final Object target;
    private final Interceptor interceptor;
    private final Map<Class<?>, Set<Method>> signatureMap;

    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    public static Object wrap(Object target, Interceptor interceptor) {
        // 解析拦截器上的注解@Intercepts, 获取拦截器作用的方法签名
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        // 获取定义在@Intercepts的接口
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        // 如果有接口, 就返回代理类
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    interfaces,
                    // Plugin作为代理类
                    new Plugin(target, interceptor, signatureMap));
        }
        // 如果没有接口, 就原样返回.
        return target;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // 获取方法签名已经定义好了的方法集合
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            if (methods != null && methods.contains(method)) {
                // 若不为空, 则通过拦截器执行方法
                return interceptor.intercept(new Invocation(target, method, args));
            }
            // 若为空, 则直接反射回调即可
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

    /**
     * 获取{@link Interceptor}上的方法签名, 它定义了此拦截器要作用的方法
     *
     * @param interceptor 拦截器
     * @return 方法信息
     */
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        // 获取拦截器上的注解Intercepts, 必须要定义作用的方法.
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        // 获取注解Intercepts 定义的方法签名信息
        Signature[] signatures = interceptsAnnotation.value();
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
        for (Signature signature : signatures) {
            // 每一个类, 一个方法集合
            Set<Method> methods = signatureMap.computeIfAbsent(signature.type(), k -> new HashSet<>());
            try {
                // 通过方法名和参数类型定义一个Method对象, 加入到集合中
                Method method = signature.type().getMethod(signature.method(), signature.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + signature.type() + " named " + signature.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }

    /**
     * 获取所有接口类型
     *
     * @param type         获取它的所有接口类型
     * @param signatureMap 解析{@link Intercepts}得到的方法签名
     * @return 接口类型集合
     */
    private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
        Set<Class<?>> interfaces = new HashSet<>();
        while (type != null) {
            // 获取type的所有接口类型
            for (Class<?> c : type.getInterfaces()) {
                // 如果包含在方法签名集合内, 则将其添加到set中
                if (signatureMap.containsKey(c)) {
                    interfaces.add(c);
                }
            }
            // 处理父类
            type = type.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[0]);
    }

}
