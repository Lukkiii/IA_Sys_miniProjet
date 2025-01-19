import java.util.List;

public class FireStatistics {
    private int totalSurvivors;
    private int rescuedSurvivors;
    private int deadSurvivors;
    private int initialFireCells;
    private int currentFireCells;
    private int maxFireCells;
    private final long startTime;
    private double survivalRate;
    private double fireControlRate;

    public FireStatistics() {
        this.startTime = System.currentTimeMillis();
        this.maxFireCells = 0;
    }

    public void updateStatistics(double[][] fireGrid, List<Survivor> survivors, double threshold) {
        totalSurvivors = survivors.size();
        rescuedSurvivors = 0;
        deadSurvivors = 0;
        
        for (Survivor survivor : survivors) {
            if (survivor.isRescued()) rescuedSurvivors++;
            if (survivor.isDead()) deadSurvivors++;
        }

        // Calculer le taux de survie
        survivalRate = totalSurvivors == 0 ? 0 : 
            ((double) rescuedSurvivors / totalSurvivors) * 100;

        // Calculer le nombre de cellules de feu actives
        currentFireCells = 0;
        for (double[] row : fireGrid) {
            for (double intensity : row) {
                if (intensity > threshold) {
                    currentFireCells++;
                }
            }
        }

        // Mettre à jour le nombre maximal de cellules de feu
        if (currentFireCells > maxFireCells) {
            maxFireCells = currentFireCells;
        }

        if (initialFireCells == 0) {
            initialFireCells = currentFireCells;
        }

        // Calculer le taux de contrôle du feu
        fireControlRate = maxFireCells == 0 ? 100 : 
            ((double)(maxFireCells - currentFireCells) / maxFireCells) * 100;
    }

    public String getFormattedStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("Survival Rate: %.1f%%\n", survivalRate));
        stats.append(String.format("- Total Survivors: %d\n", totalSurvivors));
        stats.append(String.format("- Rescued: %d\n", rescuedSurvivors));
        stats.append(String.format("- Lost: %d\n", deadSurvivors));
        stats.append(String.format("- Active: %d\n\n", 
            totalSurvivors - rescuedSurvivors - deadSurvivors));

        stats.append(String.format("Fire Control Rate: %.1f%%\n", fireControlRate));
        stats.append(String.format("- Initial Fires: %d\n", initialFireCells));
        stats.append(String.format("- Current Fires: %d\n", currentFireCells));
        stats.append(String.format("- Max Spread: %d\n", maxFireCells));

        // Calculer la durée de la simulation
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        stats.append(String.format("\nSimulation Time: %02d:%02d\n", 
            duration / 60, duration % 60));

        return stats.toString();
    }

    // Getters
    public double getSurvivalRate() { return survivalRate; }
    public double getFireControlRate() { return fireControlRate; }
}
