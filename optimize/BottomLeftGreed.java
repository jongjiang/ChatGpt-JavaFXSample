/**
The Bottom Left algorithm with greed method is a way of traversing a 2D array by always moving down or left, 
and choosing the largest adjacent value to continue the path.

In this code, the bottomLeftGreed method takes a 2D array as input 
and returns an array of integers representing the path traversed by the algorithm. 
The path array has a length of row + col - 1, 
where row is the number of rows in the input array and col is the number of columns.

The algorithm starts at the bottom-left corner of the array (arr[row - 1][0]) 
and moves either down or left to the adjacent cell with the larger value. 
The while loop continues until the algorithm reaches the top-right corner of the array (arr[0][col - 1]). 
The if-else statements inside the loop check the edge cases where the algorithm is at the edge of the array, 
and the else statement chooses the larger adjacent value to continue the path.

In the main method, an example 2D array is created and passed to the bottomLeftGreed method. 
The resulting path array is printed to the console.
*/


public class BottomLeftGreed {
    public static int[] bottomLeftGreed(int[][] arr) {
        int row = arr.length;
        int col = arr[0].length;
        int[] path = new int[row + col - 1];
        int i = row - 1;
        int j = 0;
        int k = 0;
        
        while (i >= 0 && j < col) {
            path[k++] = arr[i][j];
            
            if (i == 0) {
                j++;
            } else if (j == col - 1) {
                i--;
            } else if (arr[i - 1][j] > arr[i][j + 1]) {
                i--;
            } else {
                j++;
            }
        }
        
        return path;
    }
    
    public static void main(String[] args) {
        int[][] arr = {
            {3, 2, 1},
            {4, 5, 6},
            {9, 8, 7}
        };
        int[] path = bottomLeftGreed(arr);
        
        for (int i = 0; i < path.length; i++) {
            System.out.print(path[i] + " ");
        }
        // Output: 9 8 7 6 5 4 3 2 1
    }
}
