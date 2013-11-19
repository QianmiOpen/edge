/**
 * FileWatchdog.java
 */
package com.ofpay.edge.util;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 文件监控器，监控文件的状态，当文件发生修改时，触发事件
 * </p>
 * @author Angus
 * @date 2013-3-1
 * @version 1.0
 * @since 1.0
 */
public abstract class FileWatchdog extends Thread {

    public static final long DEFAULT_DELAY = 60000;

    private static final Logger logger = LoggerFactory.getLogger(FileWatchdog.class);

    private static final String DEFAULT_FILTER_PARTTEN = ".*.(jar|zip)";

    /**
     * The name of the file to observe for changes.
     */
    private String watchfilePath;

    private String filterPatten;

    /**
     * The delay to observe between every check. By default set {@link #DEFAULT_DELAY}.
     */
    private long delay = DEFAULT_DELAY;

    private Map<String, Long> lastModifMap = new HashMap<String, Long>();

    private boolean interrupted = false;

    public FileWatchdog(String watchfilePath) {
        this(watchfilePath, DEFAULT_FILTER_PARTTEN);
    }

    public FileWatchdog(String watchfilePath, String filterPatten) {
        super("FileWatchdog");
        this.watchfilePath = watchfilePath;
        this.filterPatten = filterPatten;
        setDaemon(true);
    }

    /**
     * Set the delay to observe between each check of the file changes.
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    protected abstract void doOnChange(File file);

    /**
     * 循环遍历目录，找出所有的JAR包
     */
    private void loopFiles(File file, Set<File> files) {
        if (file.isDirectory()) {
            File[] tmps = file.listFiles();
            for (File tmp : tmps) {
                loopFiles(tmp, files);
            }
        } else {
            if (Pattern.matches(filterPatten, file.getName())) {
                files.add(file);
            }
        }
    }

    protected void checkAndConfigure(String watchfilePath) {
        if (StringUtils.hasText(watchfilePath)) {
            File watchFile = new File(watchfilePath);
            if (!watchFile.exists()) {
                return;
            }

            try {
                Set<File> fileSet = new HashSet<File>();
                this.loopFiles(watchFile, fileSet); // 加载所有文件

                for (Iterator<File> iterator = fileSet.iterator(); iterator.hasNext();) {
                    File file = iterator.next();
                    if (file.exists()) {
                        long l = file.lastModified(); // this can also throw a SecurityException

                        String fileAbsPath = file.getAbsolutePath();
                        long lastModif = getLastModif(fileAbsPath);

                        if (l > lastModif) { // however, if we reached this point this
                            recordLastModify(fileAbsPath, l);
                            doOnChange(file);
                        }
                    }
                }
            } catch (Throwable ex) {
                logger.error("Check and load file field! ", ex);
            }
        }

    }

    private long getLastModif(String key) {
        long lastModif = 0;
        if (lastModifMap.get(key) != null) {
            lastModif = lastModifMap.get(key).longValue();
        }
        return lastModif;
    }

    private void recordLastModify(String key, Long lastModif) {
        lastModifMap.put(key, lastModif);
    }

    public void run() {
        while (!interrupted) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // no interruption expected
            }
            checkAndConfigure(watchfilePath);
        }
    }

    public void shutDown() {
        this.interrupted = true;
    }
}
