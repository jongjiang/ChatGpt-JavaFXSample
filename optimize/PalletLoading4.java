package optimize;

import java.util.Arrays;

/**
This implementation uses the Bottom-Left algorithm to pack pallets into bins.
The bottomLeft function sorts the pallets in decreasing order and then packs them into bins one at a time,
looking for the first bin with enough remaining height to fit the pallet.
If a bin is found, the pallet is added to that bin.
If no bin can fit the pallet, a new bin is started at the bottom left corner.

The function returns the number of bins required to pack all the pallets.
In the example code above, the pallets array contains heights of 8 pallets and the binWidth and binHeight are both set to 10.
The output of the program will be Number of bins required: 3,
indicating that it requires 3 bins of size 10x10 to pack all the pallets using the Bottom-Left algorithm.
 */

public class PalletLoading4 {

    public static void main(String[] args) {
        int[] pallets = {4, 8, 1, 2, 3, 9, 7, 6};
        int binWidth = 10;
        int binHeight = 10;

        int numBins = bottomLeft(pallets, binWidth, binHeight);
        System.out.println("Number of bins required: " + numBins);
    }

    public static int bottomLeft(int[] pallets, int binWidth, int binHeight) {
        // Sort pallets in decreasing order
        Arrays.sort(pallets);
        int n = pallets.length;

        // Initialize the first bin
        int binCount = 1;
        int[] binHeightRemaining = new int[binCount];
        binHeightRemaining[0] = binHeight;
        int[][] bin = new int[binCount][2];
        bin[0][0] = 0;
        bin[0][1] = 0;

        for (int i = n - 1; i >= 0; i--) {
            int palletWidth = pallets[i];
            int palletHeight = pallets[i];

            // Find the first bin with enough remaining height to fit the pallet
            int bestBin = -1;
            for (int j = 0; j < binCount; j++) {
                if (bin[j][0] + palletWidth <= binWidth && binHeightRemaining[j] >= palletHeight) {
                    bestBin = j;
                    break;
                }
            }

            // If a bin is found, add the pallet to it
            if (bestBin != -1) {
                int x = bin[bestBin][0];
                int y = bin[bestBin][1];
                bin[bestBin][1] += palletHeight;
                binHeightRemaining[bestBin] -= palletHeight;

                System.out.println("add " + palletHeight + "x" + palletHeight + " in bin(" + bestBin + ")");
            }
            // If no bin can fit the pallet, start a new bin at the bottom left corner
            else {
                binCount++;
                binHeightRemaining = Arrays.copyOf(binHeightRemaining, binCount);
                binHeightRemaining[binCount - 1] = binHeight;
                //bin = Arrays.copyOf(bin, binCount); //?
                //bin = Arrays.stream(bin).map(int[]::clone).toArray(int[][]::new);
                bin = new int[binCount][2];
                bin[binCount - 1][0] = 0;
                bin[binCount - 1][1] = bin[binCount - 2][1] + palletHeight;
            }
        }

        return binCount;
    }
}
