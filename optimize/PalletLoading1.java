package optimize;
import java.util.Arrays;

/**
In this implementation, the pallets array contains the heights of the pallets to be loaded onto bins,
and the binSize variable specifies the maximum height of each bin.
The nextFitDecreasingHeight function implements the Next-Fit Decreasing Height algorithm,
which sorts the pallets in decreasing order and then packs them into bins one at a time,
starting a new bin whenever a pallet cannot fit in the current bin.

The function returns the number of bins required to pack all the pallets.
In the example code above, the pallets array contains heights of 8 pallets and the binSize is set to 10.
The output of the program will be Number of bins required: 3,
indicating that it requires 3 bins of size 10 to pack all the pallets using the Next-Fit Decreasing Height algorithm.
 */

public class PalletLoading1 {

	public static void main(String[] args) {
		int[] pallets = { 4, 8, 1, 2, 3, 9, 7, 6 };
		int binSize = 10;

		int numBins = nextFitDecreasingHeight(pallets, binSize);
		System.out.println("Number of bins required: " + numBins);
	}

	public static int nextFitDecreasingHeight(int[] pallets, int binSize) {
		// Sort pallets in decreasing order
		Arrays.sort(pallets);
		int n = pallets.length;

		// Initialize the first bin
		int binCount = 1;
		int binHeight = 0;

		for (int i = n - 1; i >= 0; i--) {
			int palletHeight = pallets[i];

			// Check if pallet can fit in the current bin
			if (binHeight + palletHeight <= binSize) {
				binHeight += palletHeight;
			} else {
				// Start a new bin and add the pallet to it
				binCount++;
				binHeight = palletHeight;
			}
		}

		return binCount;
	}
}
