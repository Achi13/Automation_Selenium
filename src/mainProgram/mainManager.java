package mainProgram;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.CSVReader;
//for bean classes
import Beans.storagePathsBean;
import dbConnectivity.dbConnect;
//for thread class
import thread.threadGenerator;



//porting to another computer requires changing filePath for folders toDiscard and toDo
//file path format is different for osx (/) and windows(\\)

public class mainManager {
	
	static String filePathToDo;
	static String filePathPassedProcessedFiles;
	static String filePathFailedProcessedFiles;
	static String filePathIgnoredProcessedFiles;
	static String filePathToDoImpFiles;
	static int numberOfThreads = 10; //set number of CSVs accommodated per process per minute here
	
	//start of program execution
	public static void main(String[] args) {
		assignPaths();
		File file = new File(filePathToDo);
		
		//sends toDo folder path to getCSVFile() to produce string[] with file paths of csv files continuously
		boolean cont = true;
		while(cont==true) {
			System.out.println("\nProgram is fetching files...\n");
			sleep(2000);
			initiateProcess(getCSVFiles(file));
		}
		 
	}
	
	public static String[] getCSVFiles(File location){
			
		//acquires absolute file path for all CSV files in folder
		List<File> csvHolder = new ArrayList<>();
		
		for (File files:location.listFiles()) {
			String fileName = files.toString();
			String extension = "";
		
			int i = fileName.lastIndexOf('.');
			int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		
			if (i > p) {
			    extension = fileName.substring(i+1);
			    if(extension.equals("csv")) {
				    	csvHolder.add(files);
				    }
				}
		}
		
		String[] csvFiles = new String[csvHolder.size()];
		
		
		for(int i=0; i<csvHolder.size(); i++) { //to generate only 10 threads at most
			csvFiles[i] = csvHolder.get(i).toString();
		}
		
		return csvFiles;
	}
	
	public static void assignPaths() {
		storagePathsBean storagePathsBeanJr = new storagePathsBean();
		filePathToDo = storagePathsBeanJr.getFilePathToDo();
		filePathPassedProcessedFiles = storagePathsBeanJr.getFilePathPassedProcessedFiles();
		filePathFailedProcessedFiles = storagePathsBeanJr.getFilePathFailedProcessedFiles();
		filePathIgnoredProcessedFiles = storagePathsBeanJr.getFilePathIgnoredProcessedFiles();
		filePathToDoImpFiles = storagePathsBeanJr.getFilePathToDoImpFiles();
	}
	
	public static void initiateProcess(String[] fullFilePath) {
		
		List<Thread> threadList = new ArrayList<Thread>();
		int ctr = 0;
		
		while(checkNumberOfAliveThread(threadList)&&ctr<fullFilePath.length) {
	
			int loopCount = 0;
			for(int i=ctr; loopCount<numberOfThreads&&i<fullFilePath.length; i++) {
				threadGenerator threadGeneratorJr = new threadGenerator(fullFilePath[i], filePathPassedProcessedFiles, filePathFailedProcessedFiles, filePathIgnoredProcessedFiles, filePathToDoImpFiles);
				threadGeneratorJr.start();
				threadList.add(threadGeneratorJr.t);
				sleep(1500); //give time for thread to check and update wuilogincredentials
				ctr = i;
				loopCount++;
			}
			
			ctr+=1;
			sleep(2000);
		}
		
		sleep(5000);
		System.out.println("\nThis may take a few minutes...\n");
		
		checkThreadActivity(threadList);
		
		resetLoginCredentials();
	}
	
	public static boolean checkNumberOfAliveThread(List<Thread> threadList) {
		
		int alive = 0;
		boolean tempA = false;
		
		for(int i=0; i<threadList.size(); i++) {
			if(threadList.get(i).isAlive()) {
				alive++;
			}
		}
		
		if(alive<numberOfThreads) {
			tempA = true;
		}
		
		return tempA;
	}
	
	public static void checkThreadActivity(List<Thread> threadList) {
		
		boolean tempA = false;
		
		while(tempA==false) {
			sleep(5000);
			for(int i=0; i<threadList.size(); i++) {
				if(!threadList.get(i).isAlive()) {
					threadList.remove(i);
				}
			}
			
			if(threadList.size()==0) {
				tempA = true;
			}
		}
	}
	
	public static void sleep(int n) {
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {/*nothing to do here*/}
	}

	public static void resetLoginCredentials() {
		
		//create db connection
		dbConnect dbConnectJr = new dbConnect();
		dbConnectJr.loadDriver();
		Connection con = dbConnectJr.createConnection();
				
		//no need to param for testCaseNumber since it is a global variable
		dbConnectJr.dataBaseController(con, 6, "available", null, null, null, null, 0, null, 0, null, 0); //0 is not used
		
		//close db connection
		try {
			con.close();
		} catch (SQLException e) {System.out.println("ERROR: jdbc connection failed to close in mainManager.java");}
	}
	
	public static List<String[]> readFile(String filePathLocation) {
		
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

}
 