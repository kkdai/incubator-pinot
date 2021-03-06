package org.apache.pinot.thirdeye.detection.yaml;

import org.apache.pinot.thirdeye.datalayer.bao.DAOTestBase;
import org.apache.pinot.thirdeye.datalayer.bao.DetectionConfigManager;
import org.apache.pinot.thirdeye.datalayer.dto.ApplicationDTO;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionAlertConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionConfigDTO;
import org.apache.pinot.thirdeye.datasource.DAORegistry;
import org.apache.pinot.thirdeye.detection.annotation.registry.DetectionAlertRegistry;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class YamlResourceTest {

  private DAOTestBase testDAOProvider;
  private YamlResource yamlResource;
  private DAORegistry daoRegistry;
  private static long alertId1;
  private static long alertId2;

  @BeforeMethod
  public void beforeClass() {
    testDAOProvider = DAOTestBase.getInstance();
    this.yamlResource = new YamlResource();
    this.daoRegistry = DAORegistry.getInstance();
    DetectionConfigManager detectionDAO = this.daoRegistry.getDetectionConfigManager();
    DetectionConfigDTO config1 = new DetectionConfigDTO();
    config1.setName("test_detection_1");
    alertId1 = detectionDAO.save(config1);
    DetectionConfigDTO config2 = new DetectionConfigDTO();
    config2.setName("test_detection_2");
    alertId2 = detectionDAO.save(config2);

    DetectionAlertRegistry.getInstance().registerAlertScheme("EMAIL", "EmailClass");
    DetectionAlertRegistry.getInstance().registerAlertScheme("IRIS", "IrisClass");
    DetectionAlertRegistry.getInstance().registerAlertSuppressor("TIME_WINDOW", "TimeWindowClass");
    DetectionAlertRegistry.getInstance().registerAlertFilter("DIMENSIONAL_ALERTER_PIPELINE", "DimClass");
  }

  @AfterMethod(alwaysRun = true)
  void afterClass() {
    testDAOProvider.cleanup();
  }

  @Test
  public void testCreateDetectionAlertConfig() throws IOException {
    String blankYaml = "";
    try {
      this.yamlResource.createSubscriptionGroup(blankYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "The Yaml Payload in the request is empty.");
    }

    String inValidYaml = "application:test:application";
    try {
      this.yamlResource.createSubscriptionGroup(inValidYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Could not parse as map: application:test:application");
    }

    String noSubscriptGroupYaml = "application: test_application";
    try {
      this.yamlResource.createSubscriptionGroup(noSubscriptGroupYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Subscription group name field cannot be left empty.");
    }

    String appFieldMissingYaml = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-1.yaml"));
    try {
      this.yamlResource.createSubscriptionGroup(appFieldMissingYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Application field cannot be left empty");
    }

    String appMissingYaml = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-2.yaml"));
    try {
      this.yamlResource.createSubscriptionGroup(appMissingYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Application name doesn't exist in our registry."
          + " Please use an existing application name. You may search for registered applications from the ThirdEye"
          + " dashboard or reach out to ask_thirdeye if you wish to setup a new application.");
    }

    DetectionAlertConfigDTO oldAlertDTO = new DetectionAlertConfigDTO();
    oldAlertDTO.setName("test_group");
    daoRegistry.getDetectionAlertConfigManager().save(oldAlertDTO);

    ApplicationDTO request = new ApplicationDTO();
    request.setApplication("test_application");
    request.setRecipients("abc@abc.in");
    daoRegistry.getApplicationDAO().save(request);

    String groupExists = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-3.yaml"));
    try {
      this.yamlResource.createSubscriptionGroup(groupExists);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Subscription group name is already taken. Please use a different name.");
    }

    String validYaml = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-4.yaml"));
    try {
      long id = this.yamlResource.createSubscriptionGroup(validYaml);
      DetectionConfigDTO detection = daoRegistry.getDetectionConfigManager().findById(id);
      Assert.assertNotNull(detection);
      Assert.assertEquals(detection.getName(), "Subscription Group Name");
    } catch (Exception e) {
      Assert.fail("Exception should not be thrown for valid yaml. Message = " + e);
    }
  }

  @Test
  public void testUpdateDetectionAlertConfig() throws IOException {
    ApplicationDTO request = new ApplicationDTO();
    request.setApplication("test_application");
    request.setRecipients("abc@abc.in");
    daoRegistry.getApplicationDAO().save(request);

    String validYaml = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-4.yaml"));
    long oldId = -1;
    try {
      oldId = this.yamlResource.createSubscriptionGroup(validYaml);
    } catch (Exception e) {
      Assert.fail("Exception should not be thrown for valid yaml. Message = " + e);
    }

    DetectionAlertConfigDTO alertDTO;

    try {
      this.yamlResource.updateSubscriptionGroup(-1, "");
      Assert.fail("Exception not thrown when the subscription group doesn't exist");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Cannot find subscription group -1");
    }

    String blankYaml = "";
    try {
      this.yamlResource.updateSubscriptionGroup(oldId, blankYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "The Yaml Payload in the request is empty.");
    }

    String inValidYaml = "application:test:application";
    try {
      this.yamlResource.updateSubscriptionGroup(oldId, inValidYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Could not parse as map: application:test:application");
    }

    String noSubscriptGroupYaml = "application: test_app";
    try {
      this.yamlResource.updateSubscriptionGroup(oldId, noSubscriptGroupYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Subscription group name field cannot be left empty.");
    }

    String appFieldMissingYaml = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-1.yaml"));
    try {
      this.yamlResource.updateSubscriptionGroup(oldId, appFieldMissingYaml);
      Assert.fail("Exception not thrown on empty yaml");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "Application field cannot be left empty");
    }

    String validYaml2 = IOUtils.toString(this.getClass().getResourceAsStream("alertconfig/alert-config-5.yaml"));
    try {
      this.yamlResource.updateSubscriptionGroup(oldId, validYaml2);
      alertDTO = daoRegistry.getDetectionAlertConfigManager().findById(oldId);
      Assert.assertNotNull(alertDTO);
      Assert.assertEquals(alertDTO.getName(), "Subscription Group Name");
      Assert.assertEquals(alertDTO.getApplication(), "test_application");

      // Verify if the vector clock is updated with the updated detection
      Assert.assertEquals(alertDTO.getVectorClocks().keySet().size(), 1);
      Assert.assertEquals(alertDTO.getVectorClocks().keySet().toArray()[0], alertId2);
    } catch (Exception e) {
      Assert.fail("Exception should not be thrown for valid yaml. Message = " + e);
    }
  }
}

