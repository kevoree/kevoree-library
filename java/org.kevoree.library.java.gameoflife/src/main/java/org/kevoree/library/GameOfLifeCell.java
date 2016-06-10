package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.log.Log;

/**
 * Created by mleduc on 23/05/16.
 */
@ComponentType(description = "A game of life cell")
public class GameOfLifeCell {
    @Param(optional = true, defaultValue = "0")
    public Long x;

    @Param(optional = true, defaultValue = "0")
    public Long y;

    @Start
    public void start() {
        Log.debug(x + " : " + y);
    }
}
