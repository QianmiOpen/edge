/**
 * StartupListener.java
 */
package com.ofpay.edge.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
        // ServletContext servletContext = sce.getServletContext();
        // WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        // RegistryConfig registry = context.getBean("default-dubbo-registry", RegistryConfig.class);
        // ApplicationConfig application = context.getBean("default-dubbo-application", ApplicationConfig.class);
        //
        // InterfaceLoader.init(registry, application);
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
