package com.researchspace.dcd.rspaceadapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchspace.dcd.client.DigitalCommonsDataClient;
import com.researchspace.dcd.model.DigitalCommonDataSubmission;
import com.researchspace.dcd.model.DigitalCommonsDataDataset;
import com.researchspace.dcd.model.DigitalCommonsDataFile;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.SubmissionMetadata;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.HttpClientErrorException;


class DigitalCommonsDataRepositoryTest {

  @Mock
  private DigitalCommonsDataClient digitalCommonsDataClient;
  @InjectMocks
  private DigitalCommonsDataRepository repoAdapter;
  @Mock
  private IDepositor author;
  private ObjectMapper mapper = new ObjectMapper();
  private RepositoryConfig repositoryConfig;

  @BeforeEach
  public void setUp() throws MalformedURLException {
    MockitoAnnotations.openMocks(this);
    this.repositoryConfig = new RepositoryConfig(new URL("https://data.mendeley.com"),
        "API_ACCESS_TOKEN", null, "app.digital_commons_data");
    this.repoAdapter = new DigitalCommonsDataRepository();
    this.repoAdapter.configure(repositoryConfig);
    this.repoAdapter.setDigitalCommonsDataClient(digitalCommonsDataClient);
  }

  @Test
  void testTestConnectionSucceed() {
    when(digitalCommonsDataClient.testConnection()).thenReturn(true);
    assertTrue(repoAdapter.testConnection().isSucceeded());
  }

  @Test
  void testTestConnectionFails() {
    when(digitalCommonsDataClient.testConnection()).thenReturn(false);
    assertFalse(repoAdapter.testConnection().isSucceeded());
  }

  @Test
  void testGetSubjectIsEmpty() {
    assertTrue(repoAdapter.getSubjects().isEmpty());
  }

  @Test
  void testGetLicenseConfigIsRequired() {
    assertTrue(repoAdapter.getLicenseConfigInfo().isLicenseRequired());
  }

  @Test
  void testGetOtherPropertiesIsEmpty() {
    assertTrue(repoAdapter.getOtherProperties().isEmpty());
  }

  @Test
  void testSubmitDepositSucceed() throws IOException {
    when(digitalCommonsDataClient.createDataset(any(DigitalCommonDataSubmission.class))).thenReturn(
        getDigitalCommonsDataDatasetResponse());
    when(digitalCommonsDataClient.depositFile(any(DigitalCommonsDataDataset.class),
            any(String.class), any(File.class))).thenReturn(
        getDigitalCommonsDataFile());
    SubmissionMetadata metadata = getTestSubmissionMetaData();
    File file = new File("src/test/resources/files/test.txt");
    RepositoryOperationResult dataset = repoAdapter.submitDeposit(null, file,
        metadata, repositoryConfig);
    assertTrue(dataset.isSucceeded());
  }

  @Test
  void testSubmitDepositFailCreatingDataset() throws IOException {
    when(digitalCommonsDataClient.createDataset(any(DigitalCommonDataSubmission.class)))
        .thenThrow(HttpClientErrorException.class);
    when(digitalCommonsDataClient.depositFile(any(DigitalCommonsDataDataset.class),
            any(String.class), any(File.class))).thenReturn(
        getDigitalCommonsDataFile());
    SubmissionMetadata metadata = getTestSubmissionMetaData();
    File file = new File("src/test/resources/files/test.txt");
    RepositoryOperationResult dataset = repoAdapter.submitDeposit(null, file,
        metadata, repositoryConfig);
    assertFalse(dataset.isSucceeded());
  }

@Test
  void testSubmitDepositFailDepositingFile() throws IOException {
    when(digitalCommonsDataClient.createDataset(any(DigitalCommonDataSubmission.class))).thenReturn(
        getDigitalCommonsDataDatasetResponse());
    when(digitalCommonsDataClient.depositFile(any(DigitalCommonsDataDataset.class),
            any(String.class), any(File.class))).thenThrow(HttpClientErrorException.class);
    SubmissionMetadata metadata = getTestSubmissionMetaData();
    File file = new File("src/test/resources/files/test.txt");
    RepositoryOperationResult dataset = repoAdapter.submitDeposit(null, file,
        metadata, repositoryConfig);
    assertFalse(dataset.isSucceeded());
  }



  private DigitalCommonsDataFile getDigitalCommonsDataFile() throws IOException {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(
        IOUtils.resourceToString("/json/fileDepositResponse.json", StandardCharsets.UTF_8),
        DigitalCommonsDataFile.class);
  }

  private DigitalCommonsDataDataset getDigitalCommonsDataDatasetResponse() throws IOException {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(
        IOUtils.resourceToString("/json/depositionCreationResponse.json", StandardCharsets.UTF_8),
        DigitalCommonsDataDataset.class);
  }

  private SubmissionMetadata getTestSubmissionMetaData() throws IOException {
    SubmissionMetadata md = new SubmissionMetadata();
    when(author.getEmail()).thenReturn("email@somewhere.com");
    when(author.getUniqueName()).thenReturn("anyone");

    md.setAuthors(List.of(author));
    md.setContacts(List.of(author));
    md.setDescription("desc");
    md.setPublish(false);
    md.setSubjects(List.of("Other natural sciences"));
    md.setLicense(Optional.of(new URL("https://creativecommons.org/publicdomain/zero/1.0/")));
    md.setTitle("title");
    md.setDmpDoi(Optional.of("10.5072/digitalCommonsData.1059996"));

    return md;
  }


}
