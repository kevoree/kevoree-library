package org.kevoree.library.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mleduc on 05/01/16.
 */
public class ParamService {

    private final Pattern regex = Pattern.compile("([A-Z0-9_]+=(\"[^\"]+\"|[^\" ]+)?)");

    @NotNull
    public List<String> computeParamToList(String param) {
        List<String> list = new ArrayList<>();
        if (param != null && !param.isEmpty()) {
            list = Arrays.asList(param.split(" "));
        }
        return list;
    }

    public List<String> computeEnvs(String environment) {
        final List<String> ret = new ArrayList<>();
        if(org.apache.commons.lang.StringUtils.isNotBlank(environment)) {

            Matcher regexMatcher = regex.matcher(environment);

            while (regexMatcher.find()) {
                ret.add(regexMatcher.group(0));
            }

        }
        return ret;
    }
}
