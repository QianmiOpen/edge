/**
 * InterfaceLoader.java
 */
package com.ofpay.edge;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * <p>
 * 接口加载类
 * </p>
 * @author Angus
 * @date 2013-3-11
 * @version 1.0
 * @since 1.0
 */
public class InterfaceLoader {

    private static Logger log = LoggerFactory.getLogger(InterfaceLoader.class);

    /**
     * 所有的Bean的Map；
     * key为beanName;value为bean实例
     */
    public static Map<String, Object> allBeanMap = new TreeMap<String, Object>();

    /**
     * 所有的methdo的Map；
     * key为beanName.methodName;
     * value为Method
     */
    public static Map<String, Method> allMethodMap = new TreeMap<String, Method>();

    /**
     * 接口入参描述的map
     * key为beanName.methodName;
     * value为接口入参的Json描述
     */
    public static Map<String, String> allMethodMapParamDesc = new TreeMap<String, String>();

    /**
     * Bean的加载方法；根据给定的filters从context中加载bean，只从context中加载以beanfilter结尾的bean到内存；
     * @param context Sping上下文
     * @param beanFilters bean过滤参数；
     */
    public static void loadAllBean(ApplicationContext context, String... beanFilters) {
        String[] beanNames = context.getBeanDefinitionNames();

        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            for (int j = 0; j < beanFilters.length; j++) {
                if (beanName.endsWith(beanFilters[j])) {
                    allBeanMap.put(beanName, context.getBean(beanName));
                    break;
                }
            }
        }

        log.info("*************allBeanMap*******************");
        log.debug("{}", allBeanMap);
    }

    /**
     * Bean对应的Method加载
     * @param beanMap 需要加载方法的BeanMap
     */
    public static void loadAllMethod(Map<String, Object> beanMap) {
        for (Iterator<String> iterator = beanMap.keySet().iterator(); iterator.hasNext();) {
            String beanName = iterator.next();
            Object serviceBean = beanMap.get(beanName);
            Class<?> beanClass = serviceBean.getClass();

            Method[] mths = beanClass.getDeclaredMethods();
            for (int i = 0; i < mths.length; i++) {
                String methodName = mths[i].getName();
                if (methodName.equals("$echo")) // 跳过反射出来的方法
                    continue;
                allMethodMap.put(beanName + "." + methodName, mths[i]);
            }
        }

        log.info("*************allMethodMap*******************");
        log.debug("{}", allMethodMap);
    }

    /**
     * @param methodMap
     */
    public static void loadAllMethodMapParamDesc(Map<String, Method> methodMap) {
        for (Iterator<String> iterator = methodMap.keySet().iterator(); iterator.hasNext();) {
            String methodName = iterator.next();
            Method mth = methodMap.get(methodName);
            Class<?>[] types = mth.getParameterTypes();

            if (types != null) {
                Object[] objs = new Object[types.length];

                for (int i = 0; i < objs.length; i++) {
                    try {
                        Class<?> type = types[i];
                        if (isWrapClass(type) || type.isEnum() || type.isInterface()) {
                            objs[i] = null;
                        } else if (type.isArray()) {
                            objs[i] = new Object[0];
                        } else {
                            objs[i] = type.newInstance();
                        }
                    } catch (Exception e) {
                        objs[i] = null;
                        log.error("加载{}方法的参数描述出错", new Object[] { methodName, e });
                    }
                }

                String paramDesc = JSON.toJSONString(objs, SerializerFeature.QuoteFieldNames,
                        SerializerFeature.UseSingleQuotes, SerializerFeature.WriteMapNullValue,
                        SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullListAsEmpty,
                        SerializerFeature.SortField);

                allMethodMapParamDesc.put(methodName, paramDesc);
            }
        }

        log.info("*************allMethodMapParamDesc*******************");
        log.debug("{}", allMethodMapParamDesc);
    }

    public static boolean isWrapClass(Class<?> clz) {
        try {
            return clz.isPrimitive() || clz.getName().startsWith("java.lang");
        } catch (Exception e) {
            return false;
        }
    }
}
