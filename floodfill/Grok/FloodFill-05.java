import java.util.Stack;

public class FloodFill {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        if (image == null || image.length == 0 || image[0].length == 0 ||
            sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length) {
            return;
        }
        
        int originalColor = image[sr][sc];
        if (originalColor == newColor) {
            return;
        }
        
        int rows = image.length;
        int cols = image[0].length;
        
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{sr, sc});
        image[sr][sc] = newColor;
        
        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int row = curr[0];
            int col = curr[1];
            
            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                stack.push(new int[]{row + 1, col});
            }
            // Up
            if (row - 1 >= 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                stack.push(new int[]{row - 1, col});
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                stack.push(new int[]{row, col + 1});
            }
            // Left
            if (col - 1 >= 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                stack.push(new int[]{row, col - 1});
            }
        }
    }
    
    public static void main(String[] args) {
        int[][] image = {
            {1, 1, 1},
            {1, 1, 0},
            {1, 0, 1}
        };
        
        FloodFill ff = new FloodFill();
        ff.floodFill(image, 1, 1, 2);
        
        // Optimized printing
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                System.out.print(image[i][j] + (j < image[0].length - 1 ? " " : ""));
            }
            System.out.println();
        }
    }
}