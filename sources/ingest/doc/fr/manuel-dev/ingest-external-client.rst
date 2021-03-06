ingest-external-client
######################

Utilisation
###########

Paramètres
**********
Les paramètres sont les InputStreams du fichier SIP pour le dépôt dans la base VITAM

La factory
**********

Afin de récupérer le client, une factory a été mise en place.

.. code-block:: java

    // Récupération du client ingest-external 
    IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
      IngestExternalClientFactory.setConfiguration(IngestExternalClientFactory.IngestExternalClientType.MOCK_CLIENT, null);
      // Récupération explicite du client mock
      IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();


Le client
*********


Pour instancier son client en mode Production :

.. code-block:: java

      // Ajouter un fichier functional-administration-client.conf dans /vitam/conf
	  // Récupération explicite du client
     IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();
     

Le client propose actuellement deux méthodes : 

.. code-block:: java

	  Status status();
	  void upload(InputStream stream)