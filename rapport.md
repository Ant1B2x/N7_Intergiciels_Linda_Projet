Bédex Antoine

Condamine Alexandre

2APPSN

# Rapport - Projet Intergiciels 2020 / 2021

## Sommaire

[TOC]

## Préambule

Ce rapport a pour but de vous présenter le travail réalisé en cours d'Intergiciels à l'ENSEEIHT durant l'année scolaire 2020 / 2021. Seront en premier lieu présentés les quelques choix techniques et des explications relatifs aux différentes parties du projet (mémoire centralisée, client / serveur, etc...). Par la suite, les tests développés seront détaillés.

Pour compléter la lecture de ce rapport, notre code se trouve sur ce répertoire Git : https://github.com/Ant1B2x/N7_Intergiciels_Linda_Projet

Il a été décidé de séparer le projet en deux branches Git distinctes :

- `main` : branche principale, avec sujet fait jusqu'à la sérialisation (incluse) mais pas de redondance de serveur

- `redundantserver` : pareil que la branche principale + ajout de la redondance du serveur principal avec un serveur de secours (modification + ajout de certaines classes)

## Linda centralisé

Cette partie a été sans doute la plus complexe : il s'agissait plus d'un problème de systèmes concurrents que d'intergiciels.

Le problème principal était de gérer la concurrence des accès sur l'espace de tuples. Plusieurs solutions ont été envisagées (blocs `synchronized`, sémaphores, etc...). Finalement, nous avons utilisé la structure de données `CopyOnWriteArrayList` pour stocker nos tuples. Cette structure de données proposée dans `java.util.concurrent` permet de gérer les accès simultanés à une liste via un algorithme de copie à chaque écriture. Nous sommes conscients qu'il ne s'agit pas du choix le plus performant, mais il permettait de rendre le code élégant et moins lourd.

Les méthodes `tryRead`, `tryTake`, `readAll` et `takeAll` n'étant pas bloquantes, leurs algorithmes sont très simples et ne nécessitent pas d'être expliqués (recherche dans la liste, renvoyer les tuples correspondants aux templates passés en paramètres, etc...).

La méthode nous ayant donné le plus de fil à retordre est `eventRegister`. Il s'agissait ici de pouvoir enregistrer un callback pour un type d'action (read ou take) à un moment donné (immédiat ou futur), pour un template donné. Pour gérer cette partie, nous avons crée une classe interne à `CentralizedLinda`, appelée `Event`. Cette classe n'est rien de plus qu'un couple 'template de tuple recherché' - 'callback à appeler'. Elle permet de vérifier si un tuple matche avec le template de l'évènement, et d'appeler le callback relatif à l'évènement. Nous avons ensuite ajouté à `CentralizedLinda` deux listes d'`Event` : une pour les évènements read et une pour les take. En effet, dans nos algorithmes nous avons choisi de toujours privilégier les read aux take (c'est un choix arbitraire pour essayer de faire quelque chose d'un peu plus performant, puisque que quitte à servir un take, autant servir tous les reads avant).

Si le mode fourni à `eventRegister` est immédiat, la méthode va appeler `tryRead` ou `tryTake` et appeler directement le callback si un tuple est trouvé. Sinon (aucun tuple trouvé ou mode futur), un évènement est ajouté à une des deux listes (read ou take). Lors de l'appel à `write`, le noyau va envoyer un tuple à tous les évènements read qui seraient intéressés, puis l'envoyer à un des évènements take. Si aucun évènement take n'est intéressé par ce tuple, il est alors ajouté à l'espace de tuples.

Passons au détail de `read` et `take`. Une classe `LockedCallback` a en fait été développée, il s'agit d'une implantation de `Callback` qui s'initialise avec un sémaphore à 0 et le libère quand le callback est appelé. Cette classe permet de faire une attente passive en attendant qu'un callback soit appelé. `read` et `take` étant bloquants, ils consistent juste à appeler `eventRegister` en mode immédiat avec un `LockedCallback`. Cette solution est très élégante puisqu'elle permet de réutiliser au maximum le code déjà développé.

**À noter qu'après notre passage à l'oral le vendredi 28 Mai, nous avons modifié notre code pour faire passer les deux tests qui ne passaient pas (non-appel de `deepclone` et appel des callbacks avant leur suppression). Nous voulions vous montrer que nous savions quels changement opérer pour faire passer ces tests.**

## Linda client / serveur

Pour cette partie, rien de très extravagant : nous avons suivi l'exemple des RMI du cours. Il s'agissait d'une des solutions les plus simples à mettre en œuvre en Java pour faire des intergiciels, les sockets étants bien moins intuitifs pour ce que nous voulions faire. L'utilisation de files JMS a été envisagée mais paraissait un peu lourde à mettre en place et pas forcément très adaptée.

Une interface `LindaServer` a été développée, elle étend `Remote`. Son implémentation `LindaServerImpl` étend `UnicastRemoteObject`. Cette classe réutilise en interne `CentralizedLinda` via le mécanisme de composition. Son interface est très similaire. Seule petite subtilité ici : nous avons choisi de remplacer la méthode `eventRegister` par la méthode `waitEvent` dont voici le code :

```java
public Tuple waitEvent(Linda.eventMode mode, Linda.eventTiming timing, Tuple template) {
        // Créer un LockedCallback (Callback implémenté avec un sémaphore)
        LockedCallback lc = new LockedCallback();
        // Enregistre le callback sur le Linda en mémoire partagée
        this.linda.eventRegister(mode, timing, template, new AsynchronousCallback(lc));
        // Attend qu'un tuple ait été lu ou pris
        lc.await(); // utilise semaphore.acquire()
        // Retourne le tuple lu ou pris
        return lc.getTuple();
    }
```

On abandonne ici l'idée de callback dans la signature. En effet, étant donné que c'est le client qui va appeler les méthodes de `LindaServer`, il aurait fallu lui permettre d'envoyer un callback au serveur, que le serveur aurait ensuite appelé. Il aurait donc fallu développer une classe implémentant `Callback` en RMI, mais cela paraissait lourd à faire (ajout d'une interface et d'une ou deux classes). Nous sommes cependant conscients que c'est ce qu'ont fait beaucoup de groupe, et **- sploier alert -** nous avons été obligés de le faire pour la partie de tolérance aux fautes. Cette méthode est également élégante puisqu'elle réutilise la classe `LockedCallback` développée précédemment pour Linda centralisé.

Pour la suite de cette partie, nous avons complété la classe `LindaClient` qui était fournie. Elle implémente `Linda` (que nous n'avons pas touché). `LindaClient` récupère juste un `LindaServer` depuis une URI donnée et appelle ses méthodes.

Enfin, la classe `StartServer` contient une méthode main qui crée un `Registry` et y enregistre une instance de `LindaServerImpl`.

## Sérialisation

Cette partie est rapide, nous avons réutilisé la sérialisation de Java. En fait, les méthodes suivantes ont été ajoutées à `CentralizedLinda` :

```java
public void save(String filePath) {
        try {
            FileOutputStream fileWriter = new FileOutputStream(filePath);
            ObjectOutputStream objectWriter = new ObjectOutputStream(fileWriter);
            objectWriter.writeObject(this.tuples);
            objectWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Fatal IO error with " + filePath);
        }
    }
```

```java
public void load(String filePath) {
        try {
            FileInputStream fileReader = new FileInputStream(filePath);
            ObjectInputStream objectReader = new ObjectInputStream(fileReader);
            this.tuples = (CopyOnWriteArrayList<Tuple>) objectReader.readObject();
            objectReader.close();
            fileReader.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Fatal IO error with " + filePath);
        }
    }
```

Comme la classe `Tuple` est sérialisable et que notre structure de données concurrente l'est aussi, aucun problème pour les écrire dans un fichier.

Nous nous sommes un peu amusés à modifier `StartServer` pour qu'elle puisse prendre un argument (optionnel) en ligne de commande qui est le fichier dans lequel lire / écrire des tuples ; et nous avons ajouté un `Hook` à l'interruption du programme pour bien enregistrer les tuples avant de quitter. Si aucun fichier n'est passé en paramètre, le programme ne chargera ni ne sauvegardera de tuples.

## Tolérance aux fautes

Il s'agit de la deuxième partie la plus conséquente du projet et comme nous avons déjà beaucoup (trop) écrit, nous allons être succints. Dans l'architecture imaginée, seuls deux serveurs peuvent exister au même moment (mais il ne serait pas compliqué d'étendre le code à N-serveurs).

L'architecture fonctionne de la sorte :

- Un serveur démarre et s'enregistre sur le `Registry`
- Un serveur (secondaire) démarre, voit qu'un serveur s'est déjà enregistré sur le `Registry`, et en déduit que c'est le serveur principal
- Le serveur secondaire s'enregistre auprès du serveur principal, suite à quoi le serveur principal lui envoie tous les tuples enregistrés
- Le serveur secondaire commence une série de ping-pong : via la méthode `pollPrimary` il appelle la méthode `keepAlive` du serveur principal (qui ne fait rien)
  - Si le serveur principal répond, le serveur secondaire attend 2s et réappelle `pollPrimary`
  - Si le serveur principal ne répond pas (= une erreur réseau est détectée), le serveur secondaire en déduit qu'il est mort et prend sa place en tant que serveur principal via la méthode `becomePrimary`
- Lorsque le serveur principal est appelé par un client (`read`, `write`, etc...), il appelle le même évènement sur le serveur secondaire afin de s'assurer que leurs états évoluent en même temps
  - Pour cette partie, il fallait que le serveur principal puisse partager des callbacks au serveur secondaire, il était donc nécessaire de créer l'interface `RemoteCallback` et son implémentation `RemoteCallbackImpl`, ainsi que la classe `NestedCallback` ; `CentralizedLinda` ne peut en effet prendre que des `Callback` dans `eventRegister`, hors, il était impossible que `RemoteCallback` implémente `Callback` puisque cela faisait changer la signature de `call`, il a donc fallu utiliser ce contournement acrobatique que nous voulions éviter lors de la première version...
  - Petite subtilité pour `write` : pour éviter qu'un callback client soit appelé deux fois, une méthode `writeWithoutCalling` a été ajoutée à `CentralizedLinda`, elle fait exactement la même chose que `write` mais n'appelle pas les callbacks, elle est uniquement utilisée par le serveur secondaire

## Tests développés

Quelques tests ont été développés pour compléter ceux fournis, ils sont dans le package `test` :

| Classe                   | Intérêt                                                      |
| ------------------------ | ------------------------------------------------------------ |
| `BasicTestAsyncCallback` | Teste le bon fonctionnement d'un callback asynchrone.        |
| `BasicTestCallback`      | Teste le bon fonctionnement de `CentralizedLinda` lorsqu'un callback se réenregistre. |
| `TestMultipleRead`       | Anciennement appelé `BasicTest2`. Teste les lectures concurrentes dans l'espace de tuples par plusieurs threads. |
| `TestMultipleTake`       | Teste des takes concurrents dans l'espace de tuples par plusieurs threads. |
| `TestRead`               | Teste le bon fonctionnement de `read`.                       |
| `TestTake`               | Anciennement appelé `BasicTest1`. Teste le bon fonctionnement de `take`. |
| `TestReadAll`            | Teste le bon fonctionnement de `readAll`.                    |
| `TestTakeAll`            | Teste le bon fonctionnement de `takeAll`.                    |
| `TestTryRead`            | Teste le bon fonctionnement de `tryRead`.                    |
| `TestTryTake`            | Teste le bon fonctionnement de `tryTake`.                    |

L'intérêt de ces tests est de couvrir toutes les méthodes développées dans `CentralizedLinda`, mais aussi de pouvoir valider ces mêmes méthodes en architecture client / serveur. Ils n'étaient manifestement pas exhaustif : nous nous en sommes rendu compte lors du passage à l'oral. Nous n'avions pas testé qu'un tuple pouvait être écrit puis modifié, ainsi qu'un callback puisse se réenregistrer avant d'avoir été supprimé (mauvaise compréhension d'un des tests fournis).

Pour pouvoir tester la sérialisation et la tolérance aux fautes, nous nous sommes en revanche servis de l'application `Whiteboard` fournie. Le scénario de test consistait alors à lancer deux serveurs, à dessiner quelques formes puis à tuer le serveur principal pour vérifier que le serveur secondaire reprenait bien la main, à relancer un serveur secondaire, etc...

