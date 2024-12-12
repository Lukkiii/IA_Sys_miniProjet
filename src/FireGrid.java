import java.util.Random;

public class FireGrid {
    private boolean[][] grid;
    private int width;
    private int height;
    private Random random = new Random();

    public FireGrid(int width, int height) {
        this.width = width;
        this.height = height;
        grid = new boolean[width][height];
    }

    public void initializeFire() {
        int startX = random.nextInt(width);
        int startY = random.nextInt(height);
        grid[startX][startY] = true;
    }

    public void updateGrid(boolean[][] newGrid) {
        this.grid = newGrid;
    }

    public boolean[][] getGrid() {
        return this.grid;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean getValueAt(int x, int y) {
        return grid[x][y];
    }

    public void setValueAt(int x, int y, boolean value) {
        grid[x][y] = value;
    }
}
