package org.kevoree.library;

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
        if(ObjectUtilz.compare(confidence, o.confidence) != 0) {
           return ObjectUtilz.compare(confidence, o.confidence);
        }

        return  ObjectUtilz.compare(tag, o.tag);
    }
}
