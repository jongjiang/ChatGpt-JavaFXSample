package optimize;
import java.util.*;

public class IrregularStockCutting {

	public static void main(String[] args) {

		int stockWidth = 100;
		int stockHeight = 100;
		int[][] pieces = { { 40, 30 }, { 20, 50 }, { 60, 10 }, { 10, 10 }, { 30, 20 }, { 20, 10 }, { 50, 30 } };

		List<int[]> result = findBestFit(stockWidth, stockHeight, pieces);

		for (int[] piece : result) {
			System.out.println("Cut a piece with width " + piece[0] + " and height " + piece[1]);
		}
	}

	private static List<int[]> findBestFit(int stockWidth, int stockHeight, int[][] pieces) {
		List<int[]> result = new ArrayList<>();
		int[][] stock = new int[stockHeight][stockWidth];

		for (int[] piece : pieces) {
			int bestX = -1;
			int bestY = -1;
			int bestScore = -1;

			for (int y = 0; y <= stockHeight - piece[1]; y++) {
				for (int x = 0; x <= stockWidth - piece[0]; x++) {
					int score = calculateScore(stock, x, y, piece[0], piece[1]);
					if (score > bestScore) {
						bestX = x;
						bestY = y;
						bestScore = score;
					}
				}
			}

			if (bestScore == -1) {
				throw new RuntimeException("Piece " + piece[0] + "x" + piece[1] + " cannot be placed");
			}

			result.add(new int[] { bestX, bestY, piece[0], piece[1] });
			placePiece(stock, bestX, bestY, piece[0], piece[1]);
		}

		return result;
	}

	private static int calculateScore(int[][] stock, int x, int y, int w, int h) {
		int score = 0;

		for (int i = y; i < y + h; i++) {
			for (int j = x; j < x + w; j++) {
				if (stock[i][j] == 1) {
					return -1;
				}
				score += 1;
			}
		}

		return score;
	}

	private static void placePiece(int[][] stock, int x, int y, int w, int h) {
		for (int i = y; i < y + h; i++) {
			for (int j = x; j < x + w; j++) {
				stock[i][j] = 1;
			}
		}
	}
}
