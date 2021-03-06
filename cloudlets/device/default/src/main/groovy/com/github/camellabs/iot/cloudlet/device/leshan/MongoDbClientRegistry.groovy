/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.camellabs.iot.cloudlet.device.leshan

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.Mongo
import org.eclipse.leshan.server.client.Client
import org.eclipse.leshan.server.client.ClientRegistry
import org.eclipse.leshan.server.client.ClientRegistryListener
import org.eclipse.leshan.server.client.ClientUpdate

import java.util.concurrent.CopyOnWriteArrayList

import static com.github.camellabs.iot.cloudlet.device.leshan.ClientWrapper.clientWrapperFromMap
import static io.rhiot.mongodb.Mongos.discoverMongo

class MongoDbClientRegistry implements ClientRegistry {

    private final Mongo mongo

    private final ObjectMapper objectMapper

    private final List<ClientRegistryListener> listeners = new CopyOnWriteArrayList<>()

    MongoDbClientRegistry(Mongo mongo, ObjectMapper objectMapper) {
        this.mongo = mongo
        this.objectMapper = objectMapper
    }

    MongoDbClientRegistry(Mongo mongo) {
        this(mongo, defaultObjectMapper())
    }

    MongoDbClientRegistry() {
        this(discoverMongo())
    }

    @Override
    Client get(String endpoint) {
        def client = clientsCollection().findOne(new BasicDBObject([endpoint: endpoint]))
        client == null ? null : clientWrapperFromMap(client.toMap()).toLeshanClient()
    }

    @Override
    Collection<Client> allClients() {
        def bsonClients = clientsCollection().find()
        List<DBObject> clients = []
        while (bsonClients.hasNext()) {
            clients += bsonClients.next()
        }
        clients.collect { clientWrapperFromMap(it.toMap()).toLeshanClient() }
    }

    @Override
    public void addListener(ClientRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ClientRegistryListener listener) {
        listeners.remove(listener);
    }

    @Override
    boolean registerClient(Client client) {
        def existingClient = clientsCollection().findOne(new BasicDBObject([endpoint: client.endpoint]))
        if (existingClient != null) {
            clientsCollection().remove(new BasicDBObject([_id: existingClient._id]))
        }

        def previousClient = get(client.endpoint)
        def clientMap = objectMapper.convertValue(client, Map.class)
        clientsCollection().insert(new BasicDBObject(clientMap))

        if (previousClient != null) {
            listeners.each { it.unregistered(previousClient) }
        }
        listeners.each { it.registered(previousClient) }

        true
    }

    @Override
    Client updateClient(ClientUpdate update) {
        def client = clientsCollection().findOne(new BasicDBObject([registrationId: update.registrationId]))
        if (client == null) {
            return null
        } else {
            def clientUpdated = update.updateClient(clientWrapperFromMap(client.toMap()).toLeshanClient());
            def clientToUpdate = objectMapper.convertValue(clientUpdated, Map.class)
            clientToUpdate._id = client._id
            clientsCollection().save(new BasicDBObject(clientToUpdate))

            // notify listener
            for (ClientRegistryListener l : listeners) {
                l.updated(clientUpdated);
            }
            return clientUpdated;
        }
    }

    @Override
    Client deregisterClient(String registrationId) {
        def toBeUnregistered = clientsCollection().findOne(new BasicDBObject([registrationId: registrationId]))
        if (toBeUnregistered == null) {
            return null;
        } else {
            clientsCollection().remove(new BasicDBObject([endpoint: toBeUnregistered.endpoint]))
            Client unregistered = clientWrapperFromMap(toBeUnregistered.toMap()).toLeshanClient()
            for (ClientRegistryListener l : listeners) {
                l.unregistered(unregistered);
            }
            return unregistered;
        }
    }

    // Helpers

    static ObjectMapper defaultObjectMapper() {
        new ObjectMapper()
    }

    protected DBCollection clientsCollection() {
        mongo.getDB('leshan').getCollection('Client')
    }

}
