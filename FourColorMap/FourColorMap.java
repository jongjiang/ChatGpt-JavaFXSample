import java.util.*;

/**
The Four Color Map Theorem states that given any map on a two-dimensional surface, 
it is always possible to color its regions with no more than four colors, 
such that no two adjacent regions share the same color. 
Here's an implementation in Java using a backtracking algorithm:
*/

public class FourColorMap {
    
    public static void main(String[] args) {
        int[][] map = {
            {0, 1, 0, 1, 0},
            {1, 0, 1, 1, 1},
            {0, 1, 0, 1, 1},
            {1, 1, 1, 0, 1},
            {0, 1, 1, 1, 0}
        };
        int[] colors = solve(map);
        System.out.println(Arrays.toString(colors));
    }
    
    public static int[] solve(int[][] map) {
        int[] colors = new int[map.length];
        Arrays.fill(colors, -1);
        if (colorMap(map, colors, 0)) {
            return colors;
        } else {
            return null;
        }
    }
    
    public static boolean colorMap(int[][] map, int[] colors, int region) {
        if (region == map.length) {
            return true;
        }
        for (int color = 1; color <= 4; color++) {
            if (isValidColor(map, colors, region, color)) {
                colors[region] = color;
                if (colorMap(map, colors, region+1)) {
                    return true;
                }
                colors[region] = -1;
            }
        }
        return false;
    }
    
    public static boolean isValidColor(int[][] map, int[] colors, int region, int color) {
        for (int i = 0; i < map.length; i++) {
            if (map[region][i] == 1 && colors[i] == color) {
                return false;
            }
        }
        return true;
    }
}
