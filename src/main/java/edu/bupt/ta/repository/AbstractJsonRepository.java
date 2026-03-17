package edu.bupt.ta.repository;

import com.fasterxml.jackson.databind.JavaType;
import edu.bupt.ta.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractJsonRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {

    private final Path filePath;
    private final Class<T> elementType;

    protected AbstractJsonRepository(Path filePath, Class<T> elementType) {
        this.filePath = filePath;
        this.elementType = elementType;
        ensureFileExists();
    }

    @Override
    public List<T> findAll() {
        return loadAllInternal();
    }

    @Override
    public Optional<T> findById(ID id) {
        return loadAllInternal().stream().filter(entity -> Objects.equals(entity.getId(), id)).findFirst();
    }

    @Override
    public void save(T entity) {
        List<T> all = loadAllInternal();
        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            if (Objects.equals(all.get(i).getId(), entity.getId())) {
                all.set(i, entity);
                updated = true;
                break;
            }
        }
        if (!updated) {
            all.add(entity);
        }
        writeAllInternal(all);
    }

    @Override
    public void saveAll(List<T> entities) {
        writeAllInternal(entities);
    }

    @Override
    public void deleteById(ID id) {
        List<T> all = loadAllInternal();
        all.removeIf(entity -> Objects.equals(entity.getId(), id));
        writeAllInternal(all);
    }

    protected void ensureFileExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize repository file: " + filePath, e);
        }
    }

    protected List<T> loadAllInternal() {
        try {
            if (!Files.exists(filePath)) {
                return new ArrayList<>();
            }
            String content = Files.readString(filePath).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }
            JavaType listType = JsonUtils.mapper().getTypeFactory().constructCollectionType(List.class, elementType);
            return JsonUtils.mapper().readValue(content, listType);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + filePath, e);
        }
    }

    protected void writeAllInternal(List<T> entities) {
        try {
            Files.createDirectories(filePath.getParent());
            JsonUtils.mapper().writeValue(filePath.toFile(), entities);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write file: " + filePath, e);
        }
    }
}
