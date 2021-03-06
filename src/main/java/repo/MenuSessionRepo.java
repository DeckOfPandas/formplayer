package repo;

import exceptions.MenuNotFoundException;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by willpride on 8/1/16.
 */
public interface MenuSessionRepo extends CrudRepository<SerializableMenuSession, String> {
    SerializableMenuSession findOneWrapped(String id) throws MenuNotFoundException;
}
