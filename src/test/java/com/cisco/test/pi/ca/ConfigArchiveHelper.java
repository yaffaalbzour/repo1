package com.cisco.test.pi.ca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
//

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.Pattern;
import com.cisco.ifm.test.connection.ConnectionConstants;
import com.cisco.ifm.test.connection.ExpectModeCommand;
import com.cisco.ifm.test.connection.ProtocolClient;
import com.cisco.test.tea.devices.NetworkDevice;
import com.cisco.ifm.test.connection.NetworkDeviceConnectionFactory;
//
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.Assert;
import com.cisco.ifm.config.archive.service.data.ConfigRollbackFileType;
import com.cisco.ifm.config.archive.service.exceptions.IfmConfigArchiveException;
import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.InventoryUtils;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.ifm.jobscheduler.rest.dto.JobDto;
import com.cisco.ifm.jobscheduler.rest.dto.JobScheduleDto;
import com.cisco.ifm.jobscheduler.rest.dto.JobSpecListDto;
import com.cisco.ifm.jobscheduler.rest.dto.JobSpecObjectDto;
import com.cisco.ifm.jobscheduler.rest.dto.JobSpecificationDto;
import com.cisco.ifm.jobscheduler.rest.dto.JobTimeDto;
import com.cisco.ifm.jobscheduler.rest.dto.TaskElementSpecificationDto;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
//import com.cisco.xmp.model.foundation.propertyValue.PropertyNameAndValue;
import com.cisco.xmp.model.managed.vendorSpecificTechnologies.cisco.modularOS.ModularOsInstallable;
import com.cisco.xmp.persistence.common.util.DMMException;
import com.cisco.xmp.persistence.spring.intf.PersistenceFactory;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.xmp.usermgmt.model.XmpUser;
import com.cisco.xmp.usermgmt.model.XmpUserDetails;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveJobDto;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveRollbackJobDTO;
import com.cisco.ifm.config.archive.rest.dto.ConfigDeployJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveRollbackRestIntf;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;

import com.cisco.ifm.config.archive.model.ConfigFile;
import com.cisco.ifm.config.archive.rest.dto.ConfigSyncJobDto;
import com.cisco.srtg.test.aems.helper.device.DeviceCliCommand;
import com.cisco.srtg.test.aems.helper.device.DeviceFramework;

import java.io.InputStream;

public class ConfigArchiveHelper extends BaseTest {

    @Autowired
    IJobSchedulerRestService jobSchedulerServiceImpl;

    @Autowired
    public ConfigArchiveRollbackRestIntf configArchiveRollbackRestIntf;

    @Autowired(required = false)
    public InventoryUtils inventoryUtils;

    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Autowired
    private ConfigArchiveManager configArchiveManager;

    @Autowired
    private ConfigArchiveViewRestIntf configArchiveViewService;

    private static SessionFactory sessionFactory;

    public String getRunningConfigForArchive(String runJobId,
            List<ConfigVersion> archivesInDB) throws SQLException, IOException {

        ConfigVersion archive = getArchiveObjectByJobId(runJobId, archivesInDB);

        Set<ConfigFile> configFiles = archive.getConfigFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (ConfigFile f : configFiles) {
            TestLogger.info("ConfigFile type: " + f.getConfigFileType());
            if (f.getConfigFileType().toString().contains("RUNNINGCONFIG")) {
                InputStream in = f.getConfigFile().getBinaryStream();

                byte[] buf = new byte[1024];
                int n = 0;
                while ((n = in.read(buf)) >= 0) {
                    baos.write(buf, 0, n);
                }
                in.close();
                byte[] bytes = baos.toByteArray();
                String blobString = new String(bytes);

                // TestLogger.info("blobString:"+blobString);
                return blobString;
            }
        }
        return null;

    }

    public String getStartupConfigForArchive(String runJobId,
            List<ConfigVersion> archivesInDB) throws SQLException, IOException {

        ConfigVersion archive = getArchiveObjectByJobId(runJobId, archivesInDB);

        Set<ConfigFile> configFiles = archive.getConfigFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (ConfigFile f : configFiles) {
            TestLogger.info("ConfigFile type: " + f.getConfigFileType());
            if (f.getConfigFileType().toString().contains("STARTUPCONFIG")) {
                InputStream in = f.getConfigFile().getBinaryStream();

                byte[] buf = new byte[1024];
                int n = 0;
                while ((n = in.read(buf)) >= 0) {
                    baos.write(buf, 0, n);
                }
                in.close();
                byte[] bytes = baos.toByteArray();
                String blobString = new String(bytes);

                // TestLogger.info("blobString:"+blobString);
                return blobString;
            }
        }
        return null;

    }

    public String getVlanConfigForArchive(String runJobId,
            List<ConfigVersion> archivesInDB) throws SQLException, IOException {

        ConfigVersion archive = getArchiveObjectByJobId(runJobId, archivesInDB);

        Set<ConfigFile> configFiles = archive.getConfigFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (ConfigFile f : configFiles) {
            TestLogger.info("ConfigFile type: " + f.getConfigFileType());
            if (f.getConfigFileType().toString().contains("VLAN")) {
                InputStream in = f.getConfigFile().getBinaryStream();

                byte[] buf = new byte[1024];
                int n = 0;
                while ((n = in.read(buf)) >= 0) {
                    baos.write(buf, 0, n);
                }
                in.close();
                byte[] bytes = baos.toByteArray();
                String blobString = new String(bytes);

                // TestLogger.info("blobString:"+blobString);
                return blobString;
            }
        }
        return null;

    }

    public JobDto getJobDto(String jobType) {
        JobDto jobDto = new JobDto();
        jobDto.setUserName("root");
        jobDto.setDescription("NoRecurrence");
        jobDto.setJobName(getJobName());

        JobScheduleDto jobSchedule = new JobScheduleDto();
        jobSchedule.setNow(true);
        jobSchedule.setMinute(false);
        jobSchedule.setMonthly(false);
        jobSchedule.setMonthlyDay("null");
        jobSchedule.setMonthlyMonth("null");
        jobSchedule.setMonthlyWeek("null");
        jobSchedule.setMonthlyWeekDay("null");
        jobSchedule.setNoOfRepetitions("0");
        jobSchedule.setWeekly(false);
        jobSchedule.setYearly(false);
        jobSchedule.setDaily(false);
        jobSchedule.setDayRepetition("null");
        jobSchedule.setDayOfWeek("null");

        JobTimeDto jobScheduleTime = new JobTimeDto();
        jobScheduleTime.setStartDate("null");
        jobSchedule.setJobScheduleTime(jobScheduleTime);
        jobDto.setJobSchedule(jobSchedule);

        // Job Specifications
        JobSpecificationDto jobSpecification = new JobSpecificationDto();
        TaskElementSpecificationDto taskSpecification = new TaskElementSpecificationDto();
        taskSpecification.setDomainSpecId("100");
        jobSpecification.setTaskSpecification(taskSpecification);
        jobDto.setJobSpecification(jobSpecification);
        jobDto.setJobType(jobType);
//		jobDto.setJobType("Config-Archive");
        return jobDto;

    }

    /**
     *
     * @return
     */
    private String getJobName() {
        // TODO Auto-generated method stub
        int rand = (int) (Math.random() * 32000);
        int rand2 = (int) (Math.random() * 32000);
        return "CA_JOB_API_" + rand + "_" + rand2;
    }

    public String waitWhileJobCompleted(ConfigArchiveJobDto configArchiveJobDto)
            throws InterruptedException {

        String jobName = configArchiveJobDto.getBaseJobDto().getJobName();
        // Wait until job is COMPLETED
        while (!getJobSpecification(jobName)
                .getWorkState().equals("Completed")
                || getJobSpecification(jobName)
                .getLastRunResultState().equals("Running")) {
            Thread.sleep(1000);
        }

        // Check completed successfully
        if (!getJobSpecification(jobName)
                .getLastRunResultState().equals("Success")) {
            if (getJobSpecification(jobName)
                    .getLastRunResultState().equals("Partial-Success")) {
                TestLogger.info("Job Status is: "
                        + getJobSpecification(jobName).getLastRunResultState());
            } else {
                Assert.assertTrue(false, "Job is not Success, it is "
                        + getJobSpecification(jobName)
                        .getLastRunResultState());
            }
        }
        String runJobId = getJobSpecification(jobName)
                .getLastRunJobId();
        TestLogger.info("runjobId: " + runJobId);
        TestLogger.info(getJobSpecification(jobName)
                .getLastRunResultState());

        return runJobId;
    }

//    public JobSpecObjectDto getConfigArchiveJobSpecification(
//            ConfigArchiveJobDto distibutedJobDTO) {
//        JobSpecListDto jobSpecListDto = jobSchedulerServiceImpl
//                .getAllJobsSpecificationByType("Configuration Archive");
//        List<JobSpecObjectDto> jobSpecs = jobSpecListDto.getItems();
//        Iterator<JobSpecObjectDto> iterator = jobSpecs.iterator();
//        while (iterator.hasNext()) {
//
//            JobSpecObjectDto jobSpecObjectDto = (JobSpecObjectDto) iterator
//                    .next();
//
//            if (jobSpecObjectDto.getJobName().equals(
//                    distibutedJobDTO.getBaseJobDto().getJobName())) {
//                TestLogger.info(distibutedJobDTO.getBaseJobDto().getJobName()
//                        + " still running");
//                return jobSpecObjectDto;
//            }
//        }
//        TestLogger
//                .info("jobespec +++++++++++++++++++++++ jobspec is nullllllll");
//
//        return null;
//    }
    /**
     *
     * @param distibutedJobDTO
     * @return
     */
    public JobSpecObjectDto getJobSpecification(
            String jobName) throws InterruptedException {

        Thread.sleep(10000);
        JobSpecListDto jobSpecListDto = jobSchedulerServiceImpl
                .getAllJobsSpecification();
        Thread.sleep(5000);
        List<JobSpecObjectDto> jobSpecs = jobSpecListDto.getItems();
        Iterator<JobSpecObjectDto> iterator = jobSpecs.iterator();
        while (iterator.hasNext()) {

            JobSpecObjectDto jobSpecObjectDto = (JobSpecObjectDto) iterator
                    .next();

            if (jobSpecObjectDto.getJobName().equals(
                    jobName)) {
                TestLogger.info(jobName
                        + " still running");
                return jobSpecObjectDto;
            }
        }
        TestLogger
                .info("Job spec is not ready yet, wait for another second.");
        Thread.sleep(1000);

        return null;
    }

    public ConfigArchiveJobDto scheduleArchive(String device_MEI,
            boolean groupLevel) throws IfmConfigArchiveException {
        JobDto baseJobDto = getJobDto("Config-Archive");

        ConfigArchiveJobDto configArchiveJobDto = new ConfigArchiveJobDto();

        configArchiveJobDto.setBaseJobDto(baseJobDto);
        configArchiveJobDto.setDeviceIds(device_MEI);
        configArchiveJobDto.setGroupLevel(groupLevel);
        configArchiveJobDto.setJobType("Configuration Archive");

        String response = configArchiveViewService.scheduleArchive(
                configArchiveJobDto, "root");
        TestLogger.info("response is: " + response);
        return configArchiveJobDto;
    }

    public ConfigSyncJobDto scheduleOverwrite(String device_MEI, boolean isPostArchive) throws IfmConfigArchiveException {

        JobDto baseJobDto = getJobDto("Configuration-Overwrite");

        ConfigSyncJobDto configSyncJobDto = new ConfigSyncJobDto();

        configSyncJobDto.setBaseJobDto(baseJobDto);
        configSyncJobDto.setDeviceIds(device_MEI);
        configSyncJobDto.setJobType("Configuration Overwrite");
        configSyncJobDto.setPostArchive(isPostArchive);
        configSyncJobDto.setWriteMem(true);

        String response = configArchiveViewService.scheduleSync(configSyncJobDto);
        TestLogger.info("response is: " + response);
        return configSyncJobDto;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     * @throws DMMException
     */
    public List<?> getObjects(String key, String value, String className)
            throws DMMException {

        TransactionStatus tranStatus = this.transactionManager
                .getTransaction(new DefaultTransactionDefinition());

        HashMap<String, String> attrMap = new HashMap<String, String>();
        attrMap.put(key, value);

        try {
            List<?> elements = this.persistenceService.getInstances(className,
                    attrMap);
            TestLogger.info(" " + elements.size()
                    + " Modules exists in DB for device with OWNINGIDENTITY: "
                    + value);
            return elements;

        } finally {
            TestLogger.info("end getMEIDeviceByIPAddress");
            if (!tranStatus.isRollbackOnly()) {
                this.transactionManager.commit(tranStatus);
                TestLogger.info("transactionManager.commit");
            } else {
                this.transactionManager.rollback(tranStatus);
                TestLogger.info("transactionManager.rollback");
            }
        }

    }

//    public String commitCommandInConfigMode(DeviceFramework routerFramework, DeviceCliCommand theCommand,
//            String[] commands, Dataset ds) throws Exception {
//        String prompt = "#";
//        if (ds.getAsString("deviceType").equals("WLC") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
//            prompt = "";
//        }
//
//        TestLogger.info("prompt: " + prompt);
//        TestLogger.info("Command: " + commands[1]);
//        TestLogger.info("-----" + commands[1] + " started-----");
////		RouterCliCommand theCommand = new RouterCliCommand("");
//        theCommand.setCommands(commands);
//
//        int flag = 1;
//        String output = null;
//        for (int i = 0; i < 5; i++) {
//            if (flag != 0) {
//                TestLogger.info("Connecting to device, try number " + i);
//                try {
//
//                    output = routerFramework.command().command(command,
//                            theCommand, prompt);
//                    TestLogger.info("\n=============\nCommand output: " + output
//                            + "\n=============\n");
//                    flag = 0;
//                    if (output == null) {
//                        flag = 1;
//                        TestLogger.info("\nConnection to device was not successful, sleep for 1 second\n");
//                        Thread.sleep(1000);
//                    }
//                } catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    flag = 1;
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        TestLogger.info("-----" + commands[1] + " finished-----");
//
//        routerFramework.getConnection().disconnect();
//        return output;
//    }
    public List<?> getAllObjects(String className) throws DMMException {

        TransactionStatus tranStatus = this.transactionManager
                .getTransaction(new DefaultTransactionDefinition());

        HashMap<String, String> attrMap = new HashMap<String, String>();
        // attrMap.put(key, value);

        try {
            List<?> elements = this.persistenceService.getInstances(className,
                    attrMap);
            return elements;

        } finally {
            TestLogger.info("end getMEIDeviceByIPAddress");
            if (!tranStatus.isRollbackOnly()) {
                this.transactionManager.commit(tranStatus);
                TestLogger.info("transactionManager.commit");
            } else {
                this.transactionManager.rollback(tranStatus);
                TestLogger.info("transactionManager.rollback");
            }
        }

    }

    public ConfigVersion getArchiveObjectByJobId(String runJobId,
            List<ConfigVersion> archivesInDB) {
        String archiveDescription;
        ModularOsInstallable availableImage = null;
        for (int j = 0; j < archivesInDB.size(); j++) {
            archiveDescription = archivesInDB.get(j).getComments();
            if (archiveDescription == null) {
                continue;
            }
            TestLogger.info("\n" + j + archiveDescription + j);
            if (archiveDescription.contains(runJobId)) {
                TestLogger.info("Archive with description contains ("
                        + runJobId + ") exists");
                return archivesInDB.get(j);
            }
        }
        return null;

    }

    public boolean getArchiveObject(String runJobId,
            List<ConfigVersion> archivesInDB) {
        String archiveDescription;
        ModularOsInstallable availableImage = null;
        for (int j = 0; j < archivesInDB.size(); j++) {
            archiveDescription = archivesInDB.get(j).getComments();
            if (archiveDescription == null) {
                continue;
            }
            TestLogger.info("\n" + j + archiveDescription + j);
            if (archiveDescription.contains(runJobId)) {
                TestLogger.info("Archive with description contains ("
                        + runJobId + ") exists");
                return true;
            }
        }
        return false;

    }

//    public String getOutputFromDevice(DeviceFramework routerFramework,
//            String command) throws Exception {
//
//        TestLogger.info("\nCommand: " + command);
//        TestLogger.info("-----" + command + " started-----");
//        // RouterCliCommand theCommand=new RouterCliCommand(command);
//        // theCommand.setCommands(new String []{command});
//
//        int flag = 1;
//        String output = null;
//        for (int i = 0; i < 10; i++) {
//            if (flag != 0) {
//                try {
//                    // output =
//                    // routerFramework.command().command(command,theCommand,"#");
//                    output = routerFramework.command().defaultCommand(command,
//                            command);
//                    flag = 0;
//                } catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    flag = 1;
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        TestLogger.info("\n=============\n" + output + "\n=============\n");
//        TestLogger.info("-----" + command + " finished-----");
//
//        routerFramework.getConnection().disconnect();
//        return output;
//    }
    public String[] getACLsFromDevice(NetworkDevice deivce, Dataset ds) throws Exception {

        String showAclCommand = ds.getAsString("showAclCommand");
        String deviceRunningConfig = getOutputFromDevice(deivce, showAclCommand);
        deviceRunningConfig = deviceRunningConfig.replace(" --More--           ", "").trim();
        String deviceSubRunningConfig = deviceRunningConfig;
        TestLogger.info("deviceSubRunningConfig.legth(): " + deviceSubRunningConfig.length());
        if (deviceSubRunningConfig.length() == 0) {
            TestLogger.info("**No acl exists on the device**");
            return new String[0];
        } else {
            if (ds.getAsString("deviceType").equals("ASR9K")) {
                deviceSubRunningConfig = deviceRunningConfig.substring(deviceRunningConfig.indexOf('\n') + 1).trim();
                TestLogger.info("deviceRunningConfig.lenggth(): " + deviceRunningConfig.length());
                TestLogger.info("deviceSubRunningConfig.legth(): " + deviceSubRunningConfig.length());
                TestLogger.info("deviceRunningConfig:" + deviceRunningConfig + ":");
                TestLogger.info("deviceSubRunningConfig:" + deviceSubRunningConfig + ":");

                if (deviceRunningConfig.length() == deviceSubRunningConfig.length()) {
                    TestLogger.info("**No acl exists on the device**");
                    return new String[0];
                }

            }

            String[] deviceRunningConfigAsArray = deviceSubRunningConfig.split("\\r?\\n");
            return deviceRunningConfigAsArray;
        }
    }

    public void deleteACLsOnDevice(NetworkDevice device, Dataset ds) throws Exception {

        String[] arrayOfACLs = getACLsFromDevice(device, ds);
        TestLogger.info("  arrayOfACLs.length : " + arrayOfACLs.length);

        if (arrayOfACLs.length > 0) {
            TestLogger.info("ds.getAsString(deviceType:" + ds.getAsString("deviceType"));
            for (int i = 0; i < arrayOfACLs.length; i++) {

                if (ds.getAsString("deviceType").equals("ASR9K")) {
                    // Delete ACL
                    String deleteACLCommand = "no " + arrayOfACLs[i];
                    TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                    String[] commands = new String[]{"configure t", deleteACLCommand, "commit", "exit"};
                    getOutputFromDevice(device, commands);
                } else if (ds.getAsString("deviceType").equals("Nexus7K")) {
                    if (arrayOfACLs[i].contains("access-list acl") && !arrayOfACLs[i].contains("#")) {
                        TestLogger.info("arrayOfACLs[" + i + "]: " + arrayOfACLs[i]);
                        // Delete ACL
                        String deleteACLCommand = "no " + arrayOfACLs[i];
                        TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                        String[] commands = new String[]{"configure t", deleteACLCommand, "exit", "copy r st"};
                        getOutputFromDevice(device, commands);
                    }
                } else if (ds.getAsString("deviceType").equals("WLC")) {
                    if (arrayOfACLs[i].contains("acl_")) {

                        String aclName = arrayOfACLs[i].split(" ")[0];
                        String deleteACLCommand = "acl delete " + aclName;
                        TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                        String[] commands = new String[]{"config", deleteACLCommand, "exit"};
                        getOutputFromDevice(device, commands);
                    }
                } else if (ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv")) {
                    if (arrayOfACLs[i].contains("access-list acl_") && !arrayOfACLs[i].contains("#")) {
                        TestLogger.info("arrayOfACLs[" + i + "]: " + arrayOfACLs[i]);

//                    String aclName = arrayOfACLs[i].split(" ")[0];
                        String deleteACLCommand = "no " + arrayOfACLs[i];
                        TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                        String[] commands = new String[]{"config t", deleteACLCommand, "exit"};
                        getOutputFromDevice(device, commands);
                    }
                } else if (ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus3048")) {
                    if (arrayOfACLs[i].contains("access-list acl") && !arrayOfACLs[i].contains("#")) {
                        TestLogger.info("arrayOfACLs[" + i + "]: " + arrayOfACLs[i]);

//                    String aclName = arrayOfACLs[i].split(" ")[0];
                        String deleteACLCommand = "no " + arrayOfACLs[i];
                        TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                        String[] commands = new String[]{"config t", deleteACLCommand, "exit", "copy r st"};
                        getOutputFromDevice(device, commands);
                    }
                } else if (ds.getAsString("deviceType").equals("NAM")) {
                    if (arrayOfACLs[i].contains("app_")) {

                        String aclName = arrayOfACLs[i].substring(arrayOfACLs[i].lastIndexOf(" ") + 1);
                        String deleteACLCommand = "no application group " + aclName;
                        TestLogger.info(i + "delete app group command: " + deleteACLCommand);
                        String[] commands = new String[]{"", deleteACLCommand};
                        getOutputFromDevice(device, commands);
                    }
                } else if (ds.getAsString("deviceType").equals("SF350-48P") || ds.getAsString("deviceType").equals("SG350XG-24F") || ds.getAsString("deviceType").equals("SG550XG-24F")) {
                    arrayOfACLs[i] = arrayOfACLs[i].replace("permit any", "");
                    String deleteACLCommand = "no " + arrayOfACLs[i];
                    TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                    String[] commands = new String[]{"", deleteACLCommand};
                    getOutputFromDevice(device, commands);

                } else {
                    if (arrayOfACLs[i].contains("access") && !arrayOfACLs[i].contains("#")) {
                        String aclName = arrayOfACLs[i].substring(arrayOfACLs[i].lastIndexOf(" ") + 1);
                        String deleteACLCommand = "no ip access-list standard " + aclName;
                        TestLogger.info(i + "deletecACLCommand: " + deleteACLCommand);
                        String[] commands = new String[]{"configure t", deleteACLCommand, "exit", "wr"};
                        getOutputFromDevice(device, commands);
                    }
                }
            }
        }
    }

    public String getArchiveObjectVersion(String runJobId, List<ConfigVersion> archivesInDB) {
        String archiveDescription;
        ModularOsInstallable availableImage = null;
        for (int j = 0; j < archivesInDB.size(); j++) {
            archiveDescription = archivesInDB.get(j).getComments();
            if (archiveDescription == null) {
                continue;
            }
            TestLogger.info("\n" + j + archiveDescription + j);
            if (archiveDescription.contains(runJobId)) {
                TestLogger.info("Archive with description contains (" + runJobId + ") exists");
                return archivesInDB.get(j).getInstanceId() + "";
            }
        }
        return null;

    }

    public ConfigArchiveRollbackJobDTO rollbackArchive(String versionId, boolean overwrite, boolean archiveBeforeRollback) throws IfmConfigArchiveException {

        JobDto baseJobDto = getJobDto("Config-Archive-Rollback");

        ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO = new ConfigArchiveRollbackJobDTO();

        configArchiveRollbackJobDTO.setArchiveBeforeRollback(archiveBeforeRollback);
        configArchiveRollbackJobDTO.setBaseJobDto(baseJobDto);
        configArchiveRollbackJobDTO.setConfigRollbackFileType(ConfigRollbackFileType.ALL);
        configArchiveRollbackJobDTO.setContinueRollbackOnArchiveFailure(false);
        configArchiveRollbackJobDTO.setJobType("Configuration Rollback");
        configArchiveRollbackJobDTO.setOverrideRollback(overwrite);
        configArchiveRollbackJobDTO.setRebootRequired(false);
        configArchiveRollbackJobDTO.setVersionIds(versionId);
        configArchiveRollbackJobDTO.setWriteBackToStartupConfig(false);

        String response = configArchiveRollbackRestIntf.scheduleRollback(configArchiveRollbackJobDTO);
        TestLogger.info("response is: " + response);
        return configArchiveRollbackJobDTO;
    }

    /**
     * *
     * This method will get XmpUser object from users table
     *
     * @param persistenceFactory
     * @param persistenceFactory
     * @return xmpuser
     */
    public XmpUser getXMPUser(PersistenceFactory persistenceFactory,
            String username) {
        TestLogger
                .info("--------------- appHelper getXMPUser method started ---------------");
        XmpUser xmpuser = null;
        Session session = getSessionFactory(persistenceFactory).openSession();
        TestLogger.info("session object :" + session);
        long startTime = System.currentTimeMillis();

        Criteria criteria = session
                .createCriteria(com.cisco.xmp.usermgmt.model.XmpUser.class);
        TestLogger.info("Setting Restrictions to  criteria");
        // criteria.add(Restrictions.eq("username","root"));
        criteria.add(Restrictions.eq("username", username));
        List list = criteria.list();
        TestLogger.info("Number Of Records :" + list);
        if (list != null && list.size() >= 1) {
            xmpuser = (XmpUser) list.get(0);
            TestLogger.info("XmpUser object :" + xmpuser);
            TestLogger.info("Time taken to query USERS : "
                    + (System.currentTimeMillis() - startTime));
            session.close();
            TestLogger.info("session object closed: " + session);
            TestLogger
                    .info("after session closing persistenceFactory object : "
                            + persistenceFactory);
        } else {
            Assert.fail("criteria.list is empty.. testcase failed");
        }
        TestLogger
                .info("--------------- appHelper getGroupId method completed ---------------");
        return xmpuser;
    }

    /**
     * to get Session Factory
     *
     * @param persistenceFactory
     * @return
     */
    private static SessionFactory getSessionFactory(
            PersistenceFactory persistenceFactory) {
        if (sessionFactory == null) {
            sessionFactory = persistenceFactory.getSessionFactory();
        }
        return sessionFactory;
    }

    /**
     * This function will log XmpUserDetails
     *
     * @param principal
     */
    public void logXmpuserParameters(XmpUserDetails principal) {
        TestLogger
                .info("--------------- appHelper logXmpuserParameters method started ---------------");
        TestLogger.info(" _auths : " + principal.getAuths());
        TestLogger.info(" _creationTime : " + principal.getCreationTime());
        TestLogger.info("IsCredentialsNonExpired : "
                + principal.isCredentialsNonExpired());
        TestLogger.info("EmailAddr : " + principal.getEmailAddr());
        TestLogger.info("Enabled : " + principal.isEnabled());

        TestLogger.info("FirstName : " + principal.getFirstName());
        // TestLogger.info(" Id : "+principal.getId());
        TestLogger.info("LastName : " + principal.getLastName());
        TestLogger.info("Password : " + principal.getPassword());
        TestLogger.info("Username : " + principal.getUsername());

        TestLogger.info("ActiveDomain : " + principal.getActiveDomain());
        // TestLogger.info(" ActiveDomainName : "+principal.getActiveDomainName());
        // TestLogger.info("Authenticator : "+principal.getAuthenticator());
        TestLogger.info("DefaultDomain : " + principal.getDefaultDomain());
        TestLogger.info("domainToPrivilege : "
                + principal.getDomainToPrivilege());
        // TestLogger.info("RbacProvider : "+principal.getRbacProvider());
        TestLogger.info("PolicyPartition : " + principal.getPolicyPartition());
        TestLogger.info("SessionTimeOut : " + principal.getSessionTimeOut());
        TestLogger.info("SubGroups setted");
        TestLogger
                .info("--------------- appHelper logXmpuserParameters method completed ---------------");
    }

    public ConfigDeployJobDto prepareScheduleDeploy(Dataset ds, String deviceId, String configFile) {

        JobDto baseJobDto = getJobDto("Configuration-Deploy");
        ConfigDeployJobDto configDeployJobDto = new ConfigDeployJobDto();
        configDeployJobDto.setBaseJobDto(baseJobDto);
        configDeployJobDto.setConfigFile(configFile);
        configDeployJobDto.setDeployToRunning(ds.getAsBoolean("deployToRunning"));
        configDeployJobDto.setDeployToStartup(ds.getAsBoolean("deployToStartup"));
        configDeployJobDto.setDeviceId(deviceId);
        configDeployJobDto.setRebootRequired(ds.getAsBoolean("rebootRequired"));
        configDeployJobDto.setWriteMemRequired(ds.getAsBoolean("writeMemRequired"));
        configDeployJobDto.setJobType("Configuration Deploy");

        String response = null;
        try {
            response = configArchiveViewService.scheduleDeploy(configDeployJobDto);
        } catch (IfmConfigArchiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TestLogger.info("response is: " + response);

        return configDeployJobDto;
    }

    public String waitWhileRollbackJobCompleted(ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO, Dataset ds) throws InterruptedException {

        String jobName = configArchiveRollbackJobDTO.getBaseJobDto().getJobName();
        // Wait until job is COMPLETED
        while (!getJobSpecification(jobName)
                .getWorkState().equals("Completed")
                || getJobSpecification(jobName)
                .getLastRunResultState().equals("Running")) {
            Thread.sleep(1000);
        }

        // Check completed successfully
        if (!getJobSpecification(jobName)
                .getLastRunResultState().equals("Success")) {
            if (getJobSpecification(jobName)
                    .getLastRunResultState().equals("Partial-Success")) {
                if (!(ds.getAsString("deviceType").equals("IE2000") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("CAT4K") || ds.getAsString("deviceType").equals("CAT6K") || ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv") || ds.getAsString("deviceType").equals("NAM") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR2901"))) {
                    Assert.assertTrue(false,
                            "Job is not Success, it is "
                            + getJobSpecification(jobName).getLastRunResultState());
                }

            } else {
                Assert.assertTrue(false,
                        "Job is not Success, it is "
                        + getJobSpecification(jobName).getLastRunResultState());
            }

        }
        String runJobId = getJobSpecification(jobName).getLastRunJobId();
        TestLogger.info("runjobId: " + runJobId);
        TestLogger.info(getJobSpecification(jobName).getLastRunResultState());

        return runJobId;
    }

    public String waitWhileConfigSyncJobCompleted(ConfigSyncJobDto configSyncJobDTO) throws InterruptedException {

        String jobName = configSyncJobDTO.getBaseJobDto().getJobName();
        // Wait until job is COMPLETED
        while (!getJobSpecification(jobName)
                .getWorkState().equals("Completed")
                || getJobSpecification(jobName)
                .getLastRunResultState().equals("Running")) {
            Thread.sleep(1000);
        }

        // Check completed successfully
        if (!getJobSpecification(jobName)
                .getLastRunResultState().equals("Success")) {
            Assert.assertTrue(false,
                    "Job is not Success, it is "
                    + getJobSpecification(jobName).getLastRunResultState());
        }
        String runJobId = getJobSpecification(jobName).getLastRunJobId();
        TestLogger.info("runjobId: " + runJobId);
        TestLogger.info(getJobSpecification(jobName).getLastRunResultState());

        return runJobId;
    }

    public String waitWhileConfigDeployJobCompleted(ConfigDeployJobDto configDeployJobDTO, Dataset ds) throws InterruptedException {

        String jobName = configDeployJobDTO.getBaseJobDto().getJobName();
        // Wait until job is COMPLETED
        while (!getJobSpecification(jobName)
                .getWorkState().equals("Completed")
                || getJobSpecification(jobName)
                .getLastRunResultState().equals("Running")) {
            Thread.sleep(1000);
        }

        // Check completed successfully
        if (!getJobSpecification(jobName)
                .getLastRunResultState().equals("Success")) {
            if (getJobSpecification(jobName)
                    .getLastRunResultState().equals("Partial-Success")) {
                if (!(ds.getAsString("deviceType").equals("IE2000") || ds.getAsString("deviceType").equals("ISR2951") || ds.getAsString("deviceType").equals("CAT4K") || ds.getAsString("deviceType").equals("CAT6K") || ds.getAsString("deviceType").equals("ASA") || ds.getAsString("deviceType").equals("ASAv") || ds.getAsString("deviceType").equals("NAM") || ds.getAsString("deviceType").equals("Nexus5K") || ds.getAsString("deviceType").equals("Nexus7K") || ds.getAsString("deviceType").equals("CSR1000V") || ds.getAsString("deviceType").equals("ISR2901"))) {
                    Assert.assertTrue(false,
                            "Job is not Success, it is "
                            + getJobSpecification(jobName).getLastRunResultState());
                }

            } else {
                Assert.assertTrue(false,
                        "Job is not Success, it is "
                        + getJobSpecification(jobName).getLastRunResultState());
            }

        }
        String runJobId = getJobSpecification(jobName).getLastRunJobId();
        TestLogger.info("runjobId: " + runJobId);
        TestLogger.info(getJobSpecification(jobName).getLastRunResultState());

        return runJobId;
    }

//    public JobSpecObjectDto getConfigRollbackJobSpecification(
//            ConfigArchiveRollbackJobDTO configArchiveRollbackJobDTO) {
//        JobSpecListDto jobSpecListDto = jobSchedulerServiceImpl
//                .getAllJobsSpecificationByType("Configuration Rollback");
//        List<JobSpecObjectDto> jobSpecs = jobSpecListDto.getItems();
//        Iterator<JobSpecObjectDto> iterator = jobSpecs.iterator();
//        while (iterator.hasNext()) {
//
//            JobSpecObjectDto jobSpecObjectDto = (JobSpecObjectDto) iterator
//                    .next();
//
//            if (jobSpecObjectDto.getJobName().equals(
//                    configArchiveRollbackJobDTO.getBaseJobDto().getJobName())) {
//                TestLogger.info(configArchiveRollbackJobDTO.getBaseJobDto().getJobName()
//                        + " still running");
//                return jobSpecObjectDto;
//            }
//        }
//        TestLogger
//                .info("jobespec +++++++++++++++++++++++ jobspec is nullllllll");
//
//        return null;
//    }
//    public JobSpecObjectDto getConfigSyncJobSpecification(
//            ConfigSyncJobDto configDeployJobDTO) {
//        JobSpecListDto jobSpecListDto = jobSchedulerServiceImpl
//                .getAllJobsSpecificationByType("Configuration Overwrite");
//        List<JobSpecObjectDto> jobSpecs = jobSpecListDto.getItems();
//        Iterator<JobSpecObjectDto> iterator = jobSpecs.iterator();
//        while (iterator.hasNext()) {
//
//            JobSpecObjectDto jobSpecObjectDto = (JobSpecObjectDto) iterator
//                    .next();
//
//            if (jobSpecObjectDto.getJobName().equals(
//                    configDeployJobDTO.getBaseJobDto().getJobName())) {
//                TestLogger.info(configDeployJobDTO.getBaseJobDto().getJobName()
//                        + " still running");
//                return jobSpecObjectDto;
//            }
//        }
//        TestLogger
//                .info("jobespec +++++++++++++++++++++++ jobspec is null");
//
//        return null;
//    }
//    public JobSpecObjectDto getConfigDeployJobSpecification(
//            ConfigDeployJobDto configDeployJobDTO) {
//        JobSpecListDto jobSpecListDto = jobSchedulerServiceImpl
//                .getAllJobsSpecificationByType("Configuration Deploy");
//        List<JobSpecObjectDto> jobSpecs = jobSpecListDto.getItems();
//        Iterator<JobSpecObjectDto> iterator = jobSpecs.iterator();
//        while (iterator.hasNext()) {
//
//            JobSpecObjectDto jobSpecObjectDto = (JobSpecObjectDto) iterator
//                    .next();
//
//            if (jobSpecObjectDto.getJobName().equals(
//                    configDeployJobDTO.getBaseJobDto().getJobName())) {
//                TestLogger.info(configDeployJobDTO.getBaseJobDto().getJobName()
//                        + " still running");
//                return jobSpecObjectDto;
//            }
//        }
//        TestLogger
//                .info("jobespec +++++++++++++++++++++++ jobspec is nullllllll");
//
//        return null;
//    }
    public void waitWhileInitialArchiveIsCreated(String deviceNameInPrime) throws InterruptedException, DMMException {
        List<ConfigVersion> archives = (List<ConfigVersion>) getObjects("deviceName", deviceNameInPrime, ConfigVersion.class.getCanonicalName());
        boolean isInitialArchiveExist = getArchiveObject("Initial version", archives) || getArchiveObject("Archived by inventory", archives) || getArchiveObject("Archived by syslog", archives);

        int time = 120;
        int intervalTimeInSec = 5;
        while (!isInitialArchiveExist && time > 0) {
            TestLogger.info("Remaining time for wait is: " + time);
            TestLogger.info("Initial archive has not been created yet");
            Thread.sleep(intervalTimeInSec * 1000);
            archives = (List<ConfigVersion>) getObjects("deviceName", deviceNameInPrime, ConfigVersion.class.getCanonicalName());
            isInitialArchiveExist = getArchiveObject("Initial version", archives) || getArchiveObject("Archived by inventory", archives);;
            time -= intervalTimeInSec;

        }
        if (isInitialArchiveExist) {
            TestLogger.info("Initial archive has been created");
        } else {
            Assert.assertTrue(isInitialArchiveExist, "Initial archive has not been created");
        }

    }

    public void waitWhileArchiveIsCreated(String archiveDescription, String deviceNameInPrime) throws InterruptedException, DMMException {
        List<ConfigVersion> archives = (List<ConfigVersion>) getObjects("deviceName", deviceNameInPrime, ConfigVersion.class.getCanonicalName());
        boolean isArchiveExist = getArchiveObject(archiveDescription, archives);

        int time = 120;
        int intervalTimeInSec = 5;
        while (!isArchiveExist && time > 0) {
            TestLogger.info("Remaining time for wait is: " + time);
            TestLogger.info(archiveDescription + " has not been created yet");
            Thread.sleep(intervalTimeInSec * 1000);
            archives = (List<ConfigVersion>) getObjects("deviceName", deviceNameInPrime, ConfigVersion.class.getCanonicalName());
            isArchiveExist = getArchiveObject(archiveDescription, archives);
            time -= intervalTimeInSec;

        }
        if (isArchiveExist) {
            TestLogger.info("Archive has been created with description contains: " + archiveDescription);
        }

    }

    public String getOutputFromDevice(NetworkDevice device, String... command) throws Exception {
        ProtocolClient connection = NetworkDeviceConnectionFactory.getProtocolClient(device);
        String output;
        for (int i = 0; i < 5; i++) {

            try {
                connection = NetworkDeviceConnectionFactory.getProtocolClient(device);
                connection.connect();
                break;

            } catch (Exception e) {
                Thread.sleep(10000);
                TestLogger.info("-----" + i + " started-----");
                continue;
            }
        }
        output = connection.executeCLICommands(ConnectionConstants.MAX_RETRIES, ConnectionConstants.MAX_DELAY, command);

        TestLogger.info("============ Final Output Start============");
        TestLogger.info(output);
        TestLogger.info("============ Final Output End ============");
        connection.disconnect();
        return output;
    }

    public String getHandledOutput(String output) {
        String handledOutput = "";
        String[] outputAsArray = output.split("\\r?\\n");
        int length = outputAsArray.length;
        for (int i = 0; i < length; i++) {
            if (outputAsArray[i].contains("#")) {
                outputAsArray[i] = "";
            } else {
                handledOutput = handledOutput.concat(outputAsArray[i]);
                handledOutput = handledOutput.concat("\n");
            }
        }
        TestLogger.info("Start handled output");
        TestLogger.info(handledOutput);
        TestLogger.info("Finish handled output");
        return handledOutput;
    }

}
