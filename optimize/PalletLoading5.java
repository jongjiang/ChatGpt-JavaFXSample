package optimize;

import java.util.*;

/**
This code defines two classes, Box and Level, to represent the boxes and pallet levels, respectively.
The PalletLoader class contains the main method which sets up the problem
and applies the Bottom Left algorithm to place the boxes on the pallet.

The Box class simply stores the ID, height, and width of a box.

The Level class represents a single level on the pallet
 */

public class PalletLoading5 {

	static int palletHeight = 8; // height of the pallet
	static int palletWidth = 6; // width of the pallet

	public static void main(String[] args) {
		int[] heights = { 3, 2, 4, 1 }; // heights of each box
		int[] widths = { 2, 4, 1, 3 }; // widths of each box
		List<Box> boxes = new ArrayList<>();
		for (int i = 0; i < heights.length; i++) {
			boxes.add(new Box(i + 1, heights[i], widths[i]));
		}

		List<Level> levels = new ArrayList<>();
		levels.add(new Level(0, palletWidth));
		int currentLevel = 0;
		for (Box box : boxes) {
			boolean boxPlaced = false;
			while (!boxPlaced) {
				Level level = levels.get(currentLevel);
				if (level.canFitBox(box)) {
					level.placeBox(box);
					boxPlaced = true;
				} else {
					currentLevel++;
					if (currentLevel == levels.size()) {
						levels.add(new Level(levels.get(currentLevel - 1).getY() + levels.get(currentLevel - 1).getHeight(), palletWidth));
					}
				}
			}
			currentLevel = 0;
		}

		for (Level level : levels) {
			level.printLevel();
		}
	}

	public static class Box {
		private int id;
		private int height;
		private int width;

		public Box(int id, int height, int width) {
			this.id = id;
			this.height = height;
			this.width = width;
		}

		public int getId() {
			return id;
		}

		public int getHeight() {
			return height;
		}

		public int getWidth() {
			return width;
		}
	}

	public static class Level {
		private int y;
		private int width;
		private int height;
		private boolean boxPlaced;
		private List<Box> boxes = new ArrayList<>();

		public Level(int y, int width) {
			this.y = y;
			this.width = width;
			this.height = 0;
		}

		public int getY() {
			return y;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public boolean canFitBox(Box box) {
			return box.getWidth() <= width && height + box.getHeight() <= PalletLoading5.palletHeight;
		}

		public void placeBox(Box box) {
			boxPlaced = true;
			boxes.add(box);
			height += box.getHeight();
			width -= box.getWidth();
		}

		public void printLevel() {
			System.out.println("Level " + y);
			for (Box box : boxes) {
				System.out.println("Box " + box.getId() + " (" + box.getHeight() + " x " + box.getWidth() + ")");
			}
		}
	}
}
