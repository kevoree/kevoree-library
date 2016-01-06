package org.kevoree.library.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mleduc on 05/01/16.
 */
public class ParamService {
    @NotNull
    public List<String> computeParamToList(String param) {
        List<String> list = new ArrayList<>();
        if (param != null && !param.isEmpty()) {
            list = Arrays.asList(param.split(" "));
        }
        return list;
    }
}
