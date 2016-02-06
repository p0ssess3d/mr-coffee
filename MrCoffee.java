
/*
 * (C) Copyright 2016, Damian Nikodem, All Rights Reserved. Mr. Coffee is free
 * software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without Mr. Coffee the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with Mr. Coffee. If not, see http://www.gnu.org/licenses/.
 *
 * This class _IS_ available for commercial closed source projects. Please email
 * damian.nikodem.au@gmail.com to arrange a License.
 * 
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.ExceptionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;

/**
 * <p>
 * Mr Coffee, A Java class to allow for post startup import of jar files from
 * the web. </p>
 *
 * <p>
 * Due to the nature of the current JVM's one cannot easily import a .JAR file
 * into the jvm after execution has started.
 * </p>
 *
 * @author damian.nikodem.au@gmail.com
 */
public class MrCoffee {

    /**
     * Id Values for ActionEvents to allow for user notification
     */
    public final static int EVENT_DOWNLOAD_STARTED = 0x00;
    public final static int EVENT_FILE_STARTED = 0x01;
    public final static int EVENT_FILE_FINISHED = 0x02;
    public final static int EVENT_FILE_EXISTS = 0x03;
    public final static int EVENT_DOWNLOAD_FINISHED = 0x04;
    public final static int EVENT_DOWNLOAD_EXCEPTION = 0x05;

    private final LinkedList<JarIdentifier> requirements = new LinkedList<>();
    private final boolean useCache;

    /**
     * Default Constructor ( Will Cache .jar files )
     */
    public MrCoffee() {
        this(true);
    }

    /**
     * Constructor with the ability to disable cache. This can be used in the
     * event that
     *
     * @param shouldSaveToCache If set to false then .jar files will be loaded
     * directly over HTTP and not cached to disk, The use case for this is
     * primarily where custom .JAR files are being served.
     */
    public MrCoffee(boolean shouldSaveToCache) {
        this.useCache = shouldSaveToCache;
    }

    /**
     * Basic method to request a .jar file
     *
     * @param url URL of .jar file to request
     */
    public void requestJar(String url) {
        try {
            URL i = new URL(url);
            String filename = i.getPath().substring(i.getPath().lastIndexOf("/"));
            requestJar(filename, url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void requestJar(String name, String url) {
        requestJar(name, url, null);
    }

    /**
     * Advanced method for requesting a .jar file.
     *
     * @param name name of .jar file to be written to disk (if cached.)
     * @param url URL of .jar file to be downloaded.
     * @param checkClass if null ignored, otherwise a classname, if the
     * classname can be resolved then the jar will not attempt to be loaded in
     * the future ( Useful for applications that may be deployed in a number of
     * ways, or for plugin systems where the plugin may or may not already be
     * loaded. )
     */
    public void requestJar(String name, String url, String checkClass) {
        JarIdentifier jid = new JarIdentifier(name);        
        jid.jarUrl = url;
        jid.checkClass = checkClass;
        if (!isClassLoadable(jid)) {
            requirements.add(jid);
        }
    }

    /**
     * Blocking method that Iterates through the list of requested .jar files
     * and loads them into the current JVM.<br/> When this method completes
     * successfully the list of .jar files to load will be cleared.
     *
     * @throws Exception is thrown in the event of a download or classloader
     * error.
     */
    public void loadBasic() throws Exception {
        Method addUrl = getAddUrlMethod();
        BoundedRangeModel dummy = null;
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (!useCache) {
            for (JarIdentifier jar : requirements) {
                addUrl.invoke(systemClassLoader, new URL(jar.jarUrl));
            }
        } else {
            for (JarIdentifier jar : requirements) {
                File target = getCacheFilename(jar);
                if (!target.exists()) {
                    StaticTk.downloadFile(jar.jarUrl, target, dummy);
                }
                addUrl.invoke(systemClassLoader, target.toURI().toURL());
            }
        }
        requirements.clear();
    }

    private final static ActionListener noOpActionListener = (ActionEvent e) -> {
    };
    private final static ExceptionListener noOpExceptionListenerr = (Exception e) -> {
        e.printStackTrace();
    };

    /**
     * Creates a runnable which may be launched via a thread to ASynchronously
     * download
     *
     * All events are thrown in the Thread context which the runnable is
     * executed in which may be a issue if any thread unsafe operations are
     * performed from the listeners.
     *
     * @param exceptionListener Exception Listener to be called in the event of
     * a exception when downloading or loading a .jar file.
     * @param fileProgress Bounded Range Model which may be used to monitor the
     * progress of a individual .jar file ( note: this can be plugged directly
     * into a JProgress bar.)
     * @param actionListener ActionListener which will be called during the
     * process of downloading .jar files, can be used to provide feedback to the
     * user about which file is currently being downloaded.
     * @return a runnable which will load .jar files
     */
    public Runnable loadAsync(ExceptionListener exceptionListener,
            final BoundedRangeModel fileProgress, final ActionListener actionListener) {

        final Method addUrl = getAddUrlMethod();
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        final LinkedList<JarIdentifier> req = new LinkedList<>(requirements);
        requirements.clear();
        final MrCoffee instance = this;
        final ExceptionListener el = (exceptionListener != null ? exceptionListener : noOpExceptionListenerr);
        final ActionListener al = (actionListener != null ? actionListener : noOpActionListener);
        final BoundedRangeModel brm = (fileProgress != null ? fileProgress : new DefaultBoundedRangeModel());

        Runnable runner = () -> {
            al.actionPerformed(new ActionEvent(instance, EVENT_DOWNLOAD_STARTED, "Mr. Coffee"));
            for (JarIdentifier jar : req) {
                File target = getCacheFilename(jar);
                try {
                    if (!target.exists()) {
                        al.actionPerformed(new ActionEvent(instance, EVENT_FILE_STARTED, jar.jarUrl));
                        StaticTk.downloadFile(jar.jarUrl, target, brm);
                        al.actionPerformed(new ActionEvent(instance, EVENT_FILE_FINISHED, jar.jarUrl));
                    } else {
                        al.actionPerformed(new ActionEvent(instance, EVENT_FILE_EXISTS, jar.jarUrl));
                    }
                    addUrl.invoke(systemClassLoader, target.toURI().toURL());
                } catch (Exception e) {
                    el.exceptionThrown(e);
                    al.actionPerformed(new ActionEvent(instance, EVENT_DOWNLOAD_EXCEPTION, jar.jarUrl));
                }
            }
            al.actionPerformed(new ActionEvent(instance, EVENT_DOWNLOAD_FINISHED, "Mr. Coffee"));
        };
        return runner;
    }

    private static Method getAddUrlMethod() {
        for (Method m : URLClassLoader.class.getDeclaredMethods()) {
            if (m.getName().equals("addURL")) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static boolean isClassLoadable(JarIdentifier jid) {
        try {
            Class cx = ClassLoader.getSystemClassLoader().loadClass(jid.checkClass);
            return true;
        } catch (Exception e) {
        }
        return false;
    }
    private static String applicationId = null;
    private static File cacheFolder = null;

    /**
     * Static method to set the application id string for the cache folder.
     * <br/> This method can only be called once during the applications life
     * cycle and should not be called after a load operation. This is to be used
     * to prevent applications from creating jar conflicts in the event that
     * multiple applications use MagicJar on the same computer
     *
     * @param applicationId
     */
    public static void setApplicationId(String applicationId) {
        if (cacheFolder != null) {
            throw new RuntimeException("Can not set Application ID after loading .jar files");
        }
        if (applicationId != null) {
            throw new RuntimeException("Can not set Application ID multiple times");
        }
    }

    /**
     * Clears this applications .JAR cache. Useful for testing
     */
    public static void clearCache() {
        recursiveDelete(getCacheFilename(new JarIdentifier("tmp")).getParentFile());        
    }

    private static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                recursiveDelete(file);
            }
        } else {
            f.delete();
        }
    }

    private static File getCacheFilename(JarIdentifier jid) {
        if (cacheFolder == null) {
            String userHome = System.getProperty("user.home");
            cacheFolder = new File(userHome, ".p0ssess3d.net");
            cacheFolder = new File(cacheFolder.getAbsoluteFile(), ".MrCoffee-Cache");
            if (applicationId != null) {
                cacheFolder = new File(cacheFolder.getAbsoluteFile(), applicationId);
            }
            cacheFolder.mkdirs();
        }
        return new File(cacheFolder, jid.jarName);
    }

}

class JarIdentifier {

    public JarIdentifier(String jarName) {
        this.jarName = jarName;
    }
    String jarName = "";
    String jarUrl = "";
    String checkClass = "";
}

class StaticTk {

    private static int BUFFER_SIZE = 4096;

    public static void downloadFile(String fileURL, File targetFile, BoundedRangeModel boundedRangeModel)
            throws IOException {
        URL url = new URL(fileURL);
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {

                int downloadSize = httpConn.getContentLength();
                boundedRangeModel.setMinimum(0);
                boundedRangeModel.setMaximum(downloadSize);

                try (InputStream inputStream = httpConn.getInputStream()) {
                    int status = 0;
                    int bytesRead;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        status += bytesRead;
                        boundedRangeModel.setValue(status);
                    }
                }
                outputStream.flush();
                boundedRangeModel.setValue(boundedRangeModel.getMaximum());
            } else {
                throw new IOException("Server Returned: " + responseCode);
            }
            httpConn.disconnect();
        }
    }
}
