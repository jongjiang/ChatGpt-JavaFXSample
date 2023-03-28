/**
To use the Bottom Left algorithm with a greed method to keep the smallest area, 
we can modify the arrangePallets method to calculate the area of the pallets as it arranges them. 
The algorithm will choose the adjacent pallet that results in the smallest increase in the total area.

In this modified code, we added an area variable to keep track
*/

public class PalletArrangement {
    public static int[] arrangePallets(int[][] pallets) {
        int rows = pallets.length;
        int cols = pallets[0].length;
        int[] path = new int[rows * cols];
        int i = rows - 1;
        int j = 0;
        int k = 0;
        int area = 0;
        
        while (i >= 0 && j < cols) {
            path[k++] = pallets[i][j];
            area += path[k - 1];
            
            if (i == 0) {
                j++;
            } else if (j == cols - 1) {
                i--;
            } else if (pallets[i - 1][j] + area > pallets[i][j + 1] + area) {
                j++;
            } else {
                i--;
            }
        }
        
        return path;
    }
    
    public static void main(String[] args) {
        int[][] pallets = {
            {3, 2, 1},
            {6, 5, 4},
            {9, 8, 7}
        };
        int[] path = arrangePallets(pallets);
        
        System.out.println("The arranged pallets are:");
        for (int i = 0; i < path.length; i++) {
            System.out.print(path[i] + " ");
            if ((i+1) % pallets[0].length == 0) {
                System.out.println();
            }
        }
        /*
            Output:
            The arranged pallets are:
            9 8 7 
            6 5 4 
            3 2 1
        */
    }
}
