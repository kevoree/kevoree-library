package org.kevoree.library;

import java.util.HashMap;
import java.util.List;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Value;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.api.Context;
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.engine.Cell;
import org.kevoree.library.engine.CellGrid;
import org.kevoree.library.engine.GameOfLifeService;
import org.kevoree.library.engine.LifeOperation;
import org.kevoree.library.engine.LifeOperation.LifeOperationType;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.ModelCloner;

/**
 * Created by mleduc on 23/05/16.
 */
@ComponentType(description = "analyse the model and update it to a new cell generation")
public class GameOfLifeEngine {

	@KevoreeInject
	private ModelService modelService;

	@KevoreeInject
	private Context context;

	@KevoreeInject
	private KevScriptService kevscriptService;

	private final GameOfLifeService gameOfLifeService = new GameOfLifeService();
	private final KevoreeFactory factory = new DefaultKevoreeFactory();
	private final ModelCloner cloner = factory.createModelCloner();

	@Input
	public void tick(String time) {
		final CellGrid cellGrid = scanCells();

		final List<LifeOperation> lifeOperations = gameOfLifeService.doLife(cellGrid);

		if (!lifeOperations.isEmpty()) {
			final String kevscript = toKevscript(lifeOperations);
			final ContainerRoot model = cloner.clone(modelService.getCurrentModel().getModel());

			Log.debug("Kevscript :");
			Log.debug(kevscript);

			execute(kevscript, model);
		}
	}

	private void execute(final String kevscript, final ContainerRoot model) {
		try {
			kevscriptService.execute(kevscript, model);
			modelService.update(model, null);
		} catch (Exception e) {
			Log.error(e.getMessage());
		}
	}

	private String toKevscript(final List<LifeOperation> lifeOperations) {
		final StringBuilder kevscriptSB = new StringBuilder();

		int i = 0;
		for (LifeOperation lifeOperation : lifeOperations) {
			if (lifeOperation.type == LifeOperationType.Dead) {
				kevscriptSB.append("remove " + lifeOperation.qualifier + "\n");
			} else {
				final String componentName = "%%cell" + (i++) + "%%";
				
				final String nodeName;
				if(lifeOperation.x <= 0 && lifeOperation.y >= 0) {
					nodeName = "nodeA";
				} else if(lifeOperation.x > 0 && lifeOperation.y >=0) {
					nodeName = "nodeB";
				} else if(lifeOperation.x <=0 && lifeOperation.y < 0) {
					nodeName = "nodeC";
				} else {
					nodeName = "nodeD";
				}
				
				final String fullPath = nodeName + "." + componentName;
				kevscriptSB.append("add " + fullPath + " : GameOfLifeCell\n");
				kevscriptSB.append("set " + fullPath + ".x = '" + lifeOperation.x + "'\n");
				kevscriptSB.append("set " + fullPath + ".y = '" + lifeOperation.y + "'\n");
			}
		}
		final String kevscript = kevscriptSB.toString();
		return kevscript;
	}

	private CellGrid scanCells() {
		final List<ContainerNode> nodes = modelService.getCurrentModel().getModel().getNodes();

		CellGrid cellGrid = new CellGrid();
		for (ContainerNode node : nodes) {
			final List<ComponentInstance> components = node.getComponents();
			for (ComponentInstance component : components) {

				// we only keeps Components which are of type GameOfLifeCell
				if ("GameOfLifeCell".equals(component.getTypeDefinition().getName())) {
					final HashMap<String, Object> mapValues = new HashMap<>();
					for (Value value : component.getDictionary().getValues()) {
						mapValues.put(value.getName(), value.getValue());
					}

					final long x = parseOrZero(mapValues.get("x"));
					final long y = parseOrZero(mapValues.get("y"));
					cellGrid = cellGrid.add(new Cell(x, y, node.getName() + "." + component.getName()));
				}
			}

		}
		return cellGrid;
	}

	private long parseOrZero(Object object) {
		final long x;
		if(object != null) {
			x = Long.parseLong((String) object);
		} else {
			x = 0;
		}
		return x;
	}
}
