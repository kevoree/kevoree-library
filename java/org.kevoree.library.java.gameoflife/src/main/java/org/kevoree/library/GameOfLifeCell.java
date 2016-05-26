package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Param;

/**
 * Created by mleduc on 23/05/16.
 */
@ComponentType(description = "A game of life cell")
public class GameOfLifeCell {
	@Param(optional = true, defaultValue = "0")
	public Long x;

	@Param(optional = true, defaultValue = "0")
	public Long y;
}
