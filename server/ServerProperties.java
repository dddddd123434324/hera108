package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * @author Emilyx3
 */
public class ServerProperties {

    public static final String WZ_PATH;
    private static final Properties props = new Properties();

    private ServerProperties() {
    }

    static {
        loadProperties("server.properties");
        loadPropertiesOptional("server.private.properties");

        WZ_PATH = System.getProperty("net.sf.odinms.wzpath", "wz");
        System.setProperty("net.sf.odinms.wzpath", WZ_PATH);
    }

    public static void loadProperties(String s) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(s);
            props.load(fis);
            fis.close();
        } catch (IOException ex) {

        }
    }

    public static void loadPropertiesOptional(String s) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(s);
            Properties pre = new Properties();
            pre.load(fis);
            fis.close();
            fis = new FileInputStream(s);
            if (pre.getProperty("__enable", "true").equalsIgnoreCase("true")) {
                props.load(fis);
                System.out.println("Properties " + s + " is enabled");
            } else {
                System.out.println("Properties " + s + " is disabled");
            }
            fis.close();
        } catch (IOException ex) {
        }
    }

    private static String latin1ToEuckr(String x) {
        if (x == null) {
            return null;
        }
        try {
            return new String(x.getBytes("latin1"), "euckr");
        } catch (UnsupportedEncodingException ex) {
            return x;
        }
    }

    public static String getProperty(String s) {
        return latin1ToEuckr(props.getProperty(s));
    }

    public static String getProperty(String s, String def) {
        return latin1ToEuckr(props.getProperty(s, def));
    }
}
