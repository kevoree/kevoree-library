package org.kevoree.library;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaTag implements Comparable<ImaggaTag> {
    private final Double confidence;
    private final String tag;

    public ImaggaTag(final Double confidence, final String tag) {
        this.confidence = confidence;
        this.tag = tag;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getTag() {
        return tag;
    }


    @Override
    public int compareTo(ImaggaTag o) {
        if(o == null) {
            return 1;
        }
        if(ObjectUtils.compare(confidence, o.confidence) != 0) {
           return ObjectUtils.compare(confidence, o.confidence);
        }

        return  ObjectUtils.compare(tag, o.tag);
    }
}
