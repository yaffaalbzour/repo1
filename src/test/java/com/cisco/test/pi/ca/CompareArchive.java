package com.cisco.test.pi.ca;

import java.util.List;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveDTOGenerator;
import com.cisco.ifm.config.archive.service.intf.IfmConfigArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.cisco.ifm.config.archive.rest.dto.ConfigMismatchDTO;
import com.cisco.ifm.config.archive.service.data.diff.ConfigDiff;

import com.cisco.dp.test.BaseTest;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;

import com.cisco.srtg.test.aems.helper.utils.Inventory_util;
import java.util.Random;
import org.testng.SkipException;

;

public class CompareArchive extends BaseTest {

    @Autowired
    IfmConfigArchiveService ifmConfigArchiveService;

    @Autowired
    ConfigArchiveHelper caHelper;

    @Autowired
    IJobSchedulerRestService jobSchedulerServiceImpl;

    @Autowired
    public Inventory_util inventoryUtils;

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Autowired
    private ConfigArchiveManager configArchiveManager;

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

    // Archive, then compare running confg in archive with those on the device.
    @Test(timeOut = timeOut, groups = {"compareRunningConfWithDeviceAfterArchive_GROUP"}, enabled = true, description = "Compare RunningConfig for device and archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void compareRunningConfWithDeviceAfterArchive(Dataset ds) throws Exception {

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
        } else if (ds.getAsString("deviceType").equals("WLC")) {

            createACLCommand = "acl create " + accListNumber;
            showACLCommand = "show acl summary";
            deleteACLCommand = "acl delete " + accListNumber;
            commands = new String[]{"config", createACLCommand, "exit"};
        } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            createACLCommand = "access-list  acl_" + accListNumber + " standard permit any4";
            showACLCommand = "show run | i access-list acl_";
            deleteACLCommand = "no " + createACLCommand;
            commands = new String[]{"configure t", createACLCommand, "exit"};
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
        Assert.assertTrue(output.contains("" + accListNumber),
                "There is no acl with name contains " + accListNumber);

        ConfigArchiveJobDto configArchiveJobDto = caHelper.scheduleArchive(
                device_MEI, groupLevel);
        String runJobId = caHelper.waitWhileJobCompleted(configArchiveJobDto);

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        // Assert new archive has been created with description contains the
        // runJobId
        Assert.assertTrue(caHelper.getArchiveObject(runJobId, archives),
                "no archive has been created");

        String archiveRunningConfig = caHelper.getRunningConfigForArchive(
                runJobId, archives);

        if (ds.getAsString("deviceType").equals("ASR9K")) {
            archiveRunningConfig = archiveRunningConfig
                    .split("#BEGIN_MODULE_CONFIG")[0].trim();
        }

        String deviceRunningConfig = caHelper.getOutputFromDevice(this.device, "terminal length 0",
                "show running-config");

        deviceRunningConfig = deviceRunningConfig.substring(
                deviceRunningConfig.indexOf('\n') + 1).trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--           ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--         ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                "--More--", "").trim();
        String[] deviceRunningConfigAsArray = deviceRunningConfig
                .split("\\r?\\n");
        String[] archiveRunningConfigAsArray = archiveRunningConfig
                .split("\\r?\\n");

        int deviceRunLength = deviceRunningConfigAsArray.length;
        int archiveRunLength = archiveRunningConfigAsArray.length;

        TestLogger.info("deviceRunLength: " + deviceRunLength);
        TestLogger.info("archiveRunLength: " + archiveRunLength);
        int length = Math.min(deviceRunLength, archiveRunLength);

        if (deviceRunningConfigAsArray[0].contains("bytes")) {
            for (int i = 0; i < deviceRunLength - 1; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + 1];
            }
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }

            int m = 0; // m is the number of 1st line that contains "!"
            while (!(deviceRunningConfigAsArray[m].contains("!") || m==deviceRunningConfigAsArray.length)) {
                m++;
            }

            for (int i = 0; i < length - m - 3; i++) {

                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m + 2];
                 if (ds.getAsString("deviceType").equals("ASR9K")) {
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 3];     
                 }
                 else {
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 2];
                 }
            }
            length = length - m;

        if (ds.getAsString("deviceType").equals("ASR1001X") || ds.getAsString("deviceType").equals("C3750X") || ds.getAsString("deviceType").equals("C6880X") || ds.getAsString("deviceType").equals("C2960XR") || ds.getAsString("deviceType").equals("C2960Plus") || ds.getAsString("deviceType").equals("C7613") || ds.getAsString("deviceType").equals("IE2000") || ds.getAsString("deviceType").equals("IE3000") || ds.getAsString("deviceType").equals("newSupport") || ds.getAsString("deviceType").equals("IE4000") || ds.getAsString("deviceType").equals("C3750X-24P") || ds.getAsString("deviceType").equals("ME3600") || ds.getAsString("deviceType").equals("ISRv") || ds.getAsString("deviceType").equals("ISRG2") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (deviceRunningConfigAsArray[i].length() != 0) {
                    deviceRunningConfigAsArray[j] = deviceRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }

        // Fix empty lines issue for archive file
        if (ds.getAsString("deviceType").equals("ASR1001X") || ds.getAsString("deviceType").equals("C3750X") || ds.getAsString("deviceType").equals("C6880X") || ds.getAsString("deviceType").equals("C2960XR") || ds.getAsString("deviceType").equals("C2960Plus") || ds.getAsString("deviceType").equals("C7613") || ds.getAsString("deviceType").equals("IE2000") || ds.getAsString("deviceType").equals("IE3000") || ds.getAsString("deviceType").equals("newSupport") || ds.getAsString("deviceType").equals("IE4000") || ds.getAsString("deviceType").equals("C3750X-24P") || ds.getAsString("deviceType").equals("ME3600") || ds.getAsString("deviceType").equals("ISRv") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (archiveRunningConfigAsArray[i].length() != 0) {
                    archiveRunningConfigAsArray[j] = archiveRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }
        if (ds.getAsString("deviceType").equals("ASA")) {
            length = 100;
             m = 0; // m is the number of line that contains "Saved" word
            while (!deviceRunningConfigAsArray[m].contains("Saved")) {
                m++;
            }
            int realLength = length - m;

            for (int i = 0; i < realLength - 3; i++) {

                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m];
            }
        }

        if (ds.getAsString("deviceName").equals("ASAv")) {

            m = 0; // m is the number of line that contains "hostname" word
            while (!deviceRunningConfigAsArray[m].contains("hostname")) {
                m++;
            }
            for (int i = 0; i < length - 3; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m];
                TestLogger.info(i + "TdeviceRunningConfigAsArray" + deviceRunningConfigAsArray[i]);
            }

            m = 0;
            while (!archiveRunningConfigAsArray[m].contains("hostname")) {
                m++;
            }
            for (int i = 0; i < length - 3; i++) {
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + m];
                TestLogger.info(i + "TarchiveRunningConfigAsArray" + archiveRunningConfigAsArray[i]);
            }

        }

        if (ds.getAsString("deviceType").equals("Nexus3048")) {
            length = 100;
             m = 0; // m is the number of line that contains "Command" word on device
            while (!deviceRunningConfigAsArray[m].contains("!Command:")) {
                m++;
            }
            int realLength = length - m;

            for (int i = 0; i < realLength - 3; i++) {

                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m];
            }
            m = 0; // m is the number of line that contains "Command" word in archive file
            while (!archiveRunningConfigAsArray[m].contains("!Command:")) {
                m++;
            }
            length = realLength - m;
            for (int i = 0; i < length - 3; i++) {

                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + m];
            }
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }

        TestLogger.info("** Start comparing running config on device and archive ***");
        for (int i = 0; i < length - 5; i++) {
            TestLogger.info(i + deviceRunningConfigAsArray[i] + "-"
                    + archiveRunningConfigAsArray[i]);
            Assert.assertEquals(deviceRunningConfigAsArray[i].trim(),
                    archiveRunningConfigAsArray[i].trim(), "" + i
                    + deviceRunningConfigAsArray[i].trim() + "-"
                    + archiveRunningConfigAsArray[i].trim());

        }
        TestLogger
                .info("** Finish comparing running config on device and archive ***");

        // Delete ACL
        if (!ds.getAsString("deviceType").equals("ASR9K")) {
            commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
        } else {
            commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
        }
        caHelper.getOutputFromDevice(this.device, commands);

    }

    @Test(timeOut = timeOut, groups = {"getLatestDiff_GROUP"}, enabled = true, description = "get latest diff", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"compareRunningConfWithDeviceAfterArchive"})
    public void getLatestDiff(Dataset ds) throws Exception {

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

        ConfigVersion configVersion = configArchiveManager
                .getLatestArchivedConfigVersion(device_MEI);

        ConfigMismatchDTO configMismatchDTO = ConfigArchiveDTOGenerator
                .prepareMismatchDTO(configVersion, deviceMNE + "");

        long fileId = configMismatchDTO.getRunningFileId();
        TestLogger.info("RunningFileID :" + fileId);

        ConfigDiff latestConfigDiff = ifmConfigArchiveService.getLatestConfigDiff(fileId, true);
        Assert.assertNotNull(latestConfigDiff, "The config diff returns null");
        TestLogger.info("Finished get latest diff test, success");
    }

    @Test(timeOut = timeOut, groups = {"compareRunningConfWithDeviceForSimDevices_GROUP"}, enabled = true, description = "Compare RunningConfig for device and archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void compareRunningConfWithDeviceForSimDevices(Dataset ds) throws Exception {

        ManagedNetworkElement deviceMNE = inventoryUtils
                .getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
                        + this.getDeviceIP());

        String device_MEI = deviceMNE.getInstanceId() + "";
        String deviceNameInPrime = deviceMNE.getName();
        TestLogger.info("device_MEI:  " + device_MEI);
        TestLogger.info("device_name:  " + deviceNameInPrime);

        TestLogger.info("Wait while initial archive is created");
        caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);

        String deviceRunningConfig = caHelper.getOutputFromDevice(this.device,
                "show running-config");

        deviceRunningConfig = deviceRunningConfig.substring(
                deviceRunningConfig.indexOf('\n') + 1).trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--           ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--         ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                "--More--", "").trim();
        String[] deviceRunningConfigAsArray = deviceRunningConfig
                .split("\\r?\\n");

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        String archiveRunningConfig = caHelper.getRunningConfigForArchive(
                "Initial version", archives);
        String[] archiveRunningConfigAsArray = archiveRunningConfig
                .split("\\r?\\n");

        int deviceRunLength = deviceRunningConfigAsArray.length;
        int archiveRunLength = archiveRunningConfigAsArray.length;

        TestLogger.info("deviceRunLength: " + deviceRunLength);
        TestLogger.info("archiveRunLength: " + archiveRunLength);
        int length = Math.min(deviceRunLength, archiveRunLength);

        if (deviceRunningConfigAsArray[0].contains("bytes")) {
            for (int i = 0; i < deviceRunLength - 1; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + 1];
            }
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }
        if (ds.getAsString("deviceType").equals("ASR1002")) {
            for (int i = 0; i < length - 3; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + 2];
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 2];
            }
        }

        TestLogger.info("deviceRunningConfigAsArray0:" + deviceRunningConfigAsArray[0]);
        TestLogger.info("deviceRunningConfigAsArray1:" + deviceRunningConfigAsArray[1]);

        if (ds.getAsString("deviceType").equals("ISRG2") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (deviceRunningConfigAsArray[i].length() != 0) {
                    deviceRunningConfigAsArray[j] = deviceRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }

        // Fix empty lines issue for archive file
        if (ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (archiveRunningConfigAsArray[i].length() != 0) {
                    archiveRunningConfigAsArray[j] = archiveRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }
        if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            length = 100;
            for (int i = 0; i < length; i++) {
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 1];
            }
        }

        if (ds.getAsString("deviceType").equals("Nexus3048")) {
            length = 100;
            int m = 0; // m is the number of line that contains "Saved" word
            while (!deviceRunningConfigAsArray[m].contains("!Command:")) {
                m++;
            }
            int realLength = length - m;

            for (int i = 0; i < realLength - 3; i++) {

                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m];
            }
            length = realLength;
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }

        TestLogger.info("** Start comparing running config on device and archive ***");
        for (int i = 0; i < length - 5; i++) {
            TestLogger.info(i + deviceRunningConfigAsArray[i] + "-"
                    + archiveRunningConfigAsArray[i]);
            Assert.assertEquals(deviceRunningConfigAsArray[i].trim(),
                    archiveRunningConfigAsArray[i].trim(), "" + i
                    + deviceRunningConfigAsArray[i].trim() + "-"
                    + archiveRunningConfigAsArray[i].trim());

        }
        TestLogger
                .info("** Finish comparing running config on device and archive ***");

    }

    @Test(timeOut = timeOut, groups = {"compareStartupConfWithDeviceForSimDevices_GROUP"}, enabled = true, description = "Compare RunningConfig for device and archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = {"addDevice"})
    public void compareStartupConfWithDeviceForSimDevices(Dataset ds) throws Exception {

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

        String startupConfigCommand = "show startup-config";
        if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            startupConfigCommand = "show config";
        }
        String deviceRunningConfig = caHelper.getOutputFromDevice(this.device,
                startupConfigCommand);

        deviceRunningConfig = deviceRunningConfig.substring(
                deviceRunningConfig.indexOf('\n') + 1).trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--           ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                " --More--         ", "").trim();
        deviceRunningConfig = deviceRunningConfig.replace(
                "--More--", "").trim();
        String[] deviceRunningConfigAsArray = deviceRunningConfig
                .split("\\r?\\n");

        List<ConfigVersion> archives = (List<ConfigVersion>) caHelper
                .getObjects("deviceName", deviceNameInPrime,
                        ConfigVersion.class.getCanonicalName());
        String archiveRunningConfig = caHelper.getStartupConfigForArchive(
                "Initial version", archives);
        String[] archiveRunningConfigAsArray = archiveRunningConfig
                .split("\\r?\\n");

        int deviceRunLength = deviceRunningConfigAsArray.length;
        int archiveRunLength = archiveRunningConfigAsArray.length;

        TestLogger.info("deviceRunLength: " + deviceRunLength);
        TestLogger.info("archiveRunLength: " + archiveRunLength);
        int length = Math.min(deviceRunLength, archiveRunLength);

        if (deviceRunningConfigAsArray[0].contains("bytes")) {
            for (int i = 0; i < deviceRunLength - 1; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + 1];
            }
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }
        if (ds.getAsString("deviceType").equals("ASR1002")) {
            for (int i = 0; i < length - 3; i++) {
                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + 2];
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 2];
            }
        }

        if (ds.getAsString("deviceType").equals("ISRG2") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (deviceRunningConfigAsArray[i].length() != 0) {
                    deviceRunningConfigAsArray[j] = deviceRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }

        // Fix empty lines issue for archive file
        if (ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("ISR3925") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR2901")) {
            int j = 0;
            for (int i = 0; i < length - 3; i++) {
                if (archiveRunningConfigAsArray[i].length() != 0) {
                    archiveRunningConfigAsArray[j] = archiveRunningConfigAsArray[i];
                    j++;
                }
            }
            length = j;
        }
        if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
            length = 100;
            for (int i = 0; i < length; i++) {
                archiveRunningConfigAsArray[i] = archiveRunningConfigAsArray[i + 1];
            }
        }

        if (ds.getAsString("deviceType").equals("Nexus3048")) {
            length = 100;
            int m = 0; // m is the number of line that contains "Saved" word
            while (!deviceRunningConfigAsArray[m].contains("!Command:")) {
                m++;
            }
            int realLength = length - m;

            for (int i = 0; i < realLength - 3; i++) {

                deviceRunningConfigAsArray[i] = deviceRunningConfigAsArray[i + m];
            }
            length = realLength;
            deviceRunningConfigAsArray[1] = archiveRunningConfigAsArray[1];
        }

        TestLogger.info("** Start comparing running config on device and archive ***");

        for (int i = 0; i < length - 5; i++) {
            TestLogger.info(i + deviceRunningConfigAsArray[i] + "-"
                    + archiveRunningConfigAsArray[i]);
            Assert.assertEquals(deviceRunningConfigAsArray[i].trim(),
                    archiveRunningConfigAsArray[i].trim(), "" + i
                    + deviceRunningConfigAsArray[i].trim() + "-"
                    + archiveRunningConfigAsArray[i].trim());

        }
        TestLogger
                .info("** Finish comparing running config on device and archive ***");

    }

    @Test(timeOut = timeOut, groups = {"XR"}, enabled = false, description = " schedule archive", dependsOnMethods = {"addDevice"})
    public void compareArchiveRunningConfigWithOtherArchiveRunningConfig()
            throws Exception {

    }

}
