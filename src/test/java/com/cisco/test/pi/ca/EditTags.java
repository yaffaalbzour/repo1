package com.cisco.test.pi.ca;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.Assert;
import org.testng.annotations.Test;


import com.cisco.ifm.config.archive.service.exceptions.IfmConfigArchiveException;
import com.cisco.ifm.config.archive.service.exceptions.IfmConfigArchiveLabelCreationException;
import com.cisco.ifm.config.archive.service.intf.IfmConfigArchiveService;
import com.cisco.dp.test.BaseTest;
import com.cisco.dp.test.helper.utils.EnumUtility;
import com.cisco.ifm.jobscheduler.rest.IJobSchedulerRestService;
import com.cisco.test.tea.core.dataprovider.Dataset;
import com.cisco.test.tea.log.TestLogger;
import com.cisco.xmp.model.foundation.encapsulatedFunctionality.ManagedNetworkElement;
import com.cisco.xmp.persistence.spring.intf.PersistenceService;
import com.cisco.ifm.config.archive.manager.intf.ConfigArchiveManager;
import com.cisco.ifm.config.archive.model.ConfigLabel;
import com.cisco.ifm.config.archive.model.ConfigVersion;
import com.cisco.ifm.config.archive.rest.dto.ConfigArchiveJobDto;
import com.cisco.ifm.config.archive.rest.service.intf.ConfigArchiveViewRestIntf;

import com.cisco.srtg.test.aems.helper.utils.Inventory_util;

import javax.servlet.http.HttpServletRequest;


import com.cisco.test.pi.ca.ConfigArchiveHelper;
import java.util.Random;


public class EditTags extends BaseTest {

	
	HttpServletRequest httpServletRequest;
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
	IfmConfigArchiveService ifmConfigArchiveService;

	@Override
	public void beforeAllTests() {

	}

	@Override
	public void clean() {

	}

	/**
	 * 
	 * @param ds
	 * @throws Exception
	 */
        /*
	@Test(groups = {"ISR_DP", "ASA_DP", "WLC_DP", "Edison_DP", "CAT6K_DP", "CAT4K_DP", "N7K_DP", "XR","C3750E","XE", "Katana_DP","Edison", "ISRG2-c8800", "ISRG2-c2900", "ISRG2-c8600" }, description = "Add Device to PI", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, enabled = true)
	public void addDevice(Dataset ds) throws Exception {
		
		//Set Device Name, Need in the beforeAllTests method.
		this.setDeviceName(ds.getAsString("deviceName"));
                this.setDeviceIP(this.getTestEnvironment().getNetworkDeviceByName(this.getDeviceName()).getHost().getHostAddress());
		//Set the device taken from Dataset.
		super.beforeAllTests();	
		
                this.setDeviceFramework(EnumUtility.getDeviceType(ds.getAsString("deviceType")));
                if(this.getCliCommand()==null)
                    TestLogger.info("[DeviceCliCommand] : Null");
                else
                    TestLogger.info("[DeviceCliCommand] : "+this.getCliCommand());                
                this.getCliCommand().setCommand(this.getCommand());
                TestLogger.info("[getDeviceFramework] : "+this.getDeviceFramework());
		//Add device to PI and sets the device ID.
		this.setDeviceIdentifier( getTestEnvironment().getDeviceIdentifier(this.getDeviceName()));
		
		String deviceId = this.getDeviceIdentifier();
		inventoryUtils.waitWhileDeviceIsSyncing(deviceId, 12000, 10);
//		caHelper.deleteACLsOnDevice(routerFramework);
		caHelper.deleteACLsOnDevice( this.getDeviceFramework(),this.getCliCommand(),ds);
                
                ManagedNetworkElement deviceMNE = inventoryUtils
				.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
						+ this.getDeviceIP());

		String device_MEI = deviceMNE.getInstanceId() + "";
		String deviceNameInPrime = deviceMNE.getName();
		TestLogger.info("device_MEI:  " + device_MEI);
		TestLogger.info("device_name:  " + deviceNameInPrime);

                TestLogger.info("Wait while initial archive is created");
		caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);
	}
	// Archive, then compare running confg in archive with those on the device.
	@Test(groups = {"ISR_DP", "ASA_DP", "WLC_DP", "Edison_DP", "CAT6K_DP", "CAT4K_DP", "N7K_DP", "XR","C3750E","XE", "Katana_DP","Edison", "ISRG2-c8800", "ISRG2-c2900", "ISRG2-c8600" },enabled = true, description = "edit tag", dependsOnMethods = { "addDevice" })
	public void editTag() throws Exception {

		ManagedNetworkElement deviceMNE = inventoryUtils
				.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
						+ this.getDeviceIP());

		String device_MEI = deviceMNE.getInstanceId() + "";
		int action = 1;
		String tag = "tagForlaestarchive";
	
		ConfigVersion configVersion = configArchiveManager
				.getLatestArchivedConfigVersion(device_MEI);
		long versionId = configVersion.getInstanceId();
		TestLogger.info("versionId is: " + versionId);

		Set<String> tags = new HashSet<String>();
		tags.add(tag);
		ifmConfigArchiveService.tagVersion(versionId, tags, action);

		List<ConfigLabel> allTags = (List<ConfigLabel>) caHelper.getObjects(
				ConfigLabel.NAME, tag, ConfigLabel.class.getCanonicalName());
		int size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 1, "Size does not equal 1, it's: " + size);
		deleteTagForArchive(versionId, tags);

	}
        @Test(groups = {"ISR_DP", "ASA_DP", "WLC_DP", "Edison_DP", "CAT6K_DP", "CAT4K_DP", "N7K_DP",  "XR","C3750E","XE", "Katana_DP","Edison", "ISRG2-c8800", "ISRG2-c2900", "ISRG2-c8600"},enabled = true, description = "save same tag for different archive", dataProvider = "TEATest.DataProvider", dataProviderClass = com.cisco.test.tea.core.dataprovider.TestDataProvider.class, dependsOnMethods = { "addDevice" })
	public void saveSameTagForDifferentArchive(Dataset ds) throws Exception {
		ManagedNetworkElement deviceMNE = inventoryUtils
				.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
						+ this.getDeviceIP());

		String device_MEI = deviceMNE.getInstanceId() + "";
		String deviceNameInPrime = deviceMNE.getName();
                TestLogger.info("device_MEI:  " + device_MEI);
		TestLogger.info("device_name:  " + deviceNameInPrime);
                
		TestLogger.info("Wait while initial archive is created");
		caHelper.waitWhileInitialArchiveIsCreated(deviceNameInPrime);
                
		int action = 1;
		String tag = "duplicateTag";
		// wait 30 seconds while initial archive is being created
		// Thread.sleep(30000);
		ConfigVersion configVersion = configArchiveManager
				.getLatestArchivedConfigVersion(device_MEI);
		long versionId = configVersion.getInstanceId();
		TestLogger.info("versionId is: " + versionId);

		Set<String> tags = new HashSet<String>();
		tags.add(tag);
		ifmConfigArchiveService.tagVersion(versionId, tags, action);

		List<ConfigLabel> allTags = (List<ConfigLabel>) caHelper.getObjects(
				ConfigLabel.NAME, tag, ConfigLabel.class.getCanonicalName());
		int size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 1, "Size does not equal 1, it's: " + size);

		// schedule new archive and tag it
		boolean groupLevel = false;
		Random random = new Random();
		int accListNumber = Math.abs(random.nextInt());
                String createACLCommand ="";
                String showACLCommand = "";
                  String deleteACLCommand="";
                String[] commands =null;
                
                if (ds.getAsString("deviceType").equals("ASR9K")){
                    
                  createACLCommand = "ipv4 access-list acl" + accListNumber+ " permit any";  
                  showACLCommand = "show ipv4 access-list | i " + accListNumber;
                  deleteACLCommand = "no ipv4 access-list acl" + accListNumber;
                   commands = new String[]{ "configure t", createACLCommand, "commit","exit" };
                }
		
                else  if (ds.getAsString("deviceType").equals("Nexus7K")){
                  createACLCommand = "ip access-list acl" + accListNumber;  
                  showACLCommand = "show ip access-list | i " + accListNumber;
                  deleteACLCommand = "no ip access-list acl" + accListNumber;
                  commands = new String[]{ "configure t", createACLCommand, "permit tcp any any","exit" }; 
                }
                 else if (ds.getAsString("deviceType").equals("WLC")){
                  createACLCommand = "acl create acl_" + accListNumber;  
                  showACLCommand = "show acl summary";
                  deleteACLCommand = "acl delete acl_" + accListNumber;
                  commands = new String[]{ "config", createACLCommand,"exit" };
                }
                 else if (ds.getAsString("deviceType").equals("ASA")){
                  createACLCommand = "access-list  acl_" + accListNumber+" standard permit any4";  
                  showACLCommand = "show run | i access-list acl_";
                  deleteACLCommand = "no " + createACLCommand;
                  commands = new String[]{ "config t", createACLCommand,"exit" };
                }
                else{
                    accListNumber = accListNumber%99 +1;
                  createACLCommand = "ip access-list standard " + accListNumber;  
                  showACLCommand = "show ip access-list | i " + accListNumber;
                  deleteACLCommand = "no ip access-list standard " + accListNumber;  
                   commands = new String[]{ "configure t", createACLCommand, "permit any", "end", "wr" };
                }
                
              
           caHelper.commitCommandInConfigMode(this.getDeviceFramework(), this.getCliCommand(),commands,ds);
		String output = caHelper.getOutputFromDevice(this.getDeviceFramework(),
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
		// Delete ACL
                 if (ds.getAsString("deviceType").equals("Nexus7K")){
                      commands = new String[]{ "configure t", deleteACLCommand, "exit", "copy r st" };
                 }
                 else if (ds.getAsString("deviceType").equals("ASR9K")){
                      commands = new String[]{ "configure t", deleteACLCommand, "commit","exit" };
                 }
                 else if (ds.getAsString("deviceType").equals("WLC")){
                      commands = new String[]{ "config", deleteACLCommand, "exit" };
                 }
                 else {
                       commands = new String[]{ "configure t", deleteACLCommand, "exit", "wr" };
                 }
		  caHelper.commitCommandInConfigMode(this.getDeviceFramework(), this.getCliCommand(),commands,ds);
                
		// Tag latest archive with same tag for previous one
		configVersion = configArchiveManager
				.getLatestArchivedConfigVersion(device_MEI);
		long versionId2 = configVersion.getInstanceId();
		try {
			ifmConfigArchiveService.tagVersion(versionId2, tags, action);
			Assert.assertFalse(true,
					"Shuold not save multiple archives with same tag");
		} catch (Exception e) {

			TestLogger.info("Expected exception: " + e.getMessage());
			// TODO: handle exception
		}
		
		deleteTagForArchive(versionId, tags);

	}

	   @Test(groups = {"ISR_DP", "ASA_DP", "WLC_DP", "Edison_DP", "CAT6K_DP", "CAT4K_DP", "N7K_DP",  "XR","C3750E","XE", "Katana_DP","Edison", "ISRG2-c8800", "ISRG2-c2900", "ISRG2-c8600"},enabled = true, description = "delete Tag", dependsOnMethods = { "addDevice" })
	public void deleteTag() throws Exception {

		ManagedNetworkElement deviceMNE = inventoryUtils
				.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
						+ this.getDeviceIP());

		String device_MEI = deviceMNE.getInstanceId() + "";
		int action = 1;
		String tag = "tagToBeDeleted";
		// wait 30 seconds while initial archive is being created
		// Thread.sleep(30000);
		ConfigVersion configVersion = configArchiveManager
				.getLatestArchivedConfigVersion(device_MEI);
		long versionId = configVersion.getInstanceId();
		TestLogger.info("versionId is: " + versionId);

		Set<String> tags = new HashSet<String>();
		tags.add(tag);
		ifmConfigArchiveService.tagVersion(versionId, tags, action);

		List<ConfigLabel> allTags = (List<ConfigLabel>) caHelper.getObjects(
				ConfigLabel.NAME, tag, ConfigLabel.class.getCanonicalName());
		int size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 1, "Size does not equal 1, it's: " + size);
		action = 2;
		ifmConfigArchiveService.tagVersion(versionId, tags, action);
		allTags = (List<ConfigLabel>) caHelper.getObjects(ConfigLabel.NAME,
				tag, ConfigLabel.class.getCanonicalName());
		size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 0, "Size does not equal 0, it's: " + size);

	}
	// Archive, then compare running confg in archive with those on the device.
	  @Test(groups = {"ISR_DP", "ASA_DP", "WLC_DP", "Edison_DP", "CAT6K_DP", "CAT4K_DP", "N7K_DP",  "XR","C3750E","XE", "Katana_DP","Edison", "ISRG2-c8800", "ISRG2-c2900", "ISRG2-c8600"},enabled = true, description = "add Multiple Tags", dependsOnMethods = { "addDevice" })
	public void addMultipleTags() throws Exception {

		ManagedNetworkElement deviceMNE = inventoryUtils
				.getMNEDeviceByOWNINGENTITY(this.getDeviceIdentifier() + "_"
						+ this.getDeviceIP());

		String device_MEI = deviceMNE.getInstanceId() + "";
		int action = 1;
		String tag = "multipletag1";
		String tag2 = "multipletag2";
		// wait 30 seconds while initial archive is being created
		// Thread.sleep(30000);
		ConfigVersion configVersion = configArchiveManager
				.getLatestArchivedConfigVersion(device_MEI);
		long versionId = configVersion.getInstanceId();
		TestLogger.info("versionId is: " + versionId);

		Set<String> tags = new HashSet<String>();
		tags.add(tag);
		tags.add(tag2);
			ifmConfigArchiveService.tagVersion(versionId, tags, action);

		List<ConfigLabel> allTags = (List<ConfigLabel>) caHelper.getObjects(
				ConfigLabel.NAME, tag, ConfigLabel.class.getCanonicalName());
		int size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 1, "Size does not equal 1, it's: " + size);
		
		allTags = (List<ConfigLabel>) caHelper.getObjects(
				ConfigLabel.NAME, tag2, ConfigLabel.class.getCanonicalName());
		 size = allTags.size();
		TestLogger.info("size is: " + size);
		Assert.assertEquals(size, 1, "Size does not equal 1, it's: " + size);
		deleteTagForArchive(versionId, tags);

	}
	
	public void deleteTagForArchive(long archiveVersion,Set<String> tags  ) throws IfmConfigArchiveLabelCreationException, IfmConfigArchiveException{
		ifmConfigArchiveService.tagVersion(archiveVersion, tags, 2);	
	}
*/
}
