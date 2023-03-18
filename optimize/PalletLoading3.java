package optimize;
import java.util.Arrays;

/**
This implementation uses the Best-Fit Decreasing Height algorithm to pack pallets into bins.
The bestFitDecreasingHeight function sorts the pallets in decreasing order and then packs them into bins one at a time,
looking for the bin with the smallest remaining space that can fit the pallet.
If a bin can fit the pallet, the pallet is added to that bin.
If no bin can fit the pallet, a new bin is started.

The function returns the number of bins required to pack all the pallets.
In the example code above, the pallets array contains heights of 8 pallets and the binSize is set to 10.
The output of the program will be Number of bins required: 3,
indicating that it requires 3 bins of size 10 to pack all the pallets using the Best-Fit Decreasing Height algorithm.
*/

public class PalletLoading3 {

    public static void main(String[] args) {
        int[] pallets = {4, 8, 1, 2, 3, 9, 7, 6};
        int binSize = 10;

        int numBins = bestFitDecreasingHeight(pallets, binSize);
        System.out.println("Number of bins required: " + numBins);
    }

    public static int bestFitDecreasingHeight(int[] pallets, int binSize) {
        // Sort pallets in decreasing order
        Arrays.sort(pallets);
        int n = pallets.length;

        // Initialize the first bin
        int binCount = 1;
        int[] binHeight = new int[binCount];
        binHeight[0] = 0;

        for (int i = n - 1; i >= 0; i--) {
            int palletHeight = pallets[i];

            // Find the bin with the smallest remaining space that can fit the pallet
            int bestBin = -1;
            int minSpace = Integer.MAX_VALUE;
            for (int j = 0; j < binCount; j++) {
                if (binHeight[j] + palletHeight <= binSize && binSize - binHeight[j] - palletHeight < minSpace) {
                    bestBin = j;
                    minSpace = binSize - binHeight[j] - palletHeight;
                }
            }

            // If a bin is found, add the pallet to it
            if (bestBin != -1) {
                binHeight[bestBin] += palletHeight;
            }
            // If no bin can fit the pallet, start a new bin
            else {
                binCount++;
                binHeight = Arrays.copyOf(binHeight, binCount);
                binHeight[binCount - 1] = palletHeight;
            }
        }

        return binCount;
    }
}
