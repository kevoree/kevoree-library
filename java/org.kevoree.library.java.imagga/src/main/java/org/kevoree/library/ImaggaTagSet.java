package org.kevoree.library;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaTagSet {
    private final String image;
    private final Set<ImaggaTag> tags;

    public ImaggaTagSet(final String image, final Set<ImaggaTag> tags) {
        this.image = image;
        this.tags = tags;
    }

    public String getImage() {
        return image;
    }

    public Set<ImaggaTag> getTags() {
        return tags;
    }
}
