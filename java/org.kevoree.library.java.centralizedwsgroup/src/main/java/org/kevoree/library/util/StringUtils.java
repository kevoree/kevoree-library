package org.kevoree.library.util;

/**
 *
 * Created by leiko on 1/11/17.
 */
public class StringUtils {

    public static String shrink(String str, int length) {
        if (str != null) {
            if (str.length() > length) {
                return str.substring(0, length);
            } else {
                return str;
            }
        }

        return "";
    }
}
