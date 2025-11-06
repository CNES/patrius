12/10/2012

Cet espace sert à stocker les données nécessaires à PATRIUS.
Les données sont classées par thème.

Le code suivant permet à OREKIT d'initialiser le DataProviderManager pour accéder à ces données.

------------------
        // Gestion des Providers
        DataProvidersManager manager = DataProvidersManager.getInstance();

        String path = "../PATRIUS_DATASET/";
        File f = new File(path);

        try {
            manager.addProvider(new DirectoryCrawler(f));
        } catch (OrekitException e) {
            System.out
                    .println("Configuration files not found. Check the path variable");
        }
-------------------

Rmq : le chemin "../PATRIUS_DATASET" est valable si le répertoire PATRIUS_DATASET 
se trouve au même niveau que les projets Java qui utilisent ces données.
        