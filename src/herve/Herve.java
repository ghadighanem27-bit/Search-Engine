package herve;

import java.util.Arrays;

public class Herve {

    public static void main(String[] args) {
        if (args.length < 1) {
            afficherAide();
            return;
        }

        String commande = args[0];

        switch (commande) {

            case "ask":
                // Concatène tous les arguments restants pour former la requête.
                afficherResultatsRequete(String.join(" ", Arrays.asList(args).subList(1, args.length)));
                break;

            case "run":
                lancerInterfaceTextuelle();
                break;

            case "web":
                lancerServeurWeb();
                break;

            default:
                System.out.println("Commande inconnue : " + commande);
                afficherAide();
        }
    }

    private static void afficherAide() {
        System.out.println("Utilisation : herve <commande>");
        System.out.println("Commandes disponibles :");
        System.out.println("  ask    - Effectue une requête et affiche le résultat.");
        System.out.println("  run     - Lance le mode interractif permettant d'effectuer des requêtes en ligne de commande ('exit' pour quitter).");
        System.out.println("  web     - Lance un serveur web interactif pour effectuer des recherches");
    }


    private static void afficherResultatsRequete(String requete) {
        // TODO : à implémenter
        System.out.println("Affichage des résultats de la requête \"" + requete + "\".");
    }

    private static void lancerInterfaceTextuelle() {
        // TODO : à implémenter
        System.out.println("Démarrage de la boucle d'interaction en mode textuel...");
    }

    private static void lancerServeurWeb() {
        // TODO : à implémenter
        System.out.println("Lancement du serveur web pour effectuer les requêtes...");
    }
}
