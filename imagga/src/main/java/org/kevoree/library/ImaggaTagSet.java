package org.kevoree.library;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaTagSet {
    private final String image;
    private final Set<ImaggaTag> tags;
    private final Boolean content;

    public ImaggaTagSet(final String image, final Boolean content, final Set<ImaggaTag> tags) {
        this.image = image;
        this.tags = tags;
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public Set<ImaggaTag> getTags() {
        return tags;
    }

    public Boolean getContent() {
        return content;
    }
}
