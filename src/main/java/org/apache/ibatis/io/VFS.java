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
package org.apache.ibatis.io;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 虚拟文件系统(Virtual File System)抽象类,
 * 提供一个非常简单的API, 用于访问应用程序服务器内的资源.
 *
 * @author Ben Gunter
 */
public abstract class VFS {
    private static final Log log = LogFactory.getLog(VFS.class);

    // 内置VFS实现类数组
    public static final Class<?>[] IMPLEMENTATIONS = {JBoss6VFS.class, DefaultVFS.class};

    // 自定义的VFS实现类的数组
    public static final List<Class<? extends VFS>> USER_IMPLEMENTATIONS = new ArrayList<>();


    /**
     * VFS的持有类, 单例
     */
    private static class VFSHolder {
        static final VFS INSTANCE = createVFS();

        @SuppressWarnings("unchecked")
        static VFS createVFS() {
            // Try the user implementations first, then the built-ins
            List<Class<? extends VFS>> impls = new ArrayList<>();
            impls.addAll(USER_IMPLEMENTATIONS);
            impls.addAll(Arrays.asList((Class<? extends VFS>[]) IMPLEMENTATIONS));

            // 选择最后一个可用的VFS实现类
            VFS vfs = null;
            for (int i = 0; vfs == null || !vfs.isValid(); i++) {
                Class<? extends VFS> impl = impls.get(i);
                try {
                    vfs = impl.getDeclaredConstructor().newInstance();
                    if (!vfs.isValid() && log.isDebugEnabled()) {
                        log.debug("VFS implementation " + impl.getName()
                                + " is not valid in this environment.");
                    }
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    log.error("Failed to instantiate " + impl, e);
                    return null;
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Using VFS adapter " + vfs.getClass().getName());
            }

            return vfs;
        }
    }

    /**
     * 获取VFS实现类, 可能会返回null
     */
    public static VFS getInstance() {
        return VFSHolder.INSTANCE;
    }

    /**
     * 添加新的VFS实现
     */
    public static void addImplClass(Class<? extends VFS> clazz) {
        if (clazz != null) {
            USER_IMPLEMENTATIONS.add(clazz);
        }
    }

    /**
     * 通过类全名获取Class对象
     */
    protected static Class<?> getClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
            // return ReflectUtil.findClass(className);
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Class not found: " + className);
            }
            return null;
        }
    }

    /**
     * 根据方法名 + 方法参数类型, 获取Method对象
     */
    protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (SecurityException e) {
            log.error("Security exception looking for method " + clazz.getName() + "." + methodName + ".  Cause: " + e);
            return null;
        } catch (NoSuchMethodException e) {
            log.error("Method not found " + clazz.getName() + "." + methodName + "." + methodName + ".  Cause: " + e);
            return null;
        }
    }

    /**
     * 反射回调方法
     */
    @SuppressWarnings("unchecked")
    protected static <T> T invoke(Method method, Object object, Object... parameters)
            throws IOException, RuntimeException {
        try {
            return (T) method.invoke(object, parameters);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IOException) {
                throw (IOException) e.getTargetException();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 从上下文类加载器中获取在指定路径上找到的所有资源的{@link URL}列表
     */
    protected static List<URL> getResources(String path) throws IOException {
        return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
    }

    /**
     * 若VFS对当前环境有效，则返回true
     */
    public abstract boolean isValid();

    /**
     * 递归列出作为URL标识的资源的子级的所有资源的完整资源路径。
     */
    protected abstract List<String> list(URL url, String forPath) throws IOException;

    /**
     * Recursively list the full resource path of all the resources that are children of all the
     * resources found at the specified path.
     *
     * @param path The path of the resource(s) to list.
     * @return A list containing the names of the child resources.
     * @throws IOException If I/O errors occur
     */
    public List<String> list(String path) throws IOException {
        List<String> names = new ArrayList<>();
        for (URL url : getResources(path)) {
            names.addAll(list(url, path));
        }
        return names;
    }
}
