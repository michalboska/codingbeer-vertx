package ch.erni.beer.vertx.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bol on 8. 12. 2014.
 */
public abstract class Entity {
    protected String name;
    protected String guid;

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }


    public Entity(String name, String guid) {
        this.name = name;
        this.guid = guid;
    }

    //GUID is an unique ID for an internal dto, program logic assumes there cannot be
    //different objects with the same guid, therefore guid field is sufficient for equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (o == null || !this.getClass().isInstance(o)) {
            return false;
        }
        Entity oo = (Entity) o;
        return guid.equals(oo.guid);
    }

    @Override
    public int hashCode() {
        return guid.hashCode();
    }

    public String getName() {
        return name;
    }

    public String getGuid() {
        return guid;
    }
}
