package edu.bupt.ta.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {
    List<T> findAll();

    Optional<T> findById(ID id);

    void save(T entity);

    void saveAll(List<T> entities);

    void deleteById(ID id);
}
