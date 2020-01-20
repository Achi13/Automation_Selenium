package mainProgram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.List;

import Beans.columnRowStructureBean;

public class createTxtShBatFile {
	
	public void txtBatShController(Connection con, String testCaseNumber, List<String[]> newDataFile, columnRowStructureBean columnStructureBeanJr, String filePathToDoImpFiles, String filePathOfNewImp) {
		
		//creates filePath to be used for creating the new BAT file
		String[] filePaths = generateFilePaths(filePathToDoImpFiles, testCaseNumber); //0: bat, 1: c1 txt, 2: sh, 3: c2 txt
		
		//get server credentials. DEPENDS ON CLIENT.
		serverCredentials serverCredentialsJr = new serverCredentials();
		String[] credentialStorage = serverCredentialsJr.getCredentials(con, newDataFile, columnStructureBeanJr);
		
		//commands to be executed on BAT file
		String[] batFileCommands = generateBatFileCommands(credentialStorage, filePaths);
		
		//generates TXT file
		File txtFile1 = generateTxtFile(filePathToDoImpFiles, filePaths[1], filePathOfNewImp,1, columnStructureBeanJr, newDataFile, testCaseNumber);
		//generates TXT file
		File txtFile2 = generateTxtFile(filePathToDoImpFiles, filePaths[3], filePathOfNewImp,2,columnStructureBeanJr, newDataFile, testCaseNumber);
		//generate SH file
		File shFile = generateShFile(filePaths[2], columnStructureBeanJr, newDataFile, testCaseNumber);
		//generate BAT file
		File batFile = generateBatFile(filePaths[0], batFileCommands[0], batFileCommands[1], batFileCommands[2], testCaseNumber);
		
		//Execute batFile
		executeBatFile(filePaths[0], testCaseNumber);
			
		//deletes TXTs, BAT, and IMP local files after execution
		deleteFiles(batFile,txtFile1, txtFile2, shFile, filePathOfNewImp);
	}
	
	public String[] generateBatFileCommands(String[] credentialStorage, String[] filePaths) {
		
		String[] batFileCommands = new String[3];
		batFileCommands[0] = "psftp "+credentialStorage[0]+" -l "+credentialStorage[1] + " -pw "+credentialStorage[2]+" -i "+credentialStorage[3]+" -b "+filePaths[1];
		batFileCommands[1] = "plink.exe -ssh "+credentialStorage[0]+" -l "+credentialStorage[1] + " -pw "+credentialStorage[2]+" -i "+credentialStorage[3] + " -m " + filePaths[2];
		batFileCommands[2] = "psftp "+credentialStorage[0]+" -l "+credentialStorage[1] + " -pw "+credentialStorage[2]+" -i "+credentialStorage[3]+" -b "+filePaths[3];
		
		return batFileCommands;
	}
	
	public String[] generateFilePaths(String filePathToDoImpFiles, String testCaseNumber) {
		
		String filePaths[] = new String[4];
		
		String tempA = filePathToDoImpFiles+"\\"+testCaseNumber+".bat";
		filePaths[0] = tempA;
		String tempB_1 = filePathToDoImpFiles+"\\"+testCaseNumber+"_c1.txt";
		filePaths[1] = tempB_1;
		String tempC = filePathToDoImpFiles+"\\"+testCaseNumber+".sh";
		filePaths[2] = tempC;
		String tempB_2 = filePathToDoImpFiles+"\\"+testCaseNumber+"_c2.txt";
		filePaths[3] = tempB_2;
		
		return filePaths;
	}
	
	public void executeBatFile(String pathFileOfBat, String testCaseNumber){
		
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(pathFileOfBat);
			int exitDeterminer;
			
			//executes batFile
			Process process = processBuilder.start();
			
			//FOR TESTING
			StringBuilder output = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

			//waits until execution is finished
			exitDeterminer = process.waitFor();
			
			//testing
			if(exitDeterminer==0) {
				System.out.println("BAT FILE RESULT: " + output);
				System.out.println(testCaseNumber + "'s BAT file execution successful"); //for testing
			}
		}catch(Exception e) {
			System.out.println("BAT file execution failed"); //for testing
		}
		
	}
	
	public void deleteFiles(File batFile, File txtFile1, File txtFile2, File shFile, String filePathOfNewImp) {
		
		//re-generate impFile
		File impFile = new File(filePathOfNewImp);
		
		//delete all files after execution
		impFile.delete();
		batFile.delete();
		txtFile1.delete();
		txtFile2.delete();
		shFile.delete();
	}

	public File generateTxtFile(String filePathToDoImpFiles, String txtFilePath, String filePathOfNewImp, int contentDeterminer, columnRowStructureBean columnStructureBeanJr, List<String[]> newDataFile, String testCaseNumber) { 
		
		//creates TXTs file
		File txtFile = new File(txtFilePath);
		String fileCommand;
		
		//MPUT for 1st text file and GET/DEL for 2nd text file
		if(contentDeterminer==1) {
			fileCommand = "mput "+filePathOfNewImp; //put file inside server
		}else{
			String fileName = newDataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1];
			String tempString;
			String NL = "\n";
			tempString = "lcd "+filePathToDoImpFiles+NL; //change default get result location
			tempString = tempString+"get "+fileName+".err"+NL; //get files from server
			tempString = tempString+"del "+fileName+".err"+NL+"del "+fileName+".out"+NL+"del "+fileName+".imp"; //delete files from server
			fileCommand = tempString;
		}
		
		//WRITE TXT FILE CONTENTS
		try{
			txtFile.createNewFile();
			FileWriter writer1 = new FileWriter(txtFile);
			String NL ="\n";
			writer1.write("dir"+NL+"cd /tap/ocs/current/temp/"+NL+"dir"+NL+fileCommand+NL+"dir"+NL+"bye" ); 
			writer1.close();
			
			//console outputs depending on the txt file that was generated
			if(contentDeterminer==1) {
				System.out.println(testCaseNumber+"'s TXT file_c1 generation successful");
			}else {
				System.out.println(testCaseNumber+"'s TXT file_c2 generation successful");
			}
			
		}catch(Exception e) {
			if(contentDeterminer==1) {
				System.out.println(testCaseNumber+"'s TXT file_c1 generation failed");
			}else {
				System.out.println(testCaseNumber+"'s TXT file_c2 generation failed");
			}
		}
		
		return txtFile;
	}
	
	public File generateBatFile(String batFilePath, String command1, String command2, String command3, String testCaseNumber) { 
		
		//creates BAT file
		File batFile = new File(batFilePath);
		
		try {
			batFile.createNewFile();
			
			//writes TXT file content
			FileWriter writer2 = new FileWriter(batFile);
			String NL = "\n";
			writer2.write(command1+NL+command2+NL+command3);
			writer2.close();
			
			System.out.println(testCaseNumber + "'s BAT file generation successful");
			
		}catch(Exception e) {
			System.out.println(testCaseNumber + "'s BAT file generation failed");
		}
		
		return batFile;
	}
	
	public File generateShFile(String shFilePath, columnRowStructureBean columnStructureBeanJr, List<String[]> newDataFile, String testCaseNumber ) {
		//SH contents
		String impFileName = newDataFile.get(columnStructureBeanJr.getRowTestCaseNumber())[1];
		String sheBang = "#!/bin/ksh";
		String bashProfile = ". .bash_profile";
		String executeCommand = "$AAAHOME/aaa imp -Uaaa -Paaaaaa </tap/ocs/current/temp/"+impFileName+".imp > /tap/ocs/current/temp/"+impFileName+".out 2>/tap/ocs/current/temp/"+impFileName+".err";
		File shFile = new File(shFilePath);
		
		try {
			shFile.createNewFile();
			
			//writes SH file content
			FileWriter writer3 = new FileWriter(shFile);
			String NL = "\n";
			writer3.write(sheBang+NL+bashProfile+NL+NL+executeCommand);
			writer3.close();
			
			System.out.println(testCaseNumber + "'s SH file generation successful");
			
		}catch(Exception e) {
			System.out.println(testCaseNumber + "'s SH file generation failed");
		}
		
		return shFile;
	}
	
}
