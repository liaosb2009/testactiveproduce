package com.example.testactiveproduce.common;

import java.lang.annotation.*;

/**
 * 用于描述注解的使用范围（注解可以用在什么地方）
 * 方法和类
 */
@Target({ElementType.METHOD,ElementType.TYPE})
/**
 * 用于注解的生命周期，用于表示该注解会在什么时期保留。
 * RetentionPolicy.RUNTIME：运行时保留，这样就可以通过反射获得了
 * 表示需要在什么级别保存该注解信息。可选的RetentionPolicy参数包括：
 * SOURCE：注解将被编译器丢弃
 * CLASS：注解在class文件中可用，但会被VM丢弃
 * RUNTIME：VM将在运行期间保留注解，因此可以通过反射机制读取注解的信息
 */
@Retention(RetentionPolicy.RUNTIME)
/**
 * 表示该注解会被作为被标注的程序成员的公共API，因此可以被例如javadoc此类的工具文档化。
 */
@Documented
/**
 * @Inherited  //可以继承
 */
@Inherited
public @interface TestAnnotation {
    //允许有值
    String value();
}
