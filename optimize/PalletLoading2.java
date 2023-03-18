package optimize;
import java.util.Arrays;

/**
This implementation is similar to the previous one using the Next-Fit algorithm,
but instead uses the First-Fit algorithm to pack pallets into bins.
The firstFitDecreasingHeight function sorts the pallets in decreasing order and then packs them into bins one at a time,
trying to fit each pallet into an existing bin.
If a pallet cannot fit in any existing bin, a new bin is started.

The function returns the number of bins required to pack all the pallets.
In the example code above, the pallets array contains heights of 8 pallets and the binSize is set to 10.
The output of the program will be Number of bins required: 4,
indicating that it requires 4 bins of size 10 to pack all the pallets using the First-Fit Decreasing Height algorithm.
 */

public class PalletLoading2 {

	public static void main(String[] args) {
		int[] pallets = { 4, 8, 1, 2, 3, 9, 7, 6 };
		int binSize = 10;

		int numBins = firstFitDecreasingHeight(pallets, binSize);
		System.out.println("Number of bins required: " + numBins);
	}

	public static int firstFitDecreasingHeight(int[] pallets, int binSize) {
		// Sort pallets in decreasing order
		Arrays.sort(pallets);
		int n = pallets.length;

		// Initialize the first bin
		int binCount = 1;
		int[] binHeight = new int[binCount];
		binHeight[0] = 0;

		for (int i = n - 1; i >= 0; i--) {
			int palletHeight = pallets[i];

			// Check if pallet can fit in any existing bin
			boolean palletFits = false;
			for (int j = 0; j < binCount; j++) {
				if (binHeight[j] + palletHeight <= binSize) {
					binHeight[j] += palletHeight;
					palletFits = true;
					break;
				}
			}

			// If pallet cannot fit in any existing bin, start a new bin
			if (!palletFits) {
				binCount++;
				binHeight = Arrays.copyOf(binHeight, binCount);
				binHeight[binCount - 1] = palletHeight;
			}
		}

		return binCount;
	}
}
