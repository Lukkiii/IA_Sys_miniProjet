public class FireScenario {
    public static class Parameters {
        // Intensité maximale du feu
        public final double maxIntensity;
        // Intensité initiale du feu
        public final double initialIntensity;
        // Seuil d'intensité pour déclencher la propagation
        public final double intensityThreshold;
        // Probabilité de propagation du feu
        public final double spreadProbability;
        // Description du scénario de feu
        public final String description;

        public Parameters(double maxIntensity, double initialIntensity, 
                        double intensityThreshold, double spreadProbability,
                        String description) {
            this.maxIntensity = maxIntensity;
            this.initialIntensity = initialIntensity;
            this.intensityThreshold = intensityThreshold;
            this.spreadProbability = spreadProbability;
            this.description = description;
        }
    }

    public static final Parameters ELECTRICAL = new Parameters(
        120.0,
        90.0,
        10.0,
        0.4,
        "Feu électrique \n - forte intensité \n - propagation rapide"
    );

    public static final Parameters CHEMICAL = new Parameters(
        150.0,
        100.0,
        15.0,
        0.35,
        "Feu chimique \n - très forte intensité \n - propagation modérée"
    );

    public static final Parameters ORDINARY = new Parameters(
        100.0,
        80.0,
        10.0,
        0.3,
        "Feu ordinaire \n - intensité moyenne \n - propagation modérée"
    );

    public static final Parameters SMOLDERING = new Parameters(
        80.0,
        60.0,
        5.0,
        0.2,
        "Feu couvant \n - faible intensité \n - propagation lente"
    );
}
