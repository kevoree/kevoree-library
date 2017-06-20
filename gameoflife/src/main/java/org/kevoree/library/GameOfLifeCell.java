package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.log.Log;

/**
 * Created by mleduc on 23/05/16.
 */
@ComponentType(version = 1, description = "A game of life cell")
public class GameOfLifeCell {
    @Param(optional = true)
    public Long x = 0L;

    @Param(optional = true)
    public Long y = 0L;

    @Start
    public void start() {
        Log.debug(x + " : " + y);
    }
}
