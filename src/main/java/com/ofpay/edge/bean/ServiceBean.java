/**
 * ServiceBean.java
 */
package com.ofpay.edge.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 功能描述
 * </p>
 * @author Angus
 * @version 1.0
 * @since 1.0
 */
public class ServiceBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6794572395054831368L;

    private String clazzName;

    private String version;

    private String host;

    private String revision;

    private int port;

    private List<String> methods;

    /**
     * @return the clazzName
     */
    public String getClazzName() {
        return clazzName;
    }

    /**
     * @param clazzName the clazzName to set
     */
    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to set
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the methods
     */
    public List<String> getMethods() {
        return methods == null ? new ArrayList<String>(0) : methods;
    }

    /**
     * @param methods the methods to set
     */
    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

}
