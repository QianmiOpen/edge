/**
 * StartupListener.java
 */
package com.ofpay.edge.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ofpay.edge.InterfaceLoader;

/**
 * <p>
 * 系统启动监听器
 * </p>
 * @author Angus
 * @version 1.0
 * @since 1.0
 */
public class StartupListener implements ServletContextListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        String filters = servletContext.getInitParameter("filters");

        // 加载dubbo接口
//        InterfaceLoader.loadAllBean(context, filters.split(","));
//        InterfaceLoader.loadAllMethod(InterfaceLoader.allBeanMap);
//        InterfaceLoader.loadAllMethodMapParamDesc(InterfaceLoader.allMethodMap);
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
