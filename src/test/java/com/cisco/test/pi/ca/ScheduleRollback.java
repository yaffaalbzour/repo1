package com.cisco.test.pi.ca;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.cisco.ifm.preference.PreferenceStore;

import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.EnumUtility;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveJobDto;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveRollbackJobDTO;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveRollbackRestIntf;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;
import com.cisco.srtg.test.aems.helper.utils.Inventory_util;
import java.util.Random;
import org.testng.annotations.AfterClass;
import java.util.prefs.BackingStoreException;
import org.testng.SkipException;

public class ScheduleRollback extends BaseTest {

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

    @AfterClass
    public void setTimeout() throws BackingStoreException {

        PreferenceStore pStore = PreferenceStore.getInstance();
        pStore.saveSystemPrefence(toSlash("default.adminPreferences"), "timeout", "360000");

    }
    private static final long timeOut = 30*60*1000;
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

    @Test(timeOut = timeOut, groups = {"scheduleRollbackWithMerge_GROUP"}, enabled = true, description = " Schedule Rollback with merge", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void scheduleRollbackWithMerge(Dataset ds) throws Exception {

        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }

        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();
        TestLogger.info("device_MEI:  " + device_MEI);
        TestLogger.info("device_name:  " + deviceNameInPrime);
        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        int accListNumber2 = Math.abs(random.nextInt());

        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";

        String createACLCommand2 = "";
        String showACLCommand2 = "";
        String deleteACLCommand2 = "";

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
            commands = new String[]{"application group", createACLCommand, "add 50331740", "exit"};
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
            showACLCommand = "show ip access-list | i Standard IP access list " + accListNumber + "_";
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        ConfigArchiveJobDto configArchiveJobDto = caHelper.scheduleArchive(
                device_MEI, groupLevel);
        String runJobID = caHelper.waitWhileJobCompleted(configArchiveJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobID, archives),
                "no archive has been created");

        String archiveVersionId = caHelper.getArchiveObjectVersion(runJobID, archives);
        TestLogger.info("archiveVersionId: " + archiveVersionId);

        if (ds.getAsString("deviceType").equals("ASR9K")) {

            createACLCommand2 = "ipv4 access-list acl" + accListNumber2 + " permit any";
            showACLCommand2 = "show ipv4 access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ipv4 access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand2 = "ip access-list acl" + accListNumber2;
            showACLCommand2 = "show ip access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ip access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit tcp any any", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            createACLCommand2 = "acl create acl_" + accListNumber2;
            showACLCommand2 = "show acl summary";
            deleteACLCommand2 = "acl delete acl_" + accListNumber2;
            commands = new String[]{"config", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            createACLCommand2 = "name app_" + accListNumber2;
            showACLCommand2 = "show application group";
            deleteACLCommand2 = "no application group app_" + accListNumber2;
            commands = new String[]{"application group", createACLCommand2, "add 50331742", "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand2 = "access-list  acl_" + accListNumber2 + " standard permit any4";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no " + createACLCommand2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            createACLCommand2 = "ip access-list acl_" + accListNumber2 + " permit any";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no ip access-list acl_" + accListNumber2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else {
            accListNumber2 = accListNumber2 % 99 + 1;
            createACLCommand2 = "ip access-list standard " + accListNumber2;
            showACLCommand2 = "show ip access-list | i Standard IP access list " + accListNumber2 + "_";
            deleteACLCommand2 = "no ip access-list standard " + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber2),
                "There is no acl with name contains " + accListNumber2);

        // Delete 1st ACL and assert that it is deleted from the device 
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            commands = new String[]{"", deleteACLCommand};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        }
        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
//          if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//             output = caHelper.getHandledOutput(output);
//          }
        output = caHelper.getHandledOutput(output);
        Assert.assertFalse(output.contains("" + accListNumber),
                "There is acl with name contains " + accListNumber + " which is not expected");

        // schedule Rollback
        ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO = caHelper.rollbackArchive(archiveVersionId, false, false);
        caHelper.waitWhileRollbackJobCompleted(configArchiveRollbackJobDTO, ds);

        String output2 = "";
        // Assert that 2 acls exist on the device
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        output2 = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output2 = caHelper.getHandledOutput(output2);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber + " which is not expected");
        if (!ds.getAsString("deviceType").equals("WLC")) {
        Assert.assertTrue(output2.contains("" + accListNumber2),
                "There is no acl with name contains " + accListNumber2 + " which is not expected");
        }
        // Delete 1st and 2nd ACLs and assert that they are deleted from the device 
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "exit", "copy r st"};
            caHelper.getOutputFromDevice(this.device, commands);
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "commit", "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
//            commands = new String[]{"config", deleteACLCommand2, "exit"};
//            caHelper.getOutputFromDevice(this.device, commands);
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            commands = new String[]{"", deleteACLCommand};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"", deleteACLCommand2};
            caHelper.getOutputFromDevice(this.device, commands);
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "exit", "wr"};
            caHelper.getOutputFromDevice(this.device, commands);
        }
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        output2 = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output2 = caHelper.getHandledOutput(output2);

//        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//        output = caHelper.getHandledOutput(output);
//        output2 = caHelper.getHandledOutput(output2);
//          }
        if (ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            Assert.assertFalse(output.contains("acl" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
            Assert.assertFalse(output2.contains("acl" + accListNumber2),
                    "There is acl with name contains " + accListNumber2 + " which is not expected");
        } else {
            Assert.assertFalse(output.contains("" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
            Assert.assertFalse(output2.contains("" + accListNumber2),
                    "There is acl with name contains " + accListNumber2 + " which is not expected");
        }

    }

    @Test(timeOut = timeOut, groups = {"scheduleRollbackWithOverwrite_GROUP"}, description = " Schedule Rollback with overwrite", enabled = true, dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void scheduleRollbackWithOverwrite(Dataset ds) throws Exception {

        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }

        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();
        TestLogger.info("device_MEI:  " + device_MEI);
        TestLogger.info("device_name:  " + deviceNameInPrime);
        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        int accListNumber2 = Math.abs(random.nextInt());

        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";

        String createACLCommand2 = "";
        String showACLCommand2 = "";
        String deleteACLCommand2 = "";
        String[] commands = null;

        // create 1st acl
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
            showACLCommand = "show ip access-list | i Standard IP access list " + accListNumber + "_";
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        ConfigArchiveJobDto configArchiveJobDto = caHelper.scheduleArchive(
                device_MEI, groupLevel);
        String runJobID = caHelper.waitWhileJobCompleted(configArchiveJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobID, archives),
                "no archive has been created");

        String archiveVersionId = caHelper.getArchiveObjectVersion(runJobID, archives);
        TestLogger.info("archiveVersionId: " + archiveVersionId);

        // create 2nd acl
        if (ds.getAsString("deviceType").equals("ASR9K")) {

            createACLCommand2 = "ipv4 access-list acl" + accListNumber2 + " permit any";
            showACLCommand2 = "show ipv4 access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ipv4 access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            createACLCommand2 = "acl create acl_" + accListNumber2;
            showACLCommand2 = "show acl summary";
            deleteACLCommand2 = "acl delete acl_" + accListNumber2;
            commands = new String[]{"config", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand2 = "ip access-list acl" + accListNumber2;
            showACLCommand2 = "show ip access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ip access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit tcp any any", "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand2 = "access-list  acl_" + accListNumber2 + " standard permit any4";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no " + createACLCommand2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            createACLCommand2 = "ip access-list acl_" + accListNumber2 + " permit any";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no ip access-list acl_" + accListNumber2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else {
            accListNumber2 = accListNumber2 % 99 + 1;
            createACLCommand2 = "ip access-list standard " + accListNumber2;
            showACLCommand2 = "show ip access-list | i Standard IP access list " + accListNumber2 + "_";
            deleteACLCommand2 = "no ip access-list standard " + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output = caHelper.getHandledOutput(output);

        Assert.assertTrue(output.contains("" + accListNumber2),
                "There is no acl with name contains " + accListNumber2);

        // Delete 1st ACL and assert that it is deleted from the device 
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        }
        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
//        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//        output = caHelper.getHandledOutput(output);
//          }
        output = caHelper.getHandledOutput(output);
        Assert.assertFalse(output.contains("" + accListNumber),
                "There is acl with name contains " + accListNumber + " which is not expected");

        // schedule Rollback
        ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO = caHelper.rollbackArchive(archiveVersionId, true, false);
        caHelper.waitWhileRollbackJobCompleted(configArchiveRollbackJobDTO, ds);

        String output2 = "";
        // Assert only 1st ACL exists on the device
        if (ds.getAsString("deviceType").equals("ASR9K")) {
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
            output2 = caHelper.getOutputFromDevice(this.device,
                    showACLCommand2);
            output = caHelper.getHandledOutput(output);
            output2 = caHelper.getHandledOutput(output2);
            Assert.assertTrue(output.contains("" + accListNumber),
                    "There is no acl with name contains " + accListNumber + " which is not expected");
            Assert.assertFalse(output2.contains("" + accListNumber2),
                    "There is acl with name contains " + accListNumber2 + " which is not expected");
        } else {
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
            output2 = caHelper.getOutputFromDevice(this.device,
                    showACLCommand2);
            output = caHelper.getHandledOutput(output);
            output2 = caHelper.getHandledOutput(output2);
            Assert.assertTrue(output.contains("" + accListNumber),
                    "There is no acl with name contains " + accListNumber + " which is not expected");
            if (!ds.getAsString("deviceType").equals("WLC")) {
            Assert.assertTrue(output2.contains("" + accListNumber2),
                    "There is no acl with name contains " + accListNumber2 + " which is not expected");
         }
        }
        // Delete 1st ACLs
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
            caHelper.getOutputFromDevice(this.device, commands);
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
            caHelper.getOutputFromDevice(this.device, commands);
            output = caHelper.getOutputFromDevice(this.device,
                    showACLCommand);
        }
//          if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//            output = caHelper.getHandledOutput(output);
//          }
        output = caHelper.getHandledOutput(output);
        if (ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            Assert.assertFalse(output.contains("acl" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
        } else {
            Assert.assertFalse(output.contains("" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
        }

    }

    @Test(timeOut = timeOut, groups = {"scheduleRollbackWithArchiveBeforeRollback_GROUP"}, enabled = true, description = "Schedule Rollback with archive before rollback", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void scheduleRollbackWithArchiveBeforeRollback(Dataset ds) throws Exception {

        ManagedNetworkElement deviceMNE = null;
        try {
            deviceMNE = inventoryUtils.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_" + this.getDeviceIP());
        } catch (Exception e) {
            throw new SkipException("The device " + this.getDeviceName() + " is not added with expected managed state..Hence skipping the test cases");
        }

        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();
        TestLogger.info("device_MEI:  " + device_MEI);
        TestLogger.info("device_name:  " + deviceNameInPrime);
        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        int accListNumber2 = Math.abs(random.nextInt());

        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";

        String createACLCommand2 = "";
        String showACLCommand2 = "";
        String deleteACLCommand2 = "";

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
            commands = new String[]{"application group", createACLCommand, "add 50331740", "exit"};
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
            showACLCommand = "show ip access-list | i Standard IP access list " + accListNumber + "_";
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        ConfigArchiveJobDto configArchiveJobDto = caHelper.scheduleArchive(
                device_MEI, groupLevel);
        String runJobID = caHelper.waitWhileJobCompleted(configArchiveJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobID, archives),
                "no archive has been created");

        String archiveVersionId = caHelper.getArchiveObjectVersion(runJobID, archives);
        TestLogger.info("archiveVersionId: " + archiveVersionId);

        if (ds.getAsString("deviceType").equals("ASR9K")) {

            createACLCommand2 = "ipv4 access-list acl" + accListNumber2 + " permit any";
            showACLCommand2 = "show ipv4 access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ipv4 access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand2 = "ip access-list acl" + accListNumber2;
            showACLCommand2 = "show ip access-list | i " + accListNumber2;
            deleteACLCommand2 = "no ip access-list acl" + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit tcp any any", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            createACLCommand2 = "acl create acl_" + accListNumber2;
            showACLCommand2 = "show acl summary";
            deleteACLCommand2 = "acl delete acl_" + accListNumber2;
            commands = new String[]{"config", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            createACLCommand2 = "name app_" + accListNumber2;
            showACLCommand2 = "show application group";
            deleteACLCommand2 = "no application group app_" + accListNumber2;
            commands = new String[]{"application group", createACLCommand2, "add 50331742", "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand2 = "access-list  acl_" + accListNumber2 + " standard permit any4";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no " + createACLCommand2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            createACLCommand2 = "ip access-list acl_" + accListNumber2 + " permit any";
            showACLCommand2 = "show run | i access-list acl_";
            deleteACLCommand2 = "no ip access-list acl_" + accListNumber2;
            commands = new String[]{"config t", createACLCommand2, "exit"};
        } else {
            accListNumber2 = accListNumber2 % 99 + 1;
            createACLCommand2 = "ip access-list standard " + accListNumber2;
            showACLCommand2 = "show ip access-list | i Standard IP access list " + accListNumber2 + "_";
            deleteACLCommand2 = "no ip access-list standard " + accListNumber2;
            commands = new String[]{"configure t", createACLCommand2, "permit any", "end", "wr"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber2),
                "There is no acl with name contains " + accListNumber2);

        // Delete 1st ACL and assert that it is deleted from the device 
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            commands = new String[]{"", deleteACLCommand};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        }
        caHelper.getOutputFromDevice(this.device, commands);
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
//          if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//        output = caHelper.getHandledOutput(output);
//          }
        output = caHelper.getHandledOutput(output);
        Assert.assertFalse(output.contains("" + accListNumber),
                "There is acl with name contains " + accListNumber + " which is not expected");

        // schedule Rollback
        ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO = caHelper.rollbackArchive(archiveVersionId, false, true);
        runJobID = caHelper.waitWhileRollbackJobCompleted(configArchiveRollbackJobDTO, ds);

        archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobID, archives),
                "no archive has been created");

        String output2 = "";
        // Assert that 2 acls exist on the device 
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output2 = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output = caHelper.getHandledOutput(output);
        output2 = caHelper.getHandledOutput(output2);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber + " which is not expected");
        if (!ds.getAsString("deviceType").equals("WLC")) {
        Assert.assertTrue(output2.contains("" + accListNumber2),
                "There is no acl with name contains " + accListNumber2 + " which is not expected");
        }
        // Delete 1st and 2nd ACLs and assert that they are deleted from the device 
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "exit", "copy r st"};
            caHelper.getOutputFromDevice(this.device, commands);
        } else if (ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "commit", "exit"};
            caHelper.getOutputFromDevice(this.device, commands);

        } else if (ds.getAsString("deviceType").equals("WLC")) {
            commands = new String[]{"config", deleteACLCommand, "exit"};
            caHelper.getOutputFromDevice(this.device, commands);
//            commands = new String[]{"config", deleteACLCommand2, "exit"};
//            caHelper.getOutputFromDevice(this.device, commands);
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            commands = new String[]{"", deleteACLCommand};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"", deleteACLCommand2};
            caHelper.getOutputFromDevice(this.device, commands);
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
            caHelper.getOutputFromDevice(this.device, commands);
            commands = new String[]{"configure t", deleteACLCommand2, "exit", "wr"};
            caHelper.getOutputFromDevice(this.device, commands);
        }
        output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output2 = caHelper.getOutputFromDevice(this.device,
                showACLCommand2);
        output = caHelper.getHandledOutput(output);
        output2 = caHelper.getHandledOutput(output2);
//         if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")){
//        output = caHelper.getHandledOutput(output);
//        output2 = caHelper.getHandledOutput(output2);
//          }
        if (ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            Assert.assertFalse(output.contains("acl" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
            Assert.assertFalse(output2.contains("acl" + accListNumber2),
                    "There is acl with name contains " + accListNumber2 + " which is not expected");
        } else {
            Assert.assertFalse(output.contains("" + accListNumber),
                    "There is acl with name contains " + accListNumber + " which is not expected");
            Assert.assertFalse(output2.contains("" + accListNumber2),
                    "There is acl with name contains " + accListNumber2 + " which is not expected");
        }

    }

    private String toSlash(String node) {
        return node.replace('.', '/');
    }

}
