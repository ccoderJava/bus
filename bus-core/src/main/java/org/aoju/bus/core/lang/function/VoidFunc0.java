package org.aoju.bus.core.lang.function;

import java.io.Serializable;

/**
 * 函数对象
 * 一个函数接口代表一个一个函数，用于包装一个函数为对象
 * 在JDK8之前，Java的函数并不能作为参数传递，也不能作为返回值存在
 * 此接口用于将一个函数包装成为一个对象，从而传递对象
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
@FunctionalInterface
public interface VoidFunc0 extends Serializable {

    /**
     * 执行函数
     *
     * @throws Exception 自定义异常
     */
    void call() throws Exception;

    /**
     * 执行函数，异常包装为RuntimeException
     */
    default void callWithRuntimeException() {
        try {
            call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
