package com.cisco.test.pi.ca;

import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import com.cisco.ifm.preference.PreferenceStore;
import org.testng.annotations.Test;
import org.testng.SkipException;

import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.EnumUtility;
import com.cisco.ifm.inventoryservice.InventoryService;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.TestEnvironment;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceFactory;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigFile;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;
import com.cisco.ifm.config.archive.service.data.Configlet;
import com.cisco.ifm.config.archive.service.data.ProcessedConfig;
import com.cisco.ifm.config.archive.service.exceptions.IfmConfigArchiveException;
import com.cisco.ifm.config.archive.service.intf.IfmConfigArchiveService;
import com.cisco.srtg.test.aems.helper.utils.Inventory_util;
import com.cisco.test.pi.ca.ConfigArchiveHelper;
import java.util.Random;
import java.util.Set;

public class ScheduleArchive extends BaseTest {

    @Autowired
    IfmConfigArchiveService ifmConfigArchiveService;
            
    @Autowired
    ConfigArchiveHelper caHelper;
    private static SessionFactory sessionFactory;
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

    // private DeviceFullViewAppHelper appHelper;
    @Autowired
    private PersistenceFactory persistenceFactory;

    @Autowired
    private InventoryService inventoryService;

    // private CATestUtil testUtil;
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
        if (!ds.getAsBoolean("isSimulated")) {
            caHelper.deleteACLsOnDevice(this.device, ds);
        }
    }

    @Test(timeOut = timeOut, groups = {"checkInitialArchive_GROUP"}, enabled = true, description = "Check initial archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void checkInitialArchive(Dataset ds) throws Exception {

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

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper
                .getArchiveObject("Initial version", archives),
                "No initial archive exists");

    }

    @Test(timeOut = timeOut, groups = {"scheduleArchive_GROUP"}, enabled = true, description = " schedule archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void scheduleArchive(Dataset ds) throws Exception {

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

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

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
            showACLCommand = "show run | i 'ip access-list acl" + accListNumber + "'";
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "exit", "copy r st"};
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
        // Delete ACL
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {

            commands = new String[]{"configure t", deleteACLCommand, "do copy r st", "exit"};
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

    }

    @Test(timeOut = timeOut, groups = {"scheduleArchiveWhenNoDifference_GROUP"}, enabled = true, description = " schedule archive when no difference", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"scheduleArchive"})
    public void scheduleArchiveWhenNoDifference(Dataset ds) throws Exception {

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
        // schedule 1st archive
        ConfigArchiveJobDto configArchiveJobDto = caHelper.scheduleArchive(
                device_MEI, groupLevel);
        caHelper.waitWhileJobCompleted(configArchiveJobDto);
        // schedule 2nd archive without make any change on the device
        // "NO ARCHIVE SHOULD BE CREATED"
        configArchiveJobDto = caHelper.scheduleArchive(device_MEI, groupLevel);

        String runJobID = caHelper.waitWhileJobCompleted(configArchiveJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId and There are no changes in the device configuration.
        Assert.assertTrue(caHelper.getArchiveObject(runJobID + ". There are no changes in the device configuration.", archives),
                "no archive has been created which not expected");

    }

    @Test(timeOut = timeOut, groups = {"XR"}, enabled = false, description = "Archive group service", dependsOnMethods = {"scheduleArchiveWhenNoDifference"})
    public void archiveGroupService() throws Exception {

        throw new SkipException("Skipping - This is not ready for testing ");
        //	
        // ManagedNetworkElement deviceMNE = inventoryUtils
        // .getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
        // + this.getDeviceIP());
        //		
        // String device_MEI = deviceMNE.getInstanceId()+"";
        // String deviceNameInPrime = deviceMNE.getName();
        // TestLogger.info("device_MEI:  " + device_MEI);
        // TestLogger.info("device_name:  " + deviceNameInPrime);
        //		
        // boolean groupLevel = true;
        // Random random = new Random();
        // int accListNumber = Math.abs(random.nextInt());
        //		
        // String createACLCommand =
        // "ipv4 access-list acl"+accListNumber+" permit any";
        // caHelper.commitCommandInConfigMode(routerFramework,createACLCommand,"");
        //		
        // ConfigArchiveJobDto configArchiveJobDto =
        // caHelper.scheduleArchive("-1", groupLevel);
        // String runJobID =
        // caHelper.waitWhileJobCompleted(configArchiveJobDto);
        //
        // List<ConfigVersion> archives = (List<ConfigVersion>)
        // caHelper.getObjects("deviceName", deviceNameInPrime,
        // ConfigVersion.class.getCanonicalName());
        // // Assert new archive has been created with description contains the
        // runJobId
        // Assert.assertTrue(caHelper.getArchiveObject(runJobID,archives),
        // "no archive has been created");
        // //Delete ACL
        // String deleteACLCommand = "no ipv4 access-list acl"+accListNumber;
        // caHelper.commitCommandInConfigMode(routerFramework,deleteACLCommand,"");

    }

    @Test(timeOut = timeOut, groups = {"archiveAfterSync_GROUP"}, enabled = true, description = "Archive after sync", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void archiveAfterSync(Dataset ds) throws Exception {

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

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        setHoldOffTimer("default.adminPreferences",
                "outofboxArchiveIntegration", "true");
        setHoldOffTimer("default.adminPreferences", "inventoryIntegration",
                "true");

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
            showACLCommand = "show run | i 'ip access-list acl" + accListNumber + "'";
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "do copy r st", "exit"};
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

        String deviceId = this.getDeviceIdentifier();
        inventoryUtils.syncAEMSServer(deviceId);
        inventoryUtils.waitWhileDeviceIsSyncing(deviceId, 12000, 10);

        TestLogger.info("Wait while Archived by inventory  is created");
        caHelper.waitWhileArchiveIsCreated("Archived by inventory",
                deviceNameInPrime);

        // disable archive after sync option
        setHoldOffTimer("default.adminPreferences", "inventoryIntegration",
                "false");
        
        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        
            // Delete ACL
            if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {

                commands = new String[]{"configure t", deleteACLCommand, "do copy r st", "exit"};
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
            // Assert new archive has been created with description contains the runJobId
            Assert.assertTrue(caHelper.getArchiveObject("Archived by inventory",
                    archives), "No archive created by inventory exists");
        
    }

    @Test(timeOut = timeOut, groups = {"archiveViaSysLog_GROUP"}, enabled = true, description = "Archive via  sysLog", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void archiveViaSysLog(Dataset ds) throws Exception {

        String deviceId = this.getDeviceIdentifier();
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
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);
        TestEnvironment env = TestEnvironment.getInstance();
        env.reset();
        TestLogger.info("TestEnvironment object is : " + env);

        String serverIP = "1.1.1.1";
        TestLogger.info("serverIP:  " + serverIP);

        setHoldOffTimer("default.adminPreferences",
                "syslogHoldOffTimer", "1");

        boolean groupLevel = false;
        Random random = new Random();
        int accListNumber = Math.abs(random.nextInt());

        String createACLCommand = "";
        String showACLCommand = "";
        String deleteACLCommand = "";
        String[] commands = null;
        String[] enableSysLog = null;
        String[] disableSysLog = null;
        if (ds.getAsString("deviceType").equals("ASR9K")) {
            enableSysLog = new String[]{"configure t", "logging " + serverIP, "commit", "exit"};
            disableSysLog = new String[]{"configure t", "no logging " + serverIP, "commit", "exit"};

            createACLCommand = "ipv4 access-list acl" + accListNumber + " permit any";
            showACLCommand = "show ipv4 access-list | i " + accListNumber;
            deleteACLCommand = "no ipv4 access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "commit", "exit"};

        } else if (ds.getAsString("deviceType").equals("ASR1002")) {
            enableSysLog = new String[]{"configure t", "logging host " + serverIP + " vrf Mgmt-intf", "exit"};
            disableSysLog = new String[]{"configure t", "no logging " + serverIP, "exit", "wr"};
            accListNumber = accListNumber % 99 + 1;
            createACLCommand = "ip access-list standard " + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        } else if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
            enableSysLog = new String[]{"configure t", "logging server " + serverIP, "exit"};
            disableSysLog = new String[]{"configure t", "no logging server " + serverIP, "exit", "copy r st"};
            createACLCommand = "ip access-list acl" + accListNumber;
            showACLCommand = "show run | i 'ip access-list acl" + accListNumber + "'";
            deleteACLCommand = "no ip access-list acl" + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "exit", "copy r st"};
        } else if (ds.getAsString("deviceType").equals("WLC")) {
            enableSysLog = new String[]{"config", "logging syslog host " + serverIP, "exit"};
            disableSysLog = new String[]{"config", "logging syslog host " + serverIP + " delete", "exit"};
            createACLCommand = "acl create acl_" + accListNumber;
            showACLCommand = "show acl summary";
            deleteACLCommand = "acl delete acl_" + accListNumber;
            commands = new String[]{"config", createACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("NAM")) {
            enableSysLog = new String[]{"syslog", "remote-server " + serverIP, "exit"};
            disableSysLog = new String[]{"syslog", "remote-server disable", "exit"};
            createACLCommand = "name app_" + accListNumber;
            showACLCommand = "show application group";
            deleteACLCommand = "no application group app_" + accListNumber;
            commands = new String[]{"application group", createACLCommand, "add 50331742", "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            enableSysLog = new String[]{"config t", "logging host management  " + serverIP, "exit"};
            disableSysLog = new String[]{"config t", "no logging host management  " + serverIP, "exit"};
            createACLCommand = "access-list  acl_" + accListNumber + " standard permit any4";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no " + createACLCommand;
            commands = new String[]{"config t", createACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
            enableSysLog = new String[]{"configure t", "logging host " + serverIP, "exit"};
            disableSysLog = new String[]{"configure t", "no logging host " + serverIP, "exit"};
            createACLCommand = "ip access-list acl_" + accListNumber + " permit any";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no ip access-list acl_" + accListNumber;
            commands = new String[]{"config t", createACLCommand, "exit"};
        } else {
            enableSysLog = new String[]{"configure t", "logging " + serverIP, "exit"};
            disableSysLog = new String[]{"configure t", "no logging " + serverIP, "exit", "wr"};
            accListNumber = accListNumber % 99 + 1;
            createACLCommand = "ip access-list standard " + accListNumber;
            showACLCommand = "show ip access-list | i " + accListNumber;
            deleteACLCommand = "no ip access-list standard " + accListNumber;
            commands = new String[]{"configure t", createACLCommand, "permit any", "end", "wr"};
        }

        // enable informational trap level
        String [] trapSeverityLevel = new String[]{"configure t", "logging trap informational", "exit"};
        caHelper.getOutputFromDevice(this.device, trapSeverityLevel);
        
        // enable syslog
        caHelper.getOutputFromDevice(this.device, enableSysLog);

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showACLCommand);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        TestLogger.info("Wait while Archived by syslog  is created");
        caHelper.waitWhileArchiveIsCreated("Archived by syslog", deviceNameInPrime);

//        setHoldOffTimer("default.adminPreferences",
//                "syslogHoldOffTimer", "10");

        // disable syslog on device
        caHelper.getOutputFromDevice(this.device, disableSysLog);

        // Delete ACL
        if (ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {

            commands = new String[]{"configure t", deleteACLCommand, "do copy r st", "exit"};
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

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper.getObjects("deviceName", deviceNameInPrime, ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the runJobId
        Assert.assertTrue(caHelper.getArchiveObject("Archived by syslog", archives), "No archive is created by syslog exists");

    }

    @Test(timeOut = timeOut, groups = {"checkVlanConfig_GROUP"}, enabled = true, description = "checkVlanConfig", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void checkVlanConfig(Dataset ds) throws Exception {

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

        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();
        TestLogger.info("device_MEI:  " + device_MEI);
        TestLogger.info("device_name:  " + deviceNameInPrime);

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        String archiveVlanConfig = caHelper.getVlanConfigForArchive(
                "Initial version", archives);
        if (archiveVlanConfig == null) {
            Assert.assertTrue(false, "no configuration found for Vlan");
        }
        String[] archiveVlanConfigAsArray = archiveVlanConfig
                .split("\\r?\\n");

        int length = archiveVlanConfigAsArray.length;
        for (int i = 0; i < length; i++) {
            TestLogger.info(i + archiveVlanConfigAsArray[i]);
        }
    }

    @Test(timeOut = timeOut, groups = {"checkVrfConfiglet_GROUP"}, description = "Check VRf definition configlet for Cat3850", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, enabled = true)
    public void checkVrfConfiglet(Dataset ds) throws Exception {

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

        String deviceNameInPrime = deviceMNE.getName();

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);
        
        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        long runningConfigInstanceId = 0;
        ConfigVersion archive = caHelper.getArchiveObjectByJobId("Initial version", archives);
        Set<ConfigFile> configFiles = archive.getConfigFile();
        for (ConfigFile f : configFiles) {
            TestLogger.info("ConfigFile type: " + f.getConfigFileType());
            TestLogger.info("ConfigFile Id: " + f.getInstanceId());
            if (f.getConfigFileType().toString().contains("RUNNINGCONFIG")) {
               runningConfigInstanceId = f.getInstanceId();
            }
        }
        Assert.assertTrue(checkVrfDefinitionConfigletName(runningConfigInstanceId), "Vrf Definition configlet does not exist" );
    }
    
    @Test(timeOut = timeOut, groups = {"checkCommandLLDP_GROUP"}, enabled = true, description = "Check LLDP command for Nexus5k", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void checkCommandLLDP(Dataset ds) throws Exception {

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

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        boolean groupLevel = false;
        String setTimer = "";
        String showLLDP = "";
        String unsetTimer = "";
        String[] commands = null;

        setTimer = "lldp timer 60";
        showLLDP = "show run | i 'lldp timer'";
        unsetTimer = "no lldp timer 60";
        commands = new String[]{"configure t", setTimer, "exit", "copy r st"};

        caHelper.getOutputFromDevice(this.device, commands);
        String output = caHelper.getOutputFromDevice(this.device,
                showLLDP);
        output = caHelper.getHandledOutput(output);
        Assert.assertTrue(output.contains("" + setTimer),
                "There is no LLDP command on device ");

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
        // unset LLDP timer
        commands = new String[]{"configure t", unsetTimer, "do copy r st", "exit"};
        caHelper.getOutputFromDevice(this.device, commands);
        
        long runningConfigInstanceId = 0;
        ConfigVersion archive = caHelper.getArchiveObjectByJobId(runJobID, archives);
        Set<ConfigFile> configFiles = archive.getConfigFile();
        for (ConfigFile f : configFiles) {
            TestLogger.info("ConfigFile type: " + f.getConfigFileType());
            TestLogger.info("ConfigFile Id: " + f.getInstanceId());
            if (f.getConfigFileType().toString().contains("RUNNINGCONFIG")) {
                runningConfigInstanceId = f.getInstanceId();
            }
        }
        Assert.assertTrue(VerifyLLAPCommandUnderGlobalConfiglet(runningConfigInstanceId), "LLDP command does not exist under global config");   
     
    }
    
    public void setHoldOffTimer(String node, String key, String time) {

        try {
            PreferenceStore pStore = PreferenceStore.getInstance();
            pStore.saveSystemPrefence(toSlash(node), key, time);
            TestLogger
                    .info("=================Hold Off Timer Value Changed Successfully");
        } catch (Exception e) {
            TestLogger
                    .info("=================Exception in changing Hold Off Timer Value ");
            TestLogger.info(e.getLocalizedMessage());
        }
    }

    private String toSlash(String node) {
        return node.replace('.', '/');
    }

    public boolean checkVrfDefinitionConfigletName(long instanceId) throws IfmConfigArchiveException {
        
        ProcessedConfig processedConfig = ifmConfigArchiveService.getProcessedConfig(instanceId);
        Configlet[] configletList = processedConfig.getConfiglets();
        for (int i = 0; i < configletList.length; i++) {
            TestLogger.info("Name: " + configletList[i].getName());
            if (configletList[i].getName().equalsIgnoreCase("Vrf")) {
                Configlet[] subConfiglets = configletList[i].getSubConfiglets();
                for (int j = 0; j < subConfiglets.length; j++) {
                    if (subConfiglets[j].getName().equalsIgnoreCase("Vrf Definition")) {
                        TestLogger.info("Name: " + subConfiglets[j].getName());
                        return true;
                    }
                    break;
                }
                break;
            }
        }
        return false;
    }
    
    public boolean VerifyLLAPCommandUnderGlobalConfiglet(long instanceId) throws IfmConfigArchiveException {

        ProcessedConfig processedConfig = ifmConfigArchiveService.getProcessedConfig(instanceId);
        Configlet[] configletList = processedConfig.getConfiglets();
        for (int i = 0; i < configletList.length; i++) {
            TestLogger.info("Name: " + configletList[i].getName());
            if (configletList[i].getName().equalsIgnoreCase("Global")) {
                for (int j = 0; j < configletList[i].getCommands().length; j++) {
                    if (configletList[i].getCommands()[j].contains("lldp timer")) {
                        TestLogger.info("Command number " + j + ":" + configletList[i].getCommands()[j]);
                        return true;
                    }
                }
                break;
            }
        } 
        return false;
    }
}
