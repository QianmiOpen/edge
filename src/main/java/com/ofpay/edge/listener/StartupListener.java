/**
 * StartupListener.java
 */
package com.ofpay.edge.listener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.util.StringUtils;

import com.ofpay.edge.util.ClassLoaderUtil;
import com.ofpay.edge.util.FileWatchdog;

/**
 * <p>
 * 系统启动监听器
 * </p>
 * @author Angus
 * @version 1.0
 * @since 1.0
 */
public class StartupListener implements ServletContextListener {
    private static final String CLASSPATH_URL_PREFIX = "classpath:";

    private static final String FILE_URL_PREFIX = "file:";

    private static final String THIRD_LIB_PATH = "thirdLibPath";

    private static final String THIRD_LIB_REFRESH_INTERVAL = "thirdLibRefreshIntervalSeconds";

    private static final long DEFAULT_INTERVAL_SECONDS = 300;

    private static boolean isUrl(String resourceLocation) {
        if (resourceLocation == null) {
            return false;
        }
        try {
            new URL(resourceLocation);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Exception ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
            // No thread context class loader -> use class loader of this class.
            cl = StartupListener.class.getClassLoader();
        }
        return cl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        String location = servletContext.getInitParameter(THIRD_LIB_PATH);

        // 判断配置的propertiesConfigLocation是否为空
        if (StringUtils.hasText(location)) {
            if (location.startsWith(FILE_URL_PREFIX)) {
                location = location.substring(FILE_URL_PREFIX.length());
            } else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
                // 如果配置的是ClassPath方式，在location中取出classpath:
                location = location.substring(CLASSPATH_URL_PREFIX.length());

                URL url = getDefaultClassLoader().getResource(location);
                location = url.getFile();

            } else if (!StartupListener.isUrl(location)) {
                if (!location.startsWith("/")) {
                    location = "/" + location;
                }

                // 根据serlvet上下文获取文件真实地址
                location = servletContext.getRealPath(location);
            }

            // 获取加载间隔时长
            String intervalString = servletContext.getInitParameter(THIRD_LIB_REFRESH_INTERVAL);

            // 加载Lib文件
            ClassLoaderUtil.loadJarPath(location);

            long interval;
            try {
                interval = Long.parseLong(intervalString);
            } catch (NumberFormatException ex) {
                interval = DEFAULT_INTERVAL_SECONDS;
            }

            FileWatchdog fw = new FileWatchdog(location) {
                @Override
                protected void doOnChange(File file) {
                    ClassLoaderUtil.loadJarFile(file);
                    System.gc();
                }
            };

            // 启动文件监控线程
            fw.setDelay(interval * 1000);
            fw.start();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
