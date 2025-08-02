import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Java 8 implementation of the Flood Fill algorithm using a Breadth-First
 * Search (BFS) approach. This approach is iterative and uses a Queue, which
 * prevents StackOverflowErrors that can occur with a recursive implementation
 * on large grids.
 */
public class FloodFill {

	// Represents a coordinate in the 2D grid.
	private static class Coordinate {
		int r, c;

		Coordinate(int r, int c) {
			this.r = r;
			this.c = c;
		}
	}

	/**
	 * Performs the multithreaded flood fill operation.
	 *
	 * @param image      The 2D integer array representing the image grid.
	 * @param sr         The starting row (source row).
	 * @param sc         The starting column (source column).
	 * @param newColor   The new color to fill with.
	 * @param numThreads The number of worker threads to use.
	 * @return The modified grid after the flood fill operation.
	 */
	public int[][] floodFill(int[][] image, int sr, int sc, int newColor, int numThreads) {
		// Handle edge cases.
		if (image == null || sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length) {
			return image;
		}

		int originalColor = image[sr][sc];
		if (originalColor == newColor) {
			return image;
		}

		int rows = image.length;
		int cols = image[0].length;

		// Use a thread-safe queue for coordinates to fill.
		Queue<Coordinate> queue = new ArrayDeque<>();
		// Lock to ensure thread-safe access to the queue and the image.
		Lock lock = new ReentrantLock();
		// Atomic counter to track active worker threads.
		AtomicInteger activeWorkers = new AtomicInteger(0);

		// Put the starting coordinate in the queue and change its color.
		lock.lock();
		try {
			queue.offer(new Coordinate(sr, sc));
			image[sr][sc] = newColor;
		} finally {
			lock.unlock();
		}

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		for (int i = 0; i < numThreads; i++) {
			executor.submit(() -> {
				activeWorkers.incrementAndGet();
				try {
					while (true) {
						Coordinate current = null;
						lock.lock();
						try {
							// Poll a coordinate from the queue.
							current = queue.poll();
						} finally {
							lock.unlock();
						}

						// If the queue is empty, break from the loop.
						if (current == null) {
							break;
						}

						// Define the directions for moving to adjacent cells (up, down, left, right).
						int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
						for (int[] direction : directions) {
							int newRow = current.r + direction[0];
							int newCol = current.c + direction[1];

							// Check if the new coordinate is within the grid boundaries and has the
							// original color.
							if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
								lock.lock();
								try {
									if (image[newRow][newCol] == originalColor) {
										// Change the color and add the new coordinate to the queue.
										image[newRow][newCol] = newColor;
										queue.offer(new Coordinate(newRow, newCol));
									}
								} finally {
									lock.unlock();
								}
							}
						}
					}
				} finally {
					activeWorkers.decrementAndGet();
				}
			});
		}

		// Shutdown the executor and wait for all tasks to complete.
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return image;
	}

    /**
     * Main method for demonstrating the multithreaded flood fill algorithm.
     */
    public static void main(String[] args) {
        FloodFill solution = new FloodFill();
        int size = 5000;
        int rows = size, cols = size;
        int[][] image = new int[rows][cols];

        // Initialize a test image with a large contiguous area of color 1.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                image[i][j] = 1;
            }
        }
        image[rows / 2][cols / 2] = 0; // A small area with a different color.

        int sr = 0; // start row
        int sc = 0; // start column
        int newColor = 2; // new color to fill
        int numThreads = Runtime.getRuntime().availableProcessors(); // Use all available cores.

        System.out.println("Starting multithreaded flood fill on a " + rows + "x" + cols + " matrix with " + numThreads + " threads.");
        
        long startTime = System.nanoTime();
        int[][] filledImage = solution.floodFill(image, sr, sc, newColor, numThreads);
        long endTime = System.nanoTime();
        
        long duration = (endTime - startTime);
        double durationInMs = duration / 1000000.0;
        
        System.out.println("Flood fill completed in: " + durationInMs + " ms");
        
        // To avoid printing a large matrix, we'll just check a single cell.
        System.out.println("\nColor of cell (" + sr + ", " + sc + ") is now: " + filledImage[sr][sc]);
        
        // Note: The printImage method is commented out to avoid printing a large matrix
        // which would slow down the demonstration significantly.
        // printImage(filledImage);
    }
}
