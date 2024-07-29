package com.researchspace.dcd.rspaceadapter;


import com.researchspace.dcd.client.DigitalCommonsDataClient;
import com.researchspace.dcd.client.DigitalCommonsDataClientImpl;
import com.researchspace.dcd.model.DigitalCommonDataSubmission;
import com.researchspace.dcd.model.DigitalCommonsDataDataset;
import com.researchspace.dcd.model.DigitalCommonsDataFile;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.IRepository;
import com.researchspace.repository.spi.License;
import com.researchspace.repository.spi.LicenseConfigInfo;
import com.researchspace.repository.spi.LicenseDef;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryConfigurer;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.Subject;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.repository.spi.properties.RepoProperty;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.client.RestClientException;


@Getter
@Setter
@Slf4j
public class DigitalCommonsDataRepository implements IRepository, RepositoryConfigurer {

  private DigitalCommonsDataClient digitalCommonsDataClient;

  @Override
  public void configure(RepositoryConfig repositoryConfig) {
    this.digitalCommonsDataClient = new DigitalCommonsDataClientImpl(
        repositoryConfig.getServerURL(),
        repositoryConfig.getIdentifier());
  }

  @Override
  public RepositoryOperationResult submitDeposit(IDepositor iDepositor, File file,
      SubmissionMetadata submissionMetadata, RepositoryConfig repositoryConfig) {
    DigitalCommonDataSubmission datasetReequest = createDatasetRequest(submissionMetadata);

    try {

      DigitalCommonsDataDataset deposition = digitalCommonsDataClient.createDataset(
          datasetReequest);
      DigitalCommonsDataFile depositedFile = digitalCommonsDataClient.depositFile(deposition,
          file.getName(), file);
      return new RepositoryOperationResult(true,
          "Export uploaded to DigitalCommonsData successfully.",
          new URL(repositoryConfig.getServerURL() + "/drafts/" + deposition.getId()));

    } catch (RestClientException | IOException e) {
      log.error("Exception occurred while submitting to DigitalCommonsData", e);
      return new RepositoryOperationResult(false,
          e.getClass().getSimpleName() + "Exception occurred while submitting to DigitalCommonsData", null);
    }
  }

  private DigitalCommonDataSubmission createDatasetRequest(SubmissionMetadata submissionMetadata) {
    return DigitalCommonDataSubmission.builder()
        .title(submissionMetadata.getTitle())
        .description(submissionMetadata.getDescription())
        .uploadType("Exported from RSpace ELN")
        .build();
  }

  @Override
  public RepositoryOperationResult testConnection() {
    if (digitalCommonsDataClient.testConnection()) {
      return new RepositoryOperationResult(true, "Test Digital Commons Data connection succeeded",
          null);
    }
    return new RepositoryOperationResult(false, "Test Digital Commons Data connection failed",
        null);
  }

  @Override
  public RepositoryConfigurer getConfigurer() {
    return this;
  }

  @Override
  public List<Subject> getSubjects() {
    return Collections.emptyList();
  }

  @SneakyThrows
  @Override
  public LicenseConfigInfo getLicenseConfigInfo() {
    License license = new License();
    license.setLicenseDefinition(new LicenseDef(
        new URL("https://creativecommons.org/publicdomain/zero/1.0/"), "CC-0"));
    return new LicenseConfigInfo(true, false,
        Collections.singletonList(license));
  }

  @Override
  public Map<String, RepoProperty> getOtherProperties() {
    return new HashMap<>();
  }
}

