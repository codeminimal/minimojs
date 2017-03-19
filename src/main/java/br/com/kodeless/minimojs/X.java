package br.com.kodeless.minimojs;

import br.com.kodeless.minimojs.parser.XHTMLParsingException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.spi.Configurator;

import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by eduardo on 11/11/16.
 */
public class X {
    private static final Logger logger = Logger.getLogger(X.class);
    private static Properties properties;
    private static ResourceLoader resourceLoader;
    private static String contextPath;
    private static boolean usingLocalFolder;
    private static boolean runningInServletContainer;


    public static void main(String[] args) throws IOException {
        PrintStreamCallbackSupportDecorator.Callback callback = new PrintStreamCallbackSupportDecorator.Callback() {
            @Override
            public boolean onPrintln(String msg) {
                if (msg.startsWith("log4j:WARN No appenders could be found for logger")) {
                    Configurator configurator = new PropertyConfigurator();
                    URL url = Loader.getResource("log4j_alternative.properties");
                    configurator.doConfigure(url, LogManager.getLoggerRepository());
                    return false;
                }
                if (msg.startsWith("log4j:WARN Please initialize the log4j system properly.")) {
                    return false;
                }
                return true;
            }
        };

        System.setErr(new PrintStreamCallbackSupportDecorator(System.err, callback));

        if (getArg("--help", args, null) != null) {
            System.out.println("This must be executed in the root folder where the sources are. There must be a X.properties on this folder");
            System.out.println("-c\tApplication context path. Ex: http://localhost:8080/contextpath/startpath");
            System.out.println("-d\tDestination folder of the compiled sources");
            System.out.println("-l\tLocal folder path. In this case the context path doesn't need to be informed. This will be accessed with file://");
            System.exit(0);
        }
        final String currentPath = System.getProperty("user.dir");
        ResourceLoader rl = new ResourceLoader() {
            @Override
            public InputStream get(String path) throws IOException {
                File file = new File(currentPath + "/" + path);
                return file.exists() ? new FileInputStream(file) : null;
            }

            @Override
            public Set<String> getPaths(String path) throws IOException {
                Set<String> set = new HashSet<String>();
                getAllPaths(path, set);
                return set;
            }

            private void getAllPaths(String path, Set<String> set) throws IOException {
                File file = new File(currentPath + "/" + path);
                if (file.isDirectory()) {

                    for (String fileName : file.list()) {
                        file = new File(currentPath + "/" + path + "/" + fileName);
                        if (file.isDirectory()) {
                            getAllPaths(path + "/" + fileName, set);
                        } else {
                            set.add(path + "/" + fileName);
                        }
                    }
                }
            }

            @Override
            public String getRealPath(String path) throws IOException {
                return currentPath + "/" + path;
            }
        };


        String destFolderStr = getArg("-l", args, null);
        String context = getArg("-c", args, "");

        if (destFolderStr == null) {
            try {
                config(context, new FileInputStream(currentPath + "/X.properties"), rl);
            } catch (IOException e) {
                System.err.println("No X.properties file found");
                System.exit(-1);
            }
            destFolderStr = getArg("-d", args, null);
            if (destFolderStr == null) {
                System.err.println("Destination folder must be passed as parameter");
                System.exit(-1);
            }
        } else {
            usingLocalFolder = true;
            try {
                if (context.equals("")) {
                    context = destFolderStr;
                }
                config(context, new FileInputStream(currentPath + "/X.properties"), rl);
            } catch (IOException e) {
                System.err.println("No X.properties file found");
                System.exit(-1);
            }
        }
        File destFolder = new File(destFolderStr);
        if (!destFolder.exists()) {
            System.err.println("Destination folder " + destFolderStr + " does not exist");
            System.exit(-1);
        }
        if (!destFolder.isDirectory()) {
            System.err.println("Path " + destFolderStr + " is not a directory");
            System.exit(-1);
        }

        try {
            load(false, destFolderStr);
        } catch (XHTMLParsingException e) {
            System.err.println("Error parsing an htmx file");
            e.printStackTrace();
            System.exit(-1);
        } catch (ScriptException e) {
            System.err.println("Script error");
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("Error excuting compiler");
            e.printStackTrace();
            System.exit(-1);
        }
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }

    private static String getArg(String name, String[] args, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals(name)) {
                return i < args.length - 1 ? args[i + 1] : "";
            }
        }
        return defaultValue;
    }

    public static void config(String ctxPath, InputStream isProperties, ResourceLoader rl) throws IOException {
        properties = new Properties();
        logger.debug("Loading properties");
        properties.load(isProperties);

        resourceLoader = rl;

        contextPath = ctxPath;
    }

    public static InputStream getResource(String path) throws IOException {
        return resourceLoader.get(path);
    }

    public static Set<String> getResourcePaths(String path) throws IOException {
        return resourceLoader.getPaths(path);
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        String result = (String) properties.get(key);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    public static String getContextPath() {
        return contextPath != null && !contextPath.trim().equals("") ? (usingLocalFolder ? "" : "/") + contextPath : "";
    }

    public static String getRealPath(String path) throws IOException {
        return resourceLoader.getRealPath(path).replace("//", "/");
    }

    public static void load()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, XHTMLParsingException, ScriptException {
        load(true, null);
    }

    private static void load(boolean servletMode, String baseDestPath)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, XHTMLParsingException, ScriptException {

        runningInServletContainer = servletMode;
        String devmode = System.getenv("XDEVMODE");
        if (devmode == null) {
            devmode = X.getProperty("devmode");
        }
        XContext.setDevMode(devmode != null && devmode.equalsIgnoreCase("true"));

        XComponents.loadComponents();

        XObjectsManager.instance.init();

        XResourceManager.instance.init(baseDestPath);

    }

    public static boolean isRunningInServletContainer() {
        return runningInServletContainer;
    }

    private static class PrintStreamCallbackSupportDecorator extends PrintStream {

        public PrintStreamCallbackSupportDecorator(OutputStream out, Callback callback) {
            super(out);
            this.callback = callback;
        }

        public interface Callback {
            public boolean onPrintln(String msg);
        }

        private Callback callback;

        @Override
        public void println(String msg) {
            if (callback.onPrintln(msg)) {
                super.println(msg);
            }
        }
    }
}
