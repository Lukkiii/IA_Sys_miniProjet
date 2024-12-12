import java.util.Random;

public class Fire {
    private FireGrid fireGrid;
    private Random random = new Random();

    public Fire(int width, int height) {
        this.fireGrid = new FireGrid(width, height);
        this.fireGrid.initializeFire();
    }

    // automate cellulaire
    public void spread(){
        boolean[][] newGrid = new boolean[fireGrid.getWidth()][fireGrid.getHeight()];
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                int neighbors = countBurningNeighbors(i, j);
                if (fireGrid.getValueAt(i, j)) {
                    newGrid[i][j] = true;
                } else {
                    newGrid[i][j] = neighbors > 1 && random.nextDouble() < 0.3;
                }
            }
        }
        fireGrid.updateGrid(newGrid);
    }

    private int countBurningNeighbors(int x, int y) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < fireGrid.getWidth() && ny >= 0 && ny < fireGrid.getHeight()) {
                    if (fireGrid.getValueAt(nx, ny) && (dx != 0 || dy != 0)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean[][] getFireMap() {
        return fireGrid.getGrid();
    }
}