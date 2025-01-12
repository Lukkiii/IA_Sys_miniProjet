import java.util.Random;
public class Fire {
    private FireGrid fireGrid;
    private Random random = new Random();
    private static final double SPREAD_PROBABILITY = 0.3;

    public Fire(int width, int height) {
        this.fireGrid = new FireGrid(width, height);
        initializeMultipleFires();
    }

    private void initializeMultipleFires() {
        int numFires = random.nextInt(2) + 2;
        for (int i = 0; i < numFires; i++) {
            createNewFire();
        }
    }

    private void createNewFire() {
        int maxAttempts = 10;
        int attempts = 0;
        while (attempts < maxAttempts) {
            int x = random.nextInt(fireGrid.getWidth());
            int y = random.nextInt(fireGrid.getHeight());
            if (!isNearHQ(x, y)) {
                double initialIntensity = FireGrid.INITIAL_INTENSITY * (0.8 + random.nextDouble() * 0.2);
                fireGrid.setIntensityAt(x, y, initialIntensity);
                break;
            }
            attempts++;
        }
    }

    public void spread() {
        double[][] currentIntensities = fireGrid.getIntensityGrid();
        double[][] newGrid = new double[fireGrid.getWidth()][fireGrid.getHeight()];
        boolean hasActiveFire = false;
        
        // Copy current state
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                newGrid[i][j] = currentIntensities[i][j];
                if (currentIntensities[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    hasActiveFire = true;
                    if (random.nextDouble() < 0.3) {
                        newGrid[i][j] = Math.min(FireGrid.MAX_INTENSITY, 
                            currentIntensities[i][j] + (random.nextDouble() * 20 - 5));
                    }
                }
            }
        }

        // Process spread
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                if (currentIntensities[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    spreadToNeighbors(i, j, newGrid, currentIntensities[i][j]);
                }
            }
        }

        // Create new fire if none exists
        if (!hasActiveFire) {
            createNewFire();
        }
        
        fireGrid.updateGrid(newGrid);
    }

    private void spreadToNeighbors(int x, int y, double[][] newGrid, double sourceIntensity) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {

                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < fireGrid.getWidth() && ny >= 0 && ny < fireGrid.getHeight()) {
                    if (newGrid[nx][ny] < FireGrid.INTENSITY_THRESHOLD && random.nextDouble() < SPREAD_PROBABILITY) {
                        double spreadIntensity = sourceIntensity * (0.6 + random.nextDouble() * 0.3);
                        spreadIntensity = Math.max(FireGrid.INTENSITY_THRESHOLD + 10, Math.min(FireGrid.MAX_INTENSITY, spreadIntensity));
                        newGrid[nx][ny] = spreadIntensity;
                    }
                }
            }
        }
    }

    private boolean isNearHQ(int x, int y) {
        int hqX = 15;
        int hqY = 15;
        int safeDistance = 5;  // Distance to prevent new fire creation
        double distance = Math.sqrt(Math.pow(x - hqX, 2) + Math.pow(y - hqY, 2));
        return distance < safeDistance;
    }

    public FireGrid getFireGrid() {
        return this.fireGrid;
    }

    public boolean[][] getFireMap() {
        boolean[][] boolMap = new boolean[fireGrid.getWidth()][fireGrid.getHeight()];
        double[][] intensityMap = fireGrid.getIntensityGrid();
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                boolMap[i][j] = intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD;
            }
        }
        return boolMap;
    }

    public double[][] getIntensityMap() {
        return fireGrid.getIntensityGrid();
    }
}