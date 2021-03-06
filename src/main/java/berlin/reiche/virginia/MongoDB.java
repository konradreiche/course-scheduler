package berlin.reiche.virginia;

import java.net.UnknownHostException;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

/**
 * A static provider of the data store interface to MongoDB.
 * 
 * @author Konrad Reiche
 * 
 */
public class MongoDB {

    private static final String DATABASE_NAME = "course-scheduler";

    private static Mongo mongo;
    private static Morphia morphia;
    private static Datastore datastore;

    /**
     * Creates a Mongo instance on localhost with port 27017 with a database.
     */
    static {
        try {
            mongo = new Mongo();
            morphia = new Morphia();
            datastore = morphia.createDatastore(mongo, DATABASE_NAME);
            datastore.ensureIndexes();
            datastore.ensureCaps();
        } catch (UnknownHostException e) {
            System.err.println("The host could not be determined.");
            e.printStackTrace();
        } catch (MongoException e) {
            System.err.println("Something went wrong in Mongo during"
                    + " construction.");
            e.printStackTrace();
        }
    }

    public static Datastore getDatastore() {
        return datastore;
    }

    /**
     * By performing a dummy request the current connection is checked. A raised
     * exceptions means there is no connection, otherwise there is a connection.
     * 
     * @return whether there is a working connection to the MongoDB server and
     *         the database as defined in the {@link Datastore}.
     */
    public static boolean isConnected() {

        try {
            datastore.getDB().getCollectionNames();
        } catch (MongoException e) {
            return false;
        }

        return true;
    }

    /**
     * Gets an entity of the database identified by its unique identifier.
     * 
     * @param cls
     *            the class type of the entity to retrieve.
     * @param id
     *            the unique identifier of the entity
     * @return the model object representing the entity.
     */
    public static <T, V> T get(Class<T> cls, V id) {
        return datastore.get(cls, id);
    }

    /**
     * Returns only one object of this entity type. It is expected, that there
     * is only one instance of this entity present in the database. Otherwise an
     * exception is raised.
     * 
     * @param cls
     *            the class type of the entity to retrieve.
     * @return the object of the entity.
     */
    public static <T> T get(Class<T> cls) {

        List<T> collection = getAll(cls);
        if (collection.size() > 1) {
            throw new IllegalStateException("There is more than one object in"
                    + " the database of type" + cls.getClass() + ".");
        }

        return (collection.size() == 1) ? collection.get(0) : null;
    }

    /**
     * Gets a list of all entities of a certain type.
     * 
     * @param cls
     *            the class type of the entity to retrieve.
     * @return the list of model objects representing the entities.
     */
    public static <T, V> List<T> getAll(Class<T> cls) {
        return datastore.find(cls).asList();
    }

    /**
     * Stores a model object as entity in the database.
     * 
     * @param entity
     *            the model object representing the entity.
     * @return the {@link String} representation of the entities key.
     */
    public static <T> Object store(T entity) {
        return datastore.save(entity).toString();
    }

    /**
     * Deletes a certain entity identified by its unique identifier.
     * 
     * @param cls
     *            the class type of the entity to retrieve.
     * @param id
     *            the unique identifier of the entity
     */
    public static <T, V> void delete(Class<T> cls, V id) {
        datastore.delete(cls, id);
    }

    /**
     * Deletes a certain entity from which only one should exist.
     * 
     * @param cls
     *            the class type of the entity to delete.
     */
    public static <T> void delete(Class<T> cls) {

        List<T> collection = getAll(cls);
        if (collection.size() > 1) {
            throw new IllegalStateException("There is more than one object in"
                    + " the database of type" + cls.getClass() + ".");
        }
        deleteAll(cls);
    }
    
    /**
     * Deletes a given entity by using its identifier.
     * 
     * @param entity the entity to be deleted.
     */
    public static <T> void delete(T entity) {
        datastore.delete(entity);
    }
    
    /**
     * Deletes the entities based on the given query.
     * 
     * @param query the query to get the entities.
     */
    public static <T> void delete(Query<T> query) {
        datastore.delete(query);
    }

    /**
     * Deletes a certain type of entities.
     * 
     * @param cls
     *            the class type of the entities to delete.
     */
    public static <T> void deleteAll(Class<T> cls) {
        datastore.delete(datastore.createQuery(cls));
    }

    /**
     * @param cls
     *            the class specifying the would-be queried entities.
     * @return a new query bound to the given class.
     */
    public static <T> Query<T> createQuery(Class<T> cls) {
        return datastore.createQuery(cls);
    }

}
