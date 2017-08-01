#!/bin/bash
mvn=/usr/local/maven/bin/mvn
#find ./ -type f -name "Configuration_File" -exec sed -i 's/10.104.119.180/'10.104.119.179'/g' "{}" \;
JavaFilesDirectory=$WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/src/test/java/com/cisco/test/pi/ca/"
DatasetFilesDirectory=$WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/src/test/resources/datasets/com.cisco.test.pi.ca/"
SuiteFilesDirectory=$WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/src/test/resources/suites/"
TestbedFilesDirectory=$WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/src/test/resources/testbeds/"

configFile=$WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/src/test/resources/Configuration_File"

TestbedFile=$TestbedFilesDirectory"localtestbed.xml"
SuiteFile=$SuiteFilesDirectory"CA_all_DevicePack_Suite.xml"

TagAll="REPLACEME-TAGS";
function PrepareDataSetFile {
        DataSetFile=$DatasetFilesDirectory""$1".xml"
        eval "$2='$DataSetFile'";
                echo "" > "$DataSetFile"
        ##Add xml header to datasetsfile
        echo '<testdata xmlns="http://www.cisco.com/tea/0.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                                                        xsi:schemaLocation="http://www.cisco.com/tea/0.1.0 http://tea/schema/dataprovider-v0_1_0.xsd">' >> "$DataSetFile"
}

function PrepareTestbedFile {
         > "$1"
        ##Add xml header to testbedfile

echo '<testbed
   xmlns="http://www.cisco.com/tea/0.1.0"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.cisco.com/tea/0.1.0 http://tea/schema/testbed-v0_1_0.xsd http://tea/schema/testbed-devices-v0_1_0.xsd">
   <driver
       http-proxy-host="proxy.esl.cisco.com"
       http-proxy-port="80"
       https-proxy-host="proxy.esl.cisco.com"
       https-proxy-port="80"
       non-proxy-hosts="*.cisco.com|10*|172*|localhost|171.69.217.*|192.168.115.*" />' >> "$1"
}


##Clean the testBedFile
sed -i  "s/<\/testbed>//g" $TestbedFile
#sed -i "s/<testSuitePath>suites\/Swim_POLARIS_CUSTOMIZED_Suite.xml<\/testSuitePath>/REPLACEME/g" $PomFile

function addGroupToTest {
		javaTestFile=`echo $1 | rev | cut -d'/' -f 1 | rev`
		javaTestFile=`echo $javaTestFile | cut -d"." -f1`
		
        javaTestFile=$JavaFilesDirectory"$javaTestFile.java";
        #echo $javaTestFile $2  $3

        sed -i  's/"'$2'_GROUP"/"'$3'","'$2'_GROUP"/g' $javaTestFile

}

function getParameter {
PAR=`echo "$1" | cut -d"=" -f2`
eval "$2='$PAR'";
}

function createDataSet {

				getParameter $5 TAG
				if [[ ("$TagAll" = *"CFD"*) ]]; then
					if [[ ("$TAG" != *"CFD"*) ]]; then
						return 0;
					fi
				fi
				getParameter $2 TestName
                
                DataSet=`echo "$1" | cut -d"=" -f2`
                DataSet_Id=`echo "$DataSet" | cut -d";" -f1`
                GROUP=`echo "$DataSet" | cut -d";" -f2`
                DEV_TYPE=`echo "$GROUP" | cut -d"_" -f1`
                DataSet_Name="$TestName"_"$DEV_TYPE"
                Command="show ip access-list | i Standard IP access list "
				if [[ "$DEV_TYPE" = *"WLC"* ]]; then
					Command="show acl summary"
				elif [[ "$DEV_TYPE" = *"NAM"* ]]; then
					Command="show application group"
				elif [[ "$DEV_TYPE" = *"Nexus"* ]]; then
					Command="show run | i 'ip access-list acl'"
				fi
                configFileName="RUNNINGCONFIG_"$DEV_TYPE".cfg"
                filePath="ftp://cisco:cisco@172.20.68.62/dynamic_CA/"$DEV_TYPE"/"
				getParameter $4 DESC
                getParameter $3 DataSetFile
                addGroupToTest $DataSetFile $TestName $GROUP

                echo "          <dataset id=\""$DataSet_Id"\" name=\""$GROUP"\" description=\""$GROUP"_"$DESC"_"$TAG"\" >" >> "$3"
                echo "                  <group name=\""$GROUP"\"/>" >> "$3"
                echo "                  <data name=\"deviceName\" value=\""$GROUP"\" />" >> "$3"
                                echo "                  <data name=\"deviceType\" value=\""$DEV_TYPE"\" />" >> "$3"

                if [ "$TestName" = "addDevice" ]; then
                echo "                  <data name=\"showAclCommand\" value=\""$Command"\" />" >> "$3"
                elif [ "$TestName" = "scheduleDeploy" ]; then
                echo "                  <data name=\"filePath\" value=\""$filePath"\"/>" >> "$3"
                echo "                  <data name=\"configFile\" value=\""$configFileName"\"/>" >> "$3"
                echo "                  <data name=\"deployToRunning\" value=\"true\"/>" >> "$3"
                echo "                  <data name=\"deployToStartup\" value=\"false\"/>" >> "$3"
                echo "                  <data name=\"rebootRequired\" value=\"false\"/>" >> "$3"
                echo "                  <data name=\"writeMemRequired\" value=\"false\"/>" >> "$3"

                elif [ "$TestName" = "scheduleDeployWithOverwriteStartupConfig" ]; then
                echo "                  <data name=\"filePath\" value=\""$filePath"\"/>" >> "$3"
                echo "                  <data name=\"configFile\" value=\""$configFileName"\"/>" >> "$3"
                echo "                  <data name=\"deployToRunning\" value=\"false\"/>" >> "$3"
                echo "                  <data name=\"deployToStartup\" value=\"true\"/>" >> "$3"
                echo "                  <data name=\"rebootRequired\" value=\"false\"/>" >> "$3"
                echo "                  <data name=\"writeMemRequired\" value=\"false\"/>" >> "$3"

                elif [ "$TestName" = "scheduleOverwrite" ]; then
                echo "                  <data name=\"isPostArchive\" value=\"false\"/>" >> "$3"

                elif [ "$TestName" = "scheduleOverwriteWithArchiveAfter" ]; then
                echo "                  <data name=\"isPostArchive\" value=\"true\"/>" >> "$3"

                fi
                echo "          </dataset>"  >> "$3"

}

function createTest     {

            BEAN="ScheduleArchive, ScheduleOverwrite , ScheduleRollback , CompareArchive , ScheduleDeploy"
        echo "  <test name=\"CA_"$1"\">" >> $2
        echo "          <parameter name=\"testBeanIds\" value=\""$BEAN"\"/>" >> $2
        echo "          <parameter name=\"parentContextId\" value=\"applicationContext-main\" />" >> $2
        echo "          <parameter name=\"contextPaths\" value=\"pi-ca-test-context.xml\" />" >> $2
        echo "          <groups>" >> $2
        echo "                  <run>" >> $2
        echo "                          <include name=\""$1"\"/>" >> $2
        echo "                  </run>" >> $2
        echo "          </groups>" >> $2
        echo "          <classes>" >> $2
        echo "                  <class name=\"com.cisco.test.tea.core.spring.SpringContextTestFactory\" />" >> $2
        echo "          </classes>" >> $2
        echo "  </test>" >> $2
}

function createDevice {
        sos=$2
        sos=`echo $sos | tr -d ' '`
        DEVICE=`echo "$sos" | cut -d"=" -f2`
        name=`echo "$DEVICE" | cut -d";" -f1`
        ip=`echo "$DEVICE" | cut -d";" -f2`
        connection_username=`echo "$DEVICE" | cut -d";" -f3`
        connection_password=`echo "$DEVICE" | cut -d";" -f4`
        enable_password=`echo "$DEVICE" | cut -d";" -f5`
        snmp_read_community=`echo "$DEVICE" | cut -d";" -f6`
        snmp_write_community=`echo "$DEVICE" | cut -d";" -f7`
        eval "$1='$name'";

        echo "
        <network-device
        host=\""$ip"\"
        os=\"IOS-XE\"
        name=\""$name"\"
        connection-username=\""$connection_username"\"
        connection-password=\""$connection_password"\"
        enable-password=\""$enable_password"\"
        timeout=\"120000\"" >> $TestbedFile
		#echo $name
		 if [[ "$name" == *"WLC"* ]]
		 then
		 echo $name
			echo " connection-prompt=\">\"">> $TestbedFile
			echo " device-protocol=\"SSH\"" >> $TestbedFile
		 else
			echo " connection-prompt=\"#\"">> $TestbedFile
			echo " device-protocol=\"telnet\"" >> $TestbedFile
		 fi        
        echo "login-type=\"Username,Password\"        
        enable-prompt=\">\"
        snmp-version=\"2c\"
        snmp-timeout=\"50000\"
        snmp-retries=\"5\"
        snmp-read-community=\""$snmp_read_community"\"
        snmp-write-community=\""$snmp_write_community"\"
        http-server=\"http\"
        http-port=\"80\"/>" >> $TestbedFile

}

function createSuite {
        SuiteFile=$1
#        sed -i 's/REPLACEME/<testSuitePath>suites\/Swim_CUSTOMIZED_Suite_'$Protocol'_'$Sour'.xml<\/testSuitePath> REPLACEME/g' $PomFile

                        ## Add xml header to Suite File.
                        echo '<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd" >' > $SuiteFile
                                                echo            "<suite name=\"NMTG__ConfigArchive__DevicePack__API__all__TestSuite\" verbose=\"10\" preserve-order=\"true\" parallel=\"tests\" thread-count=\"30\" time-out=\"7200000\">" >> $SuiteFile
}




function createServer {
        SERVER=`echo "$2" | cut -d"=" -f2`
        ip=`echo "$SERVER" | cut -d";" -f1`
        name=$ip

        application_username=`echo "$SERVER" | cut -d";" -f2`
        connection_password=`echo "$SERVER" | cut -d";" -f3`
        connection_username=`echo "$SERVER" | cut -d";" -f4`
        application_password=`echo "$SERVER" | cut -d";" -f5`
        eval "$1='$name'";

        echo "
        <tomcat-application-server
         name=\""$name"\"
         host=\""$ip"\"
         installation-location=\"/opt/CSCOlumos/server\"
         application-password=\""$application_password"\"
         application-username=\""$application_username"\"
         connection-password=\""$connection_password"\"         
		 connection-prompt=\"#\"
		 enable-prompt=\">\"
		 snmp-version=\"2c\"
		 snmp-timeout=\"50000\"
		 snmp-retries=\"5\"
         connection-protocol=\"SSH\"
         connection-username=\""$connection_username"\"
         http-port=\"80\"
         https-port=\"443\"
         ssl-enabled=\"false\"
         os=\"Linux\" />" >> $TestbedFile
}





#while read -r line
allTests=""
while IFS='' read -r line || [[ -n "$line" ]]; do

        if [ ! -z "$line" ];
        then

                if [[ $line == "==========" ]]
                then
                        echo '  </test>' >> $DATASET_FILE

                elif [[ $line == ";;" ]]
                then
                        #Add end tag for xml header to SuiteFile.
                        echo '</testdata>' >> $DATASET_FILE

                elif [[ $line == *"SERVER"* ]]
                then
                        createSuite $SuiteFile
                        PrepareTestbedFile $TestbedFile
                        createServer SERVER_NAME "$line"

                elif [[ $line == *"TestFile"* ]]
                then
                        TESTFILE=`echo "$line" | cut -d"=" -f2`
                        PrepareDataSetFile $TESTFILE DATASET_FILE

                elif [[ $line == *"TestName"* ]]
                then
                        TEST=`echo "$line" | cut -d"=" -f2`
                        TESTNAME=`echo "$TEST" | cut -d";" -f1`
                        DESC=`echo "$TEST" | cut -d";" -f2`
						TAG=`echo "$TEST" | cut -d";" -f3`
                        echo "  <test id=\"$TESTNAME\" name=\"$TESTNAME\"  description=\"$DESC\">" >> "$DATASET_FILE"
                elif [[ $line == *"Dataset"* ]]
                then
                        dataset=`echo "$line" | cut -d"=" -f2`
                        createDataSet "$line" "TESTNAME=$TESTNAME" "DATASET_FILE=$DATASET_FILE" "DESC=$DESC" "TAG=$TAG"
                elif [[ $line == "DEVICE"* ]]
                then
                        createDevice  GROUP_NAME "$line";
                        createTest $GROUP_NAME $SuiteFile;
                fi
        fi

done < "$configFile"
#Add end tag for xml header to SuiteFile.
#done < "$configFile"
##Add end tag for the testbed
echo '</testbed>' >> $TestbedFile

echo '  <listeners>' >> $SuiteFile
echo '          <listener class-name="com.cisco.test.tea.log.TestResultLoggingListener" />' >> $SuiteFile
echo '  </listeners>' >> $SuiteFile
echo '</suite>' >> $SuiteFile



##Add end tag for the test,##Add xml end tag.
#echo '  </test>' >> $EXTERNAL_DataSetFile
#echo '</testdata>' >> $EXTERNAL_DataSetFile
#sed -i 's/REPLACEME//g' $PomFile


cd $WORKSPACE"/tests/ifm_api_tests/functional_tests/CA/"
find ./ -type f -name "ScheduleArchive.java" -exec sed -i 's/1.1.1.1/'${SERVER_NAME}'/g' "{}" \;