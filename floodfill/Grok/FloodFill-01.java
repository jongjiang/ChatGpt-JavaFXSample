public class FloodFill {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        // Get the original color at starting point
        int oldColor = image[sr][sc];
        
        // If old color is same as new color, no need to process
        if (oldColor == newColor) {
            return;
        }
        
        // Start recursive flood fill
        floodFillHelper(image, sr, sc, oldColor, newColor);
    }
    
    private void floodFillHelper(int[][] image, int row, int col, int oldColor, int newColor) {
        // Check boundaries and if current pixel matches oldColor
        if (row < 0 || row >= image.length || 
            col < 0 || col >= image[0].length || 
            image[row][col] != oldColor) {
            return;
        }
        
        // Replace color at current position
        image[row][col] = newColor;
        
        // Recursively call for all 4 adjacent pixels
        floodFillHelper(image, row + 1, col, oldColor, newColor); // Down
        floodFillHelper(image, row - 1, col, oldColor, newColor); // Up
        floodFillHelper(image, row, col + 1, oldColor, newColor); // Right
        floodFillHelper(image, row, col - 1, oldColor, newColor); // Left
    }
    
    // Example usage
    public static void main(String[] args) {
        int[][] image = {
            {1, 1, 1},
            {1, 1, 0},
            {1, 0, 1}
        };
        
        FloodFill ff = new FloodFill();
        ff.floodFill(image, 1, 1, 2);
        
        // Print result
        for (int[] row : image) {
            for (int pixel : row) {
                System.out.print(pixel + " ");
            }
            System.out.println();
        }
    }
}