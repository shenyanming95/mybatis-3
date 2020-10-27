/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperProxy.MapperMethodInvoker;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

  // 相对应的Mapper接口的Class类型
  private final Class<T> mapperInterface;
  // Mapper接口内部的方法缓存, 提高反射效率吧. 它和后面的MapperProxy的方法缓存是同一对象
  private final Map<Method, MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();

  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  public Map<Method, MapperMethodInvoker> getMethodCache() {
    return methodCache;
  }

  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    // 方法很简单, 就是JDK的动态代理API调用, 需要注意的是InvocationHandler就是上面提及的
    // MapperProxy对象, 到这步时Mapper接口代理已经创建完成.
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  public T newInstance(SqlSession sqlSession) {
    // 创建MapperProxy实例, 会传入SqlSession、Mapper接口Class对象和该接口的方法缓存;
    // MapperProxy实现了java.lang.reflect.InvocationHandler, 就是JDK反射的处理接口.
    // 所以也可以猜到对Mapper接口方法的调用, 最后会转到MapperProxy的invoke()方法上.
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    // 调用重载的newInstance(MapperProxy)方法
    return newInstance(mapperProxy);
  }

}
