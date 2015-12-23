package org.kevoree.library;

/**
 * Created by mleduc on 23/12/15.
 */
public class ObjectUtilz {
    public static <T extends Comparable<? super T>> int compare(final T c1, final T c2) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return  -1;
        } else if (c2 == null) {
            return  1;
        }
        return c1.compareTo(c2);
    }
}
