package com.ofpay.edge.listener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ofpay.edge.InterfaceExecutor;
import com.ofpay.edge.InterfaceLoader;
import com.ofpay.edge.bean.ServiceBean;
import com.ofpay.edge.util.Tool;

/**
 * Created with IntelliJ IDEA.
 * User: caozupeng
 * Date: 13-3-12
 * Time: 下午8:56
 * To change this template use File | Settings | File Templates.
 */
@Component
public class NotifyMe implements InitializingBean, DisposableBean, NotifyListener {

    private static final URL SUBSCRIBE = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "",
            Constants.INTERFACE_KEY, Constants.ANY_VALUE, Constants.GROUP_KEY, Constants.ANY_VALUE,
            Constants.VERSION_KEY, Constants.ANY_VALUE, Constants.CLASSIFIER_KEY, Constants.ANY_VALUE,
            Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY, Constants.ENABLED_KEY, Constants.ENABLED_KEY,
            Constants.CHECK_KEY, String.valueOf(false));

    private static final AtomicLong ID = new AtomicLong();

    private static Logger logger = LoggerFactory.getLogger(NotifyMe.class);

    private final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> registryCache = new ConcurrentHashMap<String, ConcurrentMap<String, Map<Long, URL>>>();

    @Autowired
    private RegistryService registryService;

    // private ApplicationContext applicationContext;

    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache() {
        return registryCache;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(SUBSCRIBE, this);
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Init NotifyMe...");
        registryService.subscribe(SUBSCRIBE, this);
    }

    public void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        final Map<String, Map<String, Map<Long, URL>>> categories = new HashMap<String, Map<String, Map<Long, URL>>>();
        for (URL url : urls) {
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
            if (Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) { // 注意：empty协议的group和version为*
                ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
                if (services != null) {
                    String group = url.getParameter(Constants.GROUP_KEY);
                    String version = url.getParameter(Constants.VERSION_KEY);
                    // 注意：empty协议的group和version为*
                    if (!Constants.ANY_VALUE.equals(group) && !Constants.ANY_VALUE.equals(version)) {
                        services.remove(url.getServiceKey());
                    } else {
                        for (Map.Entry<String, Map<Long, URL>> serviceEntry : services.entrySet()) {
                            String service = serviceEntry.getKey();
                            if (Tool.getInterface(service).equals(url.getServiceInterface())
                                    && (Constants.ANY_VALUE.equals(group) || StringUtils.isEquals(group,
                                            Tool.getGroup(service)))
                                    && (Constants.ANY_VALUE.equals(version) || StringUtils.isEquals(version,
                                            Tool.getVersion(service)))) {
                                services.remove(service);
                            }
                        }
                    }
                }
            } else {
                Map<String, Map<Long, URL>> services = categories.get(category);
                if (services == null) {
                    services = new HashMap<String, Map<Long, URL>>();
                    categories.put(category, services);
                }
                String service = url.getServiceKey();
                initContext(url);
                Map<Long, URL> ids = services.get(service);
                if (ids == null) {
                    ids = new HashMap<Long, URL>();
                    // 提前占位，确保并发无问题
                    services.put(service, ids);
                }
                ids.put(ID.incrementAndGet(), url);
            }

        }
        for (Map.Entry<String, Map<String, Map<Long, URL>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
            if (services == null) {
                services = new ConcurrentHashMap<String, Map<Long, URL>>();
                registryCache.put(category, services);
            }
            services.putAll(categoryEntry.getValue());
        }
    }

    protected void initContext(URL url) {
        String clazzName = url.getPath();
        String version = url.getParameter("version");
        String host = url.getHost();
        String revision = url.getParameter("revision");
        int port = url.getPort();

        ServiceBean serviceBean = new ServiceBean();
        serviceBean.setClazzName(clazzName);
        serviceBean.setHost(host);
        serviceBean.setPort(port);
        serviceBean.setRevision(revision);
        serviceBean.setVersion(version);

        List<String> methodNames = Arrays.asList(url.getParameter("methods").split(","));

        try {
            Class<?> clazz = Class.forName(clazzName);
            Object l = InterfaceExecutor.getServiceBean(serviceBean);

            logger.info("got {} in registry !!!!", clazzName);
            String beanName = clazzName.substring(clazzName.lastIndexOf(".") + 1);
            InterfaceLoader.allBeanMap.put(beanName, l);

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodNames.contains(methodName))
                    try {
                        String key = beanName + "." + methodName;
                        InterfaceLoader.allMethodMap.put(key, method);
                        Class<?>[] types = method.getParameterTypes();

                        if (types != null) {
                            Object[] objs = new Object[types.length];

                            for (int i = 0; i < objs.length; i++) {
                                try {
                                    Class<?> type = types[i];
                                    if (InterfaceLoader.isWrapClass(type) || type.isEnum() || type.isInterface()) {
                                        objs[i] = null;
                                    } else if (type.isArray()) {
                                        objs[i] = new Object[0];
                                    } else {
                                        objs[i] = type.newInstance();
                                    }
                                } catch (Exception e) {
                                    objs[i] = null;
                                    logger.error("加载{}方法的参数描述出错", new Object[] { key, e });
                                }
                            }

                            String paramDesc = JSON.toJSONString(objs, SerializerFeature.QuoteFieldNames,
                                    SerializerFeature.UseSingleQuotes, SerializerFeature.WriteMapNullValue,
                                    SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullListAsEmpty,
                                    SerializerFeature.SortField);

                            InterfaceLoader.allMethodMapParamDesc.put(key, paramDesc);
                        }
                    } catch (Exception e2) {
                        logger.warn("can not found method {} in class", methodName);
                    }
            }

        } catch (Exception e) {
            logger.warn("can not found bean {} in context", clazzName);
        }
    }
}
