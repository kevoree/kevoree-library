package org.kevoree.library.java.haproxy;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 19:08
 */
public class OSHelper {

    public static String read(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }
            br.close();
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

}
