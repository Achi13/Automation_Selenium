package thread;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.openqa.selenium.WebDriver;
import java.sql.Connection;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import Beans.columnRowStructureBean;
import dbConnectivity.dbConnect;
import mainProgram.createImpFile;
import mainProgram.createTxtShBatFile;
import mainProgram.statusDeterminer;
import mainProgram.testAutomator;

public class threadGenerator implements Runnable{
	
	public Thread t;
	private String threadName;
	private String fullFilePath;
	private String filePathPassedProcessedFiles;
	private String filePathFailedProcessedFiles;
	private String filePathIgnoredProcessedFiles;
	private String filePathToDoImpFiles;
	private String testCaseNumber;
	private String embeddedScript;
	private String[] storedValues;
	private int executionVersion;
	private int universeId;
	public Connection con;
	
 	public threadGenerator(String fullFilePath, String filePathPassedProcessedFiles, String filePathFailedProcessedFiles, String filePathIgnoredProcessedFiles, String filePathToDoImpFiles) {
	  
	  threadName = fullFilePath; //file path is also used as thread name
	  this.fullFilePath = fullFilePath;
	  this.filePathPassedProcessedFiles = filePathPassedProcessedFiles;
	  this.filePathFailedProcessedFiles = filePathFailedProcessedFiles;
	  this.filePathIgnoredProcessedFiles = filePathIgnoredProcessedFiles;
	  this.filePathToDoImpFiles = filePathToDoImpFiles;
	}
	   
	public void run() {
		
	  System.out.println(threadName+" is being processed..." );
	  beginThreadProcess(fullFilePath);
	  System.out.println(threadName + " exiting.");
	  
	}
	   
	public void start () {
      if (t == null) {
         t = new Thread (this, threadName);
         t.start ();
      }
	}
	   
	public int beginThreadProcess(String fullFilePath) {
		
		//create db connection
		dbConnect dbConnectJr = new dbConnect();
		dbConnectJr.loadDriver();
		con = dbConnectJr.createConnection();
		
		//variables
		boolean failureFlag = false;
		ResultSet rs;
				
		//sends fullFilePath of a CSV to readFile() to produce List<String[]> containing CSV data
		List<String[]> dataFile = readFile(fullFilePath);
		
		//print test
		printTest(dataFile);
		
		//determines column and row structure of current CSV file
		columnRowStructureBean columnStructureBeanJr = new columnRowStructureBean();
		columnStructureBeanJr = getColumnRowStructure(columnStructureBeanJr, dataFile);
		
		//check login credentials
		boolean isLoginAccountAvailable = checkLoginCredentials(dataFile, columnStructureBeanJr);
		
		//check if ignoreSeverity is turned on
		boolean isIgnoreSeverity = ignoreSeverity(dataFile, columnStructureBeanJr);
		
		//get testCaseNumber of current CSV
		testCaseNumber = dataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1];
		
		//get execution_version of testCase from database
		rs = connectToDatabase(7, null, null, null, null,null,0);
		try {
			rs.absolute(1);
			executionVersion = rs.getInt("execution_version_current");
		} catch (Exception e) {e.printStackTrace();}
		
		//get universe id
		rs = connectToDatabase(10, null, null, null, null, dataFile.get(columnStructureBeanJr.getRowClientName())[1],0);
		try {
			rs.absolute(1);
			universeId = rs.getInt("universe_id");
		} catch (Exception e) {e.printStackTrace();}
		
		//get embedded script of current testcase
		rs = connectToDatabase(11, null, null, null, null, null,0);
		try {
			rs.absolute(1);
			embeddedScript = rs.getString("embedded_script");
		} catch (Exception e) {}
		
		//get stored values
		rs = connectToDatabase(15, null, null, null, null, null,0);
		try {
			rs.absolute(1);
			String tempA = rs.getString("stored_values");
			storedValues = tempA.split(",");
		} catch (Exception e) {/**/}
		
		//checks if dataFile's testCaseStatus is passed (processed) or null/failed (ignored)
		//Only CSVs with failed/null/ignored testCaseStatus will be given a test variable and be processed
		boolean isPassed = testCaseStatusChecker(columnStructureBeanJr, dataFile);
		
		//checks if client name is existing in record
		/*boolean isClientExisting = checkIfClientNameExisting(dataFile, columnStructureBeanJr);*/
		
		if(!isPassed) {
			
			if(isLoginAccountAvailable) { //nothing is done if isLoginAccountAvailable is false. dataFile will stay inside the to do folder.
				
				failureFlag = updateCsvFile(columnStructureBeanJr,fullFilePath,evaluateDataRows(columnStructureBeanJr,dataFile, isIgnoreSeverity));
			
			}else {
				String tempA = fullFilePath+" processing on hold";
				System.out.println("\n" + tempA);
				return 0; //just used for returning to avoid updating status in db
			}
			
		}else{
			updateCsvFile(columnStructureBeanJr,fullFilePath,ignoredFileStatusUpdate(columnStructureBeanJr, dataFile));
			connectToDatabase(1,"Ignored",null,null,null,null,0);
			String tempA = fullFilePath+" processing ignored";
			System.out.println("\n" + tempA);
			return 0; //0 just used for returning
		}
		
		//informs cj with processing result
		//updates database with results
		if(!failureFlag) {
			connectToDatabase(1,"Passed",null,null,null,null,0);
			String tempA = fullFilePath+" processing successful";
			System.out.println("\n" + tempA);
		}else {
			connectToDatabase(1,"Failed",null,null,null,null,0);
			String tempA = fullFilePath+" processing failed";
			System.out.println("\n" + tempA);
		}
		
		//close db connection
		try {
			con.close();
		} catch (SQLException e) {System.out.println("ERROR: jdbc connection failed to close in threadGenerator.java");}
		
		return 0; //0 just used for returning
	}
	
	public ResultSet connectToDatabase(int serviceType, String status, LinkedHashMap<String, Object> perRowData, LinkedHashMap<String, Object> footer, String loginAccountId, String clientName, long scriptId){
		
		//no need to param for testCaseNumber since it is a global variable
		dbConnect dbConnectJr = new dbConnect();
		ResultSet rs = dbConnectJr.dataBaseController(con, serviceType, status, testCaseNumber, perRowData, footer, loginAccountId, executionVersion,null,0, clientName, scriptId); //0 is not used
		
		return rs;
	}
		
	public boolean testCaseStatusChecker(columnRowStructureBean columnStructureBeanJr, List<String[]> dataFile){
			
		//check testCaseStatus of current CSV file
		String[] tempStringArray = dataFile.get(columnStructureBeanJr.getRowTestCaseStatus());
		String tempA = tempStringArray[1];
		if(tempA.contentEquals("Passed")) {
				return true;
			}
			else {
				return false;
			}
		
	}
	
	public List<String[]> readFile(String filePathLocation) {
		 
		//reads files from current CSV file and returns List<String[]> containing data
		List<String[]> dataFile = new ArrayList<>();
		String[] tempA;
		
		try {
			FileReader fileReader = new FileReader(filePathLocation);
			CSVReader csvReader = new CSVReader(fileReader); 
			while((tempA = csvReader.readNext())!=null) {
				dataFile.add(tempA);
			}
			csvReader.close();
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		return dataFile;
	}
	
	public List<String[]> evaluateDataRows(columnRowStructureBean columnStructureBeanJr, List<String[]> dataFile, boolean ignoreSeverity) {
		
		boolean failureFlag = false;
		boolean isServerImport = false;
		String tapImportStatus = null;
			
		//extract URL from current CSV file
		String webSite = getWebsitePath(columnStructureBeanJr, dataFile);
		
		//generate web browser driver for current 
		testAutomator testAutomatorJr = new testAutomator();
		WebDriver driver = testAutomatorJr.initiateDriver(webSite);
		
		//storage for the processed data from current CSV
		List<String[]> newDataFile = new ArrayList<>();
		
		//storage for footer
		LinkedHashMap<String, Object> footer = new LinkedHashMap<String, Object>();

		for(int i=0;i<dataFile.size();i++) {
			
			//for first row of data containing column headers
			if(i==0){
				String[] perRowData = dataFile.get(i);
				newDataFile.add(perRowData);
			}
			
			//for row of data containing website path 
			else if(i==columnStructureBeanJr.getRowWebsite()){
				newDataFile.add(dataFile.get(i));
				footer.put("Website", dataFile.get(i)[1]);
			}
			
			//for row of data containing testcase number
			else if(i==columnStructureBeanJr.getRowTestCaseNumber()) {
				newDataFile.add(dataFile.get(i));
				footer.put("TestCaseNumber", dataFile.get(i)[1]);
			}
			
			//for row of data containing testcase status
			else if(i==columnStructureBeanJr.getRowTestCaseStatus()) {
				String[] perRowData = dataFile.get(i);
				statusDeterminer statusDeterminerJr = new statusDeterminer();
				perRowData = statusDeterminerJr.determineStatus(failureFlag, perRowData);
				newDataFile.add(perRowData);
				footer.put("TestCaseStatus", dataFile.get(i)[1]);
			}
			
			//for row of data containing client name
			else if(i==columnStructureBeanJr.getRowClientName()){
				newDataFile.add(dataFile.get(i));
				footer.put("ClientName", dataFile.get(i)[1]);
			}
			
			//for row of data containing transaction type
			else if(i==columnStructureBeanJr.getRowTransactionType()){
				newDataFile.add(dataFile.get(i));
				footer.put("TransactionType", dataFile.get(i)[1]);
			}
			
			//for row of data containing Server Import Boolean
			else if(i==columnStructureBeanJr.getRowServerImport()){
				newDataFile.add(dataFile.get(i));
				boolean dbValue = changeOnOffIntoTrueFalse(dataFile.get(i)[1]);
				isServerImport = dbValue;
				footer.put("ServerImport", dbValue);
			}
			
			//for row of data containing sender
			else if(i==columnStructureBeanJr.getRowSender()){
				newDataFile.add(dataFile.get(i));
				footer.put("Sender", dataFile.get(i)[1]);
			}
			
			//for row of data containing ignore severity
			else if(i==columnStructureBeanJr.getRowIgnoreSeverity()){
				newDataFile.add(dataFile.get(i));
				boolean dbValue = changeOnOffIntoTrueFalse(dataFile.get(i)[1]);
				footer.put("IgnoreSeverity", dbValue);
			}
			
			//for row of data containing assigned login account
			else if(i==columnStructureBeanJr.getRowAssignedAccount()){
				newDataFile.add(dataFile.get(i));
				footer.put("AssignedAccount", dataFile.get(i)[1]);
			}
			
			//for row of data containing tap import status
			else if(i==columnStructureBeanJr.getRowTapImportStatus()){
				newDataFile.add(dataFile.get(i));
				tapImportStatus = dataFile.get(i)[1];
				footer.put("TapImportStatus", dataFile.get(i)[1]);
			}
			
			//for rows containing actual test case data
			else{
				String[] perRowData = dataFile.get(i);
				perRowData = testAutomatorJr.startAutomation(columnStructureBeanJr, driver, perRowData,ignoreSeverity, universeId);
				failureFlag = testAutomatorJr.failedAlertFrontChecker(columnStructureBeanJr, perRowData);
				newDataFile.add(perRowData);
				//insert perRowData into hashMap
				LinkedHashMap<String, Object> actualData = insertActualDataInMap(perRowData, columnStructureBeanJr);
				//insert perRowData into database
				connectToDatabase(2,null, actualData,null, null,null,0); 
			}
		}
	
		//checks testCaseStatus then generates IMP files for "passed"
		ifCreateImpTxtBatchFile(columnStructureBeanJr, newDataFile, driver, failureFlag, isServerImport, tapImportStatus); 
		
		//execute embedded_script
		getEmbeddedScript();
		
		//click logout button if it exists
		testAutomatorJr.logOutWebsite(driver);
		
		/*WARNING: TURN ON AFTER TESTING!!!!!*/
		driver.close();  
		
		//insert footer of dataFile into database
		connectToDatabase(3, null, null, footer,null,null,0);
		
		return newDataFile;
	}
	
	public LinkedHashMap<String, Object> insertActualDataInMap(String[] perRowData, columnRowStructureBean columnStructureBeanJr) {
		
		//storage for actual data
		LinkedHashMap<String, Object> actualData = new LinkedHashMap<String, Object>();
		
		//change string values to boolean
		boolean isScreenCapture = changeOnOffIntoTrueFalse(perRowData[columnStructureBeanJr.getColScreenCapture()]); 
		boolean isTriggerEnter = changeOnOffIntoTrueFalse(perRowData[columnStructureBeanJr.getColTriggerEnter()]);
		
		//put perRowData contents into map
		actualData.put("WebElementName", perRowData[columnStructureBeanJr.getColWebElementName()]);
		actualData.put("WebElementNature", perRowData[columnStructureBeanJr.getColWebElementNature()]);
		actualData.put("TriggerEnter", isTriggerEnter);
		actualData.put("TimeStamp", perRowData[columnStructureBeanJr.getColTimeStamp()]);
		actualData.put("ScreenCapture", isScreenCapture);
		actualData.put("ScPath", perRowData[columnStructureBeanJr.getColScPath()]);
		actualData.put("Remarks", perRowData[columnStructureBeanJr.getColRemarks()]);
		actualData.put("NatureOfAction", perRowData[columnStructureBeanJr.getColNatureOfAction()]);
		actualData.put("LogField", perRowData[columnStructureBeanJr.getColLogField()]);
		actualData.put("Label", perRowData[columnStructureBeanJr.getColLabel()]);
		actualData.put("InputOutputValue", perRowData[columnStructureBeanJr.getColInputOutputValue()]);
		
		return actualData;
	}
	
	public columnRowStructureBean getColumnRowStructure(columnRowStructureBean columnStructureBeanJr, List<String[]> dataFile) {
		
		String[] tempStringArrayA = dataFile.get(0);
		
		for(int i=0;i<tempStringArrayA.length;i++) {
			if(tempStringArrayA[i].toLowerCase().contains("webelementname")) {
			columnStructureBeanJr.setColWebElementName(i);
			//System.out.println("webname " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("webelementnature")) {
			columnStructureBeanJr.setColWebElementNature(i);
			//System.out.println("webnature " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("natureofaction")) {
			columnStructureBeanJr.setColNatureOfAction(i);
			//System.out.println("natureofaction" + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("inputoutputvalue")) {
			columnStructureBeanJr.setColInputOutputValue(i);
			//System.out.println("iovalue " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("remarks")) {
			columnStructureBeanJr.setColRemarks(i);
			//System.out.println("remarks " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("logfield")) {
			columnStructureBeanJr.setColLogField(i);
			//System.out.println("logfield " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("label")) {
			columnStructureBeanJr.setColLabel(i);
			//System.out.println("notes " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("triggerenter")) {
			columnStructureBeanJr.setColTriggerEnter(i);
			//System.out.println("notes " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("screencapture")) {
			columnStructureBeanJr.setColScreenCapture(i); 
			//System.out.println("screencapture: " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("timestamp")) {
			columnStructureBeanJr.setColTimeStamp(i);
			//System.out.println("timeStamp: " + i);
		}else if(tempStringArrayA[i].toLowerCase().contains("scpath")) {
			columnStructureBeanJr.setColScPath(i);
			//System.out.println("scPath: " + i);
		}
	}
	
	for(int i=dataFile.size()-1; i>=0;i--) {
		for(int x=0;x<1;x++){
			if(dataFile.get(i)[x].toLowerCase().contains("website")){
				columnStructureBeanJr.setRowWebsite(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("testcasenumber")){
				columnStructureBeanJr.setRowTestCaseNumber(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("testcasestatus")){
				columnStructureBeanJr.setRowTestCaseStatus(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("clientname")){
				columnStructureBeanJr.setRowClientName(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("transactiontype")){
				columnStructureBeanJr.setRowTransactionType(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("serverimport")){
				columnStructureBeanJr.setRowServerImport(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("sender")){
				columnStructureBeanJr.setRowSender(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("ignoreseverity")){
				columnStructureBeanJr.setRowIgnoreSeverity(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("assignedaccount")){
				columnStructureBeanJr.setRowAssignedAccount(i);
			}else if(dataFile.get(i)[x].toLowerCase().contains("tapimportstatus")){
				columnStructureBeanJr.setRowTapImportStatus(i);
			}
		}
	}
		
		return columnStructureBeanJr;
	}
	
	public String getWebsitePath(columnRowStructureBean columnStructureBeanJr, List<String[]> dataFile) {
		//extract URL from current CSV file
		String tempA;
		String[] tempStringArray = dataFile.get(columnStructureBeanJr.getRowWebsite());
		tempA = tempStringArray[1];
		return tempA;
	}
	
	public boolean updateCsvFile(columnRowStructureBean columnStructureBeanJr, String fullFilePath, List<String[]> newDataFile) {
		
		//delete CSV file from toDo folder
		File oldFile = new File(fullFilePath); 
		oldFile.delete();
		
		//Creates a new CSV file in passed/failed/Ignored ProcessedFiles containing newly processed data 
		String tempA;
		if(newDataFile.get(columnStructureBeanJr.getRowTestCaseStatus())[1].toLowerCase().contains("passed")){
			tempA = filePathPassedProcessedFiles+"/"+newDataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1]+".csv";
		}else if(newDataFile.get(columnStructureBeanJr.getRowTestCaseStatus())[1].toLowerCase().contains("failed")) {
			tempA = filePathFailedProcessedFiles+"/"+newDataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1]+".csv";
		}else {
			tempA = filePathIgnoredProcessedFiles+"/"+newDataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1]+".csv";
		}
		    
	    File newFile = new File(tempA);
	  
	    try { 
	        FileWriter outputfile = new FileWriter(newFile); 
	        CSVWriter writer = new CSVWriter(outputfile); 
	        writer.writeAll(newDataFile); 
	        writer.close(); 
	    } 
	    catch (IOException e) { 
	        e.printStackTrace(); 
	    }
		    
		//determine status and bring it back to caller method
		String tempB = newDataFile.get(columnStructureBeanJr.getRowTestCaseStatus())[1];
		if(tempB.toLowerCase().equals("passed")) {
		    	return false;
		}
		else {
		    	return true;
		}
	}
	
	public void ifCreateImpTxtBatchFile(columnRowStructureBean columnStructureBeanJr, List<String[]> newDataFile, WebDriver driver, boolean failureFlag, boolean isServerImport, String tapImportStatus) {
		
		if(!failureFlag&&isServerImport) {
				
			//generates IMP file if testCaseStatus of CSV file after being processed is "passed"
			createImpFile createImpFileJr = new createImpFile();
			String filePathOfNewImp = createImpFileJr.impController(testCaseNumber, driver, filePathToDoImpFiles, tapImportStatus);
			
			//generates BAT file if testCaseStatus of CSV file after being processed is "passed" and IMP file has been generated
			//if filePath of IMP is <2, BAT and other files will not be generated
			//<2 means IMP was not generated successfully
			if(filePathOfNewImp.length()>1) {
				createTxtShBatFile createBatFileJr = new createTxtShBatFile();
				createBatFileJr.txtBatShController(con, testCaseNumber, newDataFile, columnStructureBeanJr, filePathToDoImpFiles, filePathOfNewImp);
			}
			
		}
	}
	
	public List<String[]> ignoredFileStatusUpdate(columnRowStructureBean columnStructureBeanJr, List<String[]> dataFile){
		
		dataFile.get(columnStructureBeanJr.getRowTestCaseStatus())[1] = "Ignored";
		return dataFile;
	}
	
	/*
	public boolean checkIfClientNameExisting(List<String[]> dataFile, columnRowStructureBean columnStructureBeanJr){
		
		serverCredentials serverCredentialsJr = new serverCredentials();
		return(serverCredentialsJr.isClientNameExisting(dataFile, columnStructureBeanJr));
		
	}*/

	public boolean ignoreSeverity(List<String[]> dataFile, columnRowStructureBean columnStructureBeanJr) {
		
		String tempA = dataFile.get(columnStructureBeanJr.getRowIgnoreSeverity())[1];
		
		boolean tempB;
		
		if(tempA.toLowerCase().equals("on")) {
			tempB = true;
		}else {
			tempB = false;
		}
		
		return tempB;
	}

	public boolean checkLoginCredentials(List<String[]> dataFile, columnRowStructureBean columnStructureBeanJr) {
		
		//List<String[]> tempDataFile = readFile(filePathWuiLoginCredentials);
		
		String loginAccountId = dataFile.get(columnStructureBeanJr.getRowAssignedAccount())[1]; //gets assigned account id from current testcase
		ResultSet rs = connectToDatabase(4, null, null,null, loginAccountId,null,0);
		String loginAccountIdStatus = "";
		boolean tempA = true;
		
		try {
			while(rs.next()) {
				loginAccountIdStatus = rs.getString("status");
				break;
			}
		} catch (SQLException e) {e.printStackTrace();}
		
		if(loginAccountIdStatus.equalsIgnoreCase("available")) {
			//make status "unavailable" before returning true
			connectToDatabase(5, "unavailable", null,null, loginAccountId,null,0);
		}else if(loginAccountIdStatus.equalsIgnoreCase("unavailable")) {
			tempA = false;
		}
		
		return tempA;
	}
	
	public void updateCsvFile1(columnRowStructureBean columnStructureBeanJr, String fullFilePath, List<String[]> tempDataFile) {
		
		//update status of wui login credential
		
		File newFile = new File(fullFilePath);
		  
	    try { 
	        FileWriter outputfile = new FileWriter(newFile); 
	        CSVWriter writer = new CSVWriter(outputfile); 
	        writer.writeAll(tempDataFile); 
	        writer.close(); 
	    } 
	    catch (IOException e) { 
	        e.printStackTrace(); 
	    }
	}

	public boolean changeOnOffIntoTrueFalse(String onOff) {
		
		boolean tempA;
		
		if(onOff.equalsIgnoreCase("on") || onOff.equalsIgnoreCase("true")){
			tempA = true;
		}else{
			tempA = false;
		}
		
		return tempA;
		
	}

	public void getEmbeddedScript() {
		
		System.out.println("Executing embedded script for "+testCaseNumber);
		
		List<String> scriptId = new ArrayList<String>();
		
		//check if embedded script string is empty
		if(embeddedScript!=null && !embeddedScript.isEmpty()) {
			
			System.out.println("Embedded Script : "+embeddedScript);
			//if string contains multiple entries
			if(embeddedScript.contains(",")) {
				
				//split by comma to get different script id
				String[] embeddedScriptArray =  embeddedScript.split(",");
				for(int i=0; i<embeddedScriptArray.length; i++) {
					scriptId.add(embeddedScriptArray[i]);
				}
			}
			
			//if string only has a single entry
			else {
				scriptId.add(embeddedScript);
			}
			
			//execute Script
			executeEmbeddedScript(scriptId);
		}
	}
	
	public void executeEmbeddedScript(List<String> scriptId) {
		
		//assigns variable to correct script
		int variableStartIndex = 0;
		
		//determine number of script_variable with current script_id
		int numberOfRequiredVariables = 0;
		
		for(String iterator: scriptId) {
			
			//tempVar
			long loginAccountId = 0;
			//needs for bat file
			String scriptFilePath = null;
			String hostName = null;
			String userName = null;
			String password = null;
			String ppkFilePath = null;
			String scriptName = null;
			
			//query for script using script id
			ResultSet rs = connectToDatabase(12, null, null, null,null,null,Integer.parseInt(iterator));
			
			//get results from rs
			try {
				rs.first();
				loginAccountId = rs.getLong("login_account_id");
				scriptFilePath = rs.getString("script_filepath");
				scriptName = rs.getString("name");
			}catch(Exception e) {/*nothing to do here*/ e.printStackTrace();}
			
			//get login_account information of script using loginAccountId
			rs = connectToDatabase(13, null, null, null, Long.toString(loginAccountId), null, 0);
			
			//get results from rs
			try {
				rs.first();
				hostName = rs.getString("hostname");
				userName = rs.getString("username");
				password = rs.getString("password");
				ppkFilePath = rs.getString("ppk_filepath");
			}catch(Exception e) {/*nothing to do here*/ e.printStackTrace();}
			
			//query for script_variable connected with current script_id
			rs = connectToDatabase(14, null, null, null,null,null,Integer.parseInt(iterator));
			
			try {
				if(rs.last()) {
					numberOfRequiredVariables = rs.getRow();
				}
			} catch (SQLException e) {/*nothing to do here*/ e.printStackTrace();}
			
			//generate mput sh text file
			File mputFile = generateTxtFile(1, scriptFilePath, scriptName, 0, 0);
			
			//generate cmd text file to run sh file inside server
			File cmdFile = generateTxtFile(2, null, scriptName, variableStartIndex, numberOfRequiredVariables);
			
			//generate del file to delete sh from server
			File delFile = generateTxtFile(3, null, scriptName, 0, 0);
			
			//generateBatFile
			String tempBatFileLocationWithName = filePathToDoImpFiles+"\\"+testCaseNumber+"_"+scriptName+"_embedded_script.bat";
			File batFile = generateBatFile(tempBatFileLocationWithName, userName, password, hostName, ppkFilePath, scriptFilePath, mputFile, cmdFile, delFile);
			
			//executeBatFile
			createTxtShBatFile createTxtShBatFileJr = new createTxtShBatFile();
			createTxtShBatFileJr.executeBatFile(tempBatFileLocationWithName, testCaseNumber);
			
			//delete Bat file after execution
			mputFile.delete();
			cmdFile.delete();
			delFile.delete();
			batFile.delete();
					
			//assign new starting index for next script's variable acquisition
			variableStartIndex += numberOfRequiredVariables;
		}
		
	}
	
	public File generateBatFile(String batFilePath, String username, String password, String hostname, String ppkFilePath, String scriptFilePath, File mputFile, File cmdFile, File delFile) { 
		
		String batContent = "";
		String NL = "\n";
		
		//create batFile credential contents
		
		for(int i=0; i<3; i++) {
			
			if(i==0) {
				
				batContent = "psftp " + hostname+" -l "+username+" -pw "+password;
				
				if(ppkFilePath!=null&&!ppkFilePath.isEmpty()) {
					batContent = batContent + " -i "  + ppkFilePath + " -b " + mputFile.getAbsoluteFile();
				}else {
					batContent = batContent + " -b " + mputFile.getAbsoluteFile();
				}
				
			}
			
			else if(i==1) {
				
				batContent = batContent+ NL + "plink.exe -ssh " + hostname+" -l "+username+" -pw "+password;
				if(ppkFilePath!=null&&!ppkFilePath.isEmpty()) {
					batContent = batContent + " -i "  + ppkFilePath + " -m " + cmdFile.getAbsolutePath();
				}else {
					batContent = batContent + " -m " + cmdFile.getAbsolutePath();
				}
			}
			
			else {
				
				batContent = batContent+ NL +"psftp " + hostname+" -l "+username+" -pw "+password;
				
				if(ppkFilePath!=null&&!ppkFilePath.isEmpty()) {
					batContent = batContent + " -i "  + ppkFilePath + " -b " + delFile.getAbsoluteFile();
				}else {
					batContent = batContent + " -b " + delFile.getAbsoluteFile();
				}
				
			}
			
		}
		
		//creates BAT file
		File batFile = new File(batFilePath);
		
		try {
			batFile.createNewFile();
			
			//writes TXT file content
			FileWriter writer2 = new FileWriter(batFile);
			writer2.write(batContent);
			writer2.close();
			
			System.out.println(testCaseNumber + "'s BAT file generation successful");
			
		}catch(Exception e) {
			System.out.println(testCaseNumber + "'s BAT file generation failed");
		}
		
		return batFile;
	}
	
	public File generateTxtFile(int contentDeterminer, String scriptFilePath, String scriptName, int variableStartIndex, int numberOfRequiredVariables ) {
		
		String fileCommand;
		String NL = "\n";
		String txtFilePath;
		File txtFile;
		
		//MPUT for 1st text file and GET/DEL for 2nd text file
		if(contentDeterminer==1) {
			
			//create path for new text file
			txtFilePath = filePathToDoImpFiles + "\\" + testCaseNumber + "_" + scriptName + "_" + "mput.txt";
			
			//creates TXTs file
			txtFile = new File(txtFilePath);
			
			fileCommand = "dir"+NL+"cd /var/tmp" + NL + "dir" + NL + "mput " + scriptFilePath + NL + "dir" + NL + "bye"; //put file inside server
		}
		
		else if(contentDeterminer==2) {
			
			//create path for new text file
			txtFilePath = filePathToDoImpFiles + "\\" + testCaseNumber + "_" + scriptName + "_" + "cmd.sh";
			
			//creates TXTs file
			txtFile = new File(txtFilePath);
			
			fileCommand = ". .bash_profile"+NL+NL+"sh /var/tmp/"+ scriptName+".sh";
			
			//create batfile variable content
			for(int i=variableStartIndex; i<variableStartIndex+numberOfRequiredVariables; i++) {
				fileCommand = fileCommand + " " + storedValues[i];
			}
			
		}
		
		else{
			
			//create path for new text file
			txtFilePath = filePathToDoImpFiles + "\\" + testCaseNumber + "_" + scriptName + "_" + "del.txt";
			
			//creates TXTs file
			txtFile = new File(txtFilePath);
			
			fileCommand = "dir"+NL+"cd /var/tmp" + NL + "dir" + NL + "del " + scriptName + ".sh" + NL + "dir" + NL + "bye";
			
		}
		
		//WRITE TXT FILE CONTENTS
		try{
			txtFile.createNewFile();
			FileWriter writer1 = new FileWriter(txtFile);
			writer1.write(fileCommand); 
			writer1.close();
			
		}catch(Exception e) {/*nothing to do here*/ e.printStackTrace();}
		
		return txtFile;
	}
	
	public void printTest(List<String[]> tempList) {
		
		for(String[] iterator: tempList) {
			for(int i =0; i<iterator.length; i++) {
				System.out.print(iterator[i]+ " || ");
			}
			System.out.println();
		}
	}
		
	
}


