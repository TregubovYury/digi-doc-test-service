package com.ee.digi_doc.storage.local;

import com.ee.digi_doc.common.properties.Digidoc4jProperties;
import com.ee.digi_doc.common.properties.StorageProperties;
import com.ee.digi_doc.exception.FileNotDeletedException;
import com.ee.digi_doc.exception.FileNotReadException;
import com.ee.digi_doc.exception.FileNotWrittenException;
import com.ee.digi_doc.persistance.model.Container;
import com.ee.digi_doc.storage.StorageContainerRepository;
import lombok.extern.slf4j.Slf4j;
import org.digidoc4j.Configuration;
import org.digidoc4j.impl.asic.asice.bdoc.BDocContainerBuilder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Repository
public class LocalStorageContainerRepository implements StorageContainerRepository {

    private final Path containerStorageLocation;
    private final Configuration configuration;

    public LocalStorageContainerRepository(StorageProperties storageProperties, Digidoc4jProperties properties) {
        this.containerStorageLocation = Paths.get(storageProperties.getContainer().getPath()).toAbsolutePath().normalize();
        this.configuration = new Configuration(properties.getMode());
    }

    @PostConstruct
    public void init() {
        if (Files.notExists(containerStorageLocation)) {
            try {
                Files.createDirectories(containerStorageLocation);
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory for container", e);
            }
        }
    }

    @Override
    public void storeContainer(Container container) {
        Path containerPath = containerStorageLocation.resolve(container.getName()).normalize();
        try (OutputStream outputStream = new FileOutputStream(containerPath.toFile())) {
            container.getBdDocContainer().save(outputStream);
        } catch (IOException e) {
            log.error("Error obtained during container write", e);
            throw new FileNotWrittenException(container.getName());
        }
    }

    @Override
    public org.digidoc4j.Container getContainer(String containerName) {
        Path containerPath = containerStorageLocation.resolve(containerName).normalize();
        try (InputStream inputStream = new FileInputStream(containerPath.toFile())) {
            return BDocContainerBuilder.aContainer().fromStream(inputStream).withConfiguration(configuration).build();
        } catch (IOException e) {
            log.error("Error obtained during container read", e);
            throw new FileNotReadException(containerName);
        }
    }

    @Override
    public void deleteContainer(String containerName) {
        try {
            Path containerPath = containerStorageLocation.resolve(containerName).normalize();
            Files.delete(containerPath);
        } catch (IOException e) {
            log.error("Error obtained during container delete: " + containerName, e);
            throw new FileNotDeletedException(containerName);
        }
    }
}
