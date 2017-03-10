package org.kevoree.library.util;

import java.util.UUID;

/**
 *
 * Created by leiko on 1/10/17.
 */
public class ShortId {

    public static String gen() {
        return UUID.randomUUID().toString();
    }
}
