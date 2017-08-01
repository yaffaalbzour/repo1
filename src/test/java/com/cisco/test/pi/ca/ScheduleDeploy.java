package com.cisco.test.pi.ca;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.EnumUtility;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.rest.dto.ConfigDeployJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveRollbackRestIntf;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;
import com.cisco.ifm.preference.PreferenceStore;
import com.cisco.srtg.test.aems.helper.utils.Inventory_util;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;

public class ScheduleDeploy extends BaseTest {

    @Autowired
    ConfigArchiveHelper caHelper;

    @Autowired
    IJobSchedulerRestService jobSchedulerServiceImpl;

    @Autowired
    public Inventory_util inventoryUtils;

    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Autowired
    private ConfigArchiveManager configArchiveManager;

    @Autowired
    private ConfigArchiveViewRestIntf configArchiveViewService;

    @Autowired
    private ConfigArchiveRollbackRestIntf configArchiveRollbackRestIntf;

    @Override
    public void beforeAllTests() {

    }

    @Override
    public void clean() {

    }
    private static final long timeOut = 30*60*1000;
    @AfterClass
    public void setTimeout() throws BackingStoreException {

        PreferenceStore pStore = PreferenceStore.getInstance();
        pStore.saveSystemPrefence(toSlash("default.adminPreferences"), "timeout", "360000");

    }

    private String toSlash(String node) {
        return node.replace('.', '/');
    }

    /**
     *
     * @param ds
     * @throws Exception
     */
    @Test(timeOut = timeOut, groups = {"addDevice_GROUP"}, description = "Add Device to PI", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, enabled = true)
    public void addDevice(Dataset ds) throws Exception {

        // Set Device Name and device IP
        this.setDeviceName(ds.getAsString("deviceName"));
        this.setDeviceIP(this.getTestEnvironment().getNetworkDeviceByName(this.getDeviceName()).getHost().getHostAddress());

        super.beforeAllTests();
        this.setDeviceIdentifier(getTestEnvironment().getDeviceIdentifier(this.device.getName()));
        
        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }
        // delete any existing ACLs from device
        caHelper.deleteACLsOnDevice(this.device, ds);

        PreferenceStore pStore = PreferenceStore.getInstance();
        pStore.saveSystemPrefence(toSlash("default.adminPreferences"), "timeout", "3600000");
    }

    @Test(timeOut = timeOut, groups = {"scheduleDeploy_GROUP"}, enabled = true, dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, description = "Schedule Deploy", dependsOnMethods = {"addDevice"})
    public void scheduleDeploy(Dataset ds) throws Exception, URISyntaxException,
            IOException {

        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }
        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        // create ACL on device before deploying config file
        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";
        String[] commands = null;

        if (ds.getAsString("deviceType").equals("ASR9K")) {

            createACLCommand = "ipv4 access-list acl" + accListNumber + " permit any";
            showACLCommand = "show ipv4 access-list | i " + accListNumber;
            deleteACLCommand = "no ipv4 access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand = "ip access-list acl" + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit tcp any any", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            createACLCommand = "acl create acl_" + accListNumber;
            showACLCommand = "show acl summary";
            deleteACLCommand = "acl delete acl_" + accListNumber;
            commands = new String[]{"config", createACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            createACLCommand = "name app_" + accListNumber;
            showACLCommand = "show application group";
            deleteACLCommand = "no application group app_" + accListNumber;
            commands = new String[]{"application group", createACLCommand, "add 50331742", "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand = "access-list  acl_" + accListNumber + " standard permit any4";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no " + createACLCommand;
            commands = new String[]{"config t", createACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            createACLCommand = "ip access-list acl_" + accListNumber + " permit any";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no ip access-list acl_" + accListNumber;
            commands = new String[]{"config t", createACLCommand, "exit"};
        } else {
            accListNumber = accListNumber % 99 + 1;
            createACLCommand = "ip access-list standard " + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        String filePath = ds.getAsString("filePath");
        String fileName = ds.getAsString("configFile");

        String fileURL = filePath + fileName;
        InputStream fileInputStream = new URL(fileURL).openStream();
        String x = configArchiveViewService.copyFile(fileInputStream, fileName).toString();
        Document xml = getXMLDocument(x);
        TestLogger.info(xml.getElementsByTagName("textarea").item(0).getTextContent());
        String configFileName = xml.getElementsByTagName("textarea").item(0).getTextContent();
        ConfigDeployJobDto configDeployJobDTO = caHelper.prepareScheduleDeploy(ds, device_MEI, configFileName);
        caHelper.waitWhileConfigDeployJobCompleted(configDeployJobDTO, ds);

        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        if (!ds.getAsString("deviceType").equals("WLC")) {
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);
        }
    }  // end of test@Test

    @Test(timeOut = timeOut, groups = {"scheduleDeployWithOverwriteStartupConfig_GROUP"}, enabled = true, dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, description = "Schedule deploy with overwrite startup config", dependsOnMethods = {"addDevice"})
    public void scheduleDeployWithOverwriteStartupConfig(Dataset ds) throws Exception, URISyntaxException,
            IOException {

        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }
        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        // create ACL on device before deploying config file
        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";
        String[] commands = null;

        if (ds.getAsString("deviceType").equals("ASR9K")) {

            createACLCommand = "ipv4 access-list acl" + accListNumber + " permit any";
            showACLCommand = "show ipv4 access-list | i " + accListNumber;
            deleteACLCommand = "no ipv4 access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand = "ip access-list acl" + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit tcp any any", "exit", "exit", "copy r st"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand = "access-list  acl_" + accListNumber + " standard permit any4";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no " + createACLCommand;
            commands = new String[]{"config t", createACLCommand, "exit", "wr"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            createACLCommand = "ip access-list acl_" + accListNumber + " permit any";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no ip access-list acl_" + accListNumber;
            commands = new String[]{"config t", createACLCommand, "exit"};
        } else {
            accListNumber = accListNumber % 99 + 1;
            createACLCommand = "ip access-list standard " + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        String showCommand = "show startup-config | i _" + accListNumber + "_";
        if(ds.getAsString("deviceType").toLowerCase().contains("nexus")){
            showCommand = "show startup-config | i " + accListNumber;
        }
        output = caHelper.getOutputFromDevice(this.device,
                showCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        String filePath = ds.getAsString("filePath");
        String fileName = ds.getAsString("configFile");

        String fileURL = filePath + fileName;
        InputStream fileInputStream = new URL(fileURL).openStream();
        String x = configArchiveViewService.copyFile(fileInputStream, fileName).toString();
        Document xml = getXMLDocument(x);
        TestLogger.info(xml.getElementsByTagName("textarea").item(0).getTextContent());
        String configFileName = xml.getElementsByTagName("textarea").item(0).getTextContent();
        ConfigDeployJobDto configDeployJobDTO = caHelper.prepareScheduleDeploy(ds, device_MEI, configFileName);
        caHelper.waitWhileConfigDeployJobCompleted(configDeployJobDTO, ds);

        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        showCommand = "show startup-config | i access-list " + accListNumber + "_";
        output = caHelper.getOutputFromDevice(this.device,
                showCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertFalse(output.contains("" + accListNumber),
                "Overwrite failed: There is acl with name contains " + accListNumber + " which is not expecte");
    }  // end of test

    public Document getXMLDocument(String xml) throws SAXException, Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

}
