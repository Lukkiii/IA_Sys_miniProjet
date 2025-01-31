# IA_Sys_miniProjet

### Fonctionnalités

- **Interface Graphique** : 
  - Visualisation en temps réel de la propagation du feu
  - Affichage des robots et des survivants
  - Panneau d'informations détaillées
  - Boutons de contrôle de simulation

- **Système de Feu** :
  - 4 types de feux différents :
    - Feu électrique: forte intensité, propagation rapide
    - Feu chimique: très forte intensité, propagation modérée  
    - Feu ordinaire: intensité moyenne, propagation modérée
    - Feu couvant: faible intensité, propagation lente
  - Propagation dynamique avec intensités variables
  - Probabilité de propagation configurable
  - Zone de sécurité autour du QG

- **Système de Robots** :
  - Robots éclaireurs pour la détection
  - Robots pompiers pour l'extinction
  - Gestion autonome de l'énergie et des ressources

- **Système de Survivants** :
  - Apparition près des zones d'incendie
  - États de sauvetage dynamiques
  - Conditions de survie basées sur l'intensité du feu

- **Statistiques en Temps Réel** :
  - Taux de survie des victimes
  - Taux de contrôle du feu
  - Nombre de feux actifs/éteints
  - Durée de la simulation
  - Statistiques des survivants (total, sauvés, perdus)

### Composants Techniques

1. **SimulationGUI.java** :
   - Interface graphique interactive
   - Affichage de la grille 24x24
   - Panneau de contrôle
   - Visualisation en temps réel

2. **Simulation.java** :
   - Gestion du cycle de simulation
   - Coordination des robots
   - Mise à jour des états
   - Gestion des survivants

3. **Fire.java** :
   - Gestion de la propagation du feu
   - Initialisation de foyers multiples
   - Algorithme de propagation dynamique
   - Protection de la zone QG

4. **FireGrid.java** :
   - Gestion de la grille d'intensité du feu
   - Contrôle des seuils d'intensité
   - Mise à jour dynamique des intensités
   - Interface avec les scénarios de feu

5. **FireStatistics.java** :
   - Suivi des statistiques en temps réel
   - Calcul des taux de survie et contrôle
   - Génération des rapports

6. **FireScenario.java** :
   - Définition des différents types de feux
   - Paramètres de propagation
   - Caractéristiques spécifiques

7. **Robot.java** (Classes abstraites et dérivées) :
   - Scout : Exploration et détection
   - Firefighter : Extinction des feux
   - Gestion de l'énergie et de l'eau

8. **HeadQuarters.java** :
   - Coordination centrale
   - Déploiement des robots
   - Cartographie globale des incendies

### Comment Exécuter

1. Compiler les fichiers Java :
    ```sh
    javac -d class src/*.java
    ```

2. Exécuter la simulation :
    ```sh
    java -cp class Simulation
    ```

### Détails de la Simulation

- **Dimensions** : Grille 24x24
- **QG** : Position centrale (12,12)
- **Robots** : 
  - 2 éclaireurs initiaux
  - Maximum 7 robots total
- **Survivants** : Maximum 7
