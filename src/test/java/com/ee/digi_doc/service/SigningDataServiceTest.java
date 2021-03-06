package com.ee.digi_doc.service;

import com.ee.digi_doc.common.properties.StorageProperties;
import com.ee.digi_doc.persistance.dao.JpaFileRepository;
import com.ee.digi_doc.persistance.dao.JpaSigningDataRepository;
import com.ee.digi_doc.persistance.model.File;
import com.ee.digi_doc.persistance.model.SigningData;
import com.ee.digi_doc.util.FileGenerator;
import com.ee.digi_doc.util.TestSigningData;
import com.ee.digi_doc.web.request.CreateSigningDataRequest;
import org.digidoc4j.Container;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
class SigningDataServiceTest {

    @Value("${test.file.number:10}")
    private int fileNumber;

    @Autowired
    private SigningDataService signingDataService;

    @Autowired
    private JpaSigningDataRepository jpaSigningDataRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private JpaFileRepository jpaFileRepository;

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void whenCreateDataToSign_thenOk() {
        Path signingDataDirectoryPath = Paths.get(storageProperties.getSigningData().getPath()).toAbsolutePath().normalize();
        Path filesDirectoryPath = Paths.get(storageProperties.getFile().getPath()).toAbsolutePath().normalize();

        List<File> filesToSign = createFiles();
        List<Long> fileIds = filesToSign.stream().map(File::getId).collect(Collectors.toList());

        CreateSigningDataRequest request = new CreateSigningDataRequest();
        request.setFileIds(fileIds);
        request.setCertificateInHex(TestSigningData.getRSASigningCertificateInHex());

        SigningData signingData = signingDataService.create(request);
        assertNotNull(signingData);
        assertNotNull(signingData.getId());
        assertNotNull(signingData.getContainerName());
        assertNotNull(signingData.getContainer());
        assertNotNull(signingData.getDataToSignName());
        assertNotNull(signingData.getDataToSign());
        assertNotNull(signingData.getCreatedOn());
        assertNotNull(signingData.getSignatureInHex());

        assertTrue(signingData.getContainerName().endsWith("." + Container.DocumentType.BDOC.name().toLowerCase()));
        assertTrue(signingData.getDataToSignName().endsWith(".bin"));

        assertTrue(jpaSigningDataRepository.findById(signingData.getId()).isPresent());

        assertTrue(Files.exists(signingDataDirectoryPath.resolve(signingData.getContainerName())));
        assertTrue(Files.exists(signingDataDirectoryPath.resolve(signingData.getDataToSignName())));

        assertTrue(jpaFileRepository.findAllById(fileIds).isEmpty());

        for (File file : filesToSign) {
            assertTrue(Files.notExists(filesDirectoryPath.resolve(file.getName())));
        }
    }

    @Test
    void whenGetDataToSign_thenOk() {
        CreateSigningDataRequest request = new CreateSigningDataRequest();
        request.setFileIds(createFiles().stream().map(File::getId).collect(Collectors.toList()));
        request.setCertificateInHex(TestSigningData.getRSASigningCertificateInHex());

        Long signingDataId = signingDataService.create(request).getId();

        SigningData signingData = signingDataService.getSigningData(signingDataId).orElse(null);

        assertNotNull(signingData);
        assertNotNull(signingData.getId());
        assertNotNull(signingData.getContainerName());
        assertNotNull(signingData.getContainer());
        assertNotNull(signingData.getDataToSignName());
        assertNotNull(signingData.getDataToSign());
        assertNotNull(signingData.getCreatedOn());
        assertNotNull(signingData.getSignatureInHex());

        assertTrue(signingData.getContainerName().endsWith("." + Container.DocumentType.BDOC.name().toLowerCase()));
        assertTrue(signingData.getDataToSignName().endsWith(".bin"));
    }

    @Test
    void whenDeleteDataToSign_thenOK() {
        Path signingDataDirectoryPath = Paths.get(storageProperties.getSigningData().getPath()).toAbsolutePath().normalize();

        List<Long> fileIds = createFiles().stream().map(File::getId).collect(Collectors.toList());

        CreateSigningDataRequest request = new CreateSigningDataRequest();
        request.setFileIds(fileIds);
        request.setCertificateInHex(TestSigningData.getRSASigningCertificateInHex());

        SigningData signingData = signingDataService.create(request);

        signingDataService.delete(signingData);

        assertTrue(jpaSigningDataRepository.findById(signingData.getId()).isEmpty());
        assertTrue(Files.notExists(signingDataDirectoryPath.resolve(signingData.getContainerName())));
        assertTrue(Files.notExists(signingDataDirectoryPath.resolve(signingData.getDataToSignName())));
    }

    private List<File> createFiles() {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileNumber; i++) {
            files.add(fileService.create(FileGenerator.randomMultipartJpeg()));
        }
        return files;
    }


}