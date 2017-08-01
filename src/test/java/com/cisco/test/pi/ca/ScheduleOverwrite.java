package com.cisco.test.pi.ca;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.EnumUtility;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigSyncJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveRollbackRestIntf;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;
import com.cisco.srtg.test.aems.helper.utils.Inventory_util;
import java.util.Random;
import org.testng.SkipException;

public class ScheduleOverwrite extends BaseTest {

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
    }

    @Test(timeOut = timeOut, groups = {"scheduleOverwrite_GROUP"}, enabled = true, dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, description = "Schedule Overwrite", dependsOnMethods = {"addDevice"})
    public void scheduleOverwrite(Dataset ds) throws Exception, URISyntaxException,
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
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";
        String[] commands = null;
        boolean isPostArchive = ds.getAsBoolean("isPostArchive");
//                 if (ds.getAsString("deviceType").equals("C3750E")){

        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand = "ip access-list acl" + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit tcp any any", "exit"};
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
            commands = new String[]{"configure t", createACLCommand, "permit any", "end"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i 'access-list " + accListNumber + "'");
            output = caHelper.getHandledOutput(output);
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list acl_" + accListNumber);
            output = caHelper.getHandledOutput(output);
        } else {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list " + accListNumber + "_");
            output = caHelper.getHandledOutput(output);
        }

        TestLogger.info("output is #START" + output + "#END");

        Assert.assertFalse(output.contains("" + accListNumber),
                "There is acl with name contains " + accListNumber);

        ConfigSyncJobDto configSyncJobDto = caHelper.scheduleOverwrite(device_MEI, isPostArchive);
        caHelper.waitWhileConfigSyncJobCompleted(configSyncJobDto);

        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i 'access-list " + accListNumber + "'");
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list acl_" + accListNumber);
        } else {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list " + accListNumber + "_");
        }

        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        //Delete ACL
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        }
        caHelper.getOutputFromDevice(device, commands);

    }  // end of test

    @Test(timeOut = timeOut, groups = {"scheduleOverwriteWithArchiveAfter_GROUP"}, enabled = true, dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, description = "Schedule Overwrite with archive after overwrite", dependsOnMethods = {"addDevice"})
    public void scheduleOverwriteWithArchiveAfter(Dataset ds) throws Exception, URISyntaxException,
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

        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());
        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";
        String[] commands = null;
        boolean isPostArchive = ds.getAsBoolean("isPostArchive");
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            createACLCommand = "ip access-list acl" + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit tcp any any", "exit"};
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
            commands = new String[]{"configure t", createACLCommand, "permit any", "end"};
        }

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i 'access-list " + accListNumber + "'");
            output = caHelper.getHandledOutput(output);
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list acl_" + accListNumber);
            output = caHelper.getHandledOutput(output);
        } else {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list " + accListNumber + "_");
            output = caHelper.getHandledOutput(output);

        }
        Assert.assertFalse(output.contains(accListNumber + " "),
                "There is acl with name contains " + accListNumber);

        ConfigSyncJobDto configSyncJobDto = caHelper.scheduleOverwrite(device_MEI, isPostArchive);
        String runJobId = caHelper.waitWhileConfigSyncJobCompleted(configSyncJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
		// Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobId, archives),
                "no archive has been created");
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i 'access-list " + accListNumber + "'");
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list acl_" + accListNumber);
        } else {
            output = caHelper.getOutputFromDevice(this.device,
                    "show startup-config | i access-list " + accListNumber + "_");
        }
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        //Delete ACL
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        }
        caHelper.getOutputFromDevice(this.device, commands);

    }  // end of test

}
