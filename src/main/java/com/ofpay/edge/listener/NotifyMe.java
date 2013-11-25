package com.ofpay.edge.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.ofpay.edge.InterfaceLoader;
import com.ofpay.edge.util.Tool;

/**
 * Created with IntelliJ IDEA.
 * User: caozupeng
 * Date: 13-3-12
 * Time: 下午8:56
 * To change this template use File | Settings | File Templates.
 */
public class NotifyMe implements InitializingBean, DisposableBean, NotifyListener, ApplicationContextAware {

    private static final String[] DEFAULT_SUBSCRIBE_PARAMS = new String[] { Constants.INTERFACE_KEY,
            Constants.ANY_VALUE, Constants.GROUP_KEY, Constants.ANY_VALUE, Constants.VERSION_KEY, Constants.ANY_VALUE,
            Constants.CLASSIFIER_KEY, Constants.ANY_VALUE, Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY,
            Constants.ENABLED_KEY, Constants.ANY_VALUE, Constants.CHECK_KEY, String.valueOf(false) };

    private URL subscribe = null;

    private Map<String, String> subcribeParams = CollectionUtils.toStringMap(DEFAULT_SUBSCRIBE_PARAMS);

    private String urlFilterRegex;

    private ApplicationContext appContext;

    private static final AtomicLong ID = new AtomicLong();

    private static Logger logger = LoggerFactory.getLogger(NotifyMe.class);

    public static final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> registryCache = new ConcurrentHashMap<String, ConcurrentMap<String, Map<Long, URL>>>();

    private RegistryService registryService;

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(subscribe, this);
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Init NotifyMe...");

        // init dubbo application context
        RegistryConfig registry = appContext.getBean("default-dubbo-registry", RegistryConfig.class);
        ApplicationConfig application = appContext.getBean("default-dubbo-application", ApplicationConfig.class);

        InterfaceLoader.init(registry, application);

        // 订阅注册中心的服务变化
        subscribe = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "", subcribeParams);
        registryService.subscribe(subscribe, this);

    }

    public void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        logger.info("********************************************");

        // Map<category, Map<servicename, Map<Long, URL>>>
        final Map<String, Map<String, Map<Long, URL>>> categories = new HashMap<String, Map<String, Map<Long, URL>>>();
        for (URL url : urls) {
            logger.info(url.toFullString());

            String clazzName = url.getPath();
            if (Pattern.matches(urlFilterRegex, clazzName)) { // 过滤非关注的URL

                String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
                if (Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) { // 注意：empty协议的group和version为*
                    ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
                    if (services != null) {
                        String group = url.getParameter(Constants.GROUP_KEY);
                        String version = url.getParameter(Constants.VERSION_KEY);
                        // 注意：empty协议的group和version为*
                        if (!Constants.ANY_VALUE.equals(group) && !Constants.ANY_VALUE.equals(version)) {
                            services.remove(url.getServiceKey());
                            InterfaceLoader.destroyReference(url.getServiceKey(), Constants.ANY_VALUE);
                        } else {
                            for (Map.Entry<String, Map<Long, URL>> serviceEntry : services.entrySet()) {
                                String service = serviceEntry.getKey();
                                // 如果接口相同&&group相同&&版本相同，则下清除缓存
                                if ((Tool.getInterface(service).equals(url.getServiceInterface()) || Tool.getInterface(
                                        service).equals(url.getPath()))
                                        && (Constants.ANY_VALUE.equals(group) || StringUtils.isEquals(group,
                                                Tool.getGroup(service)))
                                        && (Constants.ANY_VALUE.equals(version) || StringUtils.isEquals(version,
                                                Tool.getVersion(service)))) {
                                    services.remove(service);
                                    InterfaceLoader.destroyReference(service, Constants.ANY_VALUE);
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
                    Map<Long, URL> ids = services.get(service);
                    if (ids == null) {
                        ids = new HashMap<Long, URL>();
                        services.put(service, ids);
                    }
                    ids.put(ID.incrementAndGet(), url);
                }

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

    /**
     * @return the subcribeParams
     */
    public Map<String, String> getSubcribeParams() {
        return subcribeParams;
    }

    /**
     * @param subcribeParams the subcribeParams to set
     */
    public void setSubcribeParams(Map<String, String> subcribeParams) {
        this.subcribeParams = subcribeParams;
    }

    /**
     * @return the urlFilterRegex
     */
    public String getUrlFilterRegex() {
        return urlFilterRegex;
    }

    /**
     * @param urlFilterRegex the urlFilterRegex to set
     */
    public void setUrlFilterRegex(String urlFilterRegex) {
        this.urlFilterRegex = urlFilterRegex;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.appContext = applicationContext;
    }
}
