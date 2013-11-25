package com.ofpay.edge.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClassLoaderUtil.java
 */

/**
 * <p>
 * 功能描述
 * </p>
 * @author Angus
 * @version 1.0
 * @since 1.0
 */
public final class ClassLoaderUtil {
    /** URLClassLoader的addURL方法 */
    private static Method addURL = initAddMethod();

    private static URLClassLoader system = (URLClassLoader) ClassLoader.getSystemClassLoader();

    private static Logger logger = LoggerFactory.getLogger(ClassLoaderUtil.class);

    /** 初始化方法 */
    private static final Method initAddMethod() {
        try {
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            add.setAccessible(true);
            return add;
        } catch (Exception e) {
            logger.error("Get addURL method failed.", e);
        }
        return null;
    }

    /**
     * 循环遍历目录，找出所有的JAR包
     */
    private static final void loopFiles(File file, List<File> files) {
        if (file.isDirectory()) {
            File[] tmps = file.listFiles();
            for (File tmp : tmps) {
                loopFiles(tmp, files);
            }
        } else {
            if (file.getAbsolutePath().endsWith(".jar") || file.getAbsolutePath().endsWith(".zip")) {
                files.add(file);
            }
        }
    }

    /**
     * <pre>
     * 加载JAR文件
     * </pre>
     * 
     * @param file
     */
    public static final void loadJarFile(File file) {

        if (file == null) {
            return;
        }
        try {
            addURL.invoke(system, new Object[] { file.toURI().toURL() });
            logger.debug("成功加载{}包：", new Object[] { file.getAbsolutePath() });
        } catch (Exception e) {
            logger.error("{}包加载失败.", new Object[] { file.getAbsolutePath(), e });
        }
    }

    /**
     * <pre>
     * 从一个目录加载所有JAR文件
     * </pre>
     * 
     * @param path
     */
    public static final void loadJarPath(String path) {
        List<File> files = new ArrayList<File>();
        File lib = new File(path);
        loopFiles(lib, files);
        for (File file : files) {
            loadJarFile(file);
        }
    }

}
