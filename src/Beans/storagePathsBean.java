package Beans;

public class storagePathsBean {
	
	private String filePathToDo = "C:\\Users\\oneaston\\Desktop\\Automation\\TodoFiles";
	private String filePathPassedProcessedFiles= "C:\\Users\\oneaston\\Desktop\\Automation\\PassedFiles";
	private String filePathFailedProcessedFiles= "C:\\Users\\oneaston\\Desktop\\Automation\\FailedFiles";
	private String filePathIgnoredProcessedFiles = "C:\\Users\\oneaston\\Desktop\\Automation\\IgnoredFiles";
	private String filePathToDoImpFiles = "C:\\Users\\oneaston\\Desktop\\Automation\\ImpFiles";
	private String credentialsFilePath = "C:\\Users\\oneaston\\Desktop\\serverFiles\\serverCredentials\\serverCredentials.csv";
	private String driverPath = "C:\\Users\\oneaston\\Desktop\\serverFiles\\drivers\\chromedriver_win32\\chromedriver.exe";
	private String screenShotFolderPath = "C:\\Users\\oneaston\\Desktop\\Automation\\screenshot\\";

	//getters
	public String getFilePathToDo() 
	{ 
	    return filePathToDo;
	}
	
	public String getFilePathPassedProcessedFiles() 
	{ 
	    return filePathPassedProcessedFiles;
	}
	
	public String getFilePathFailedProcessedFiles() 
	{ 
	    return filePathFailedProcessedFiles;
	}
	
	public String getFilePathIgnoredProcessedFiles() 
	{ 
	    return filePathIgnoredProcessedFiles;
	}
	
	public String getFilePathToDoImpFiles() 
	{ 
	    return filePathToDoImpFiles;
	}
	
	public String getCredentialsFilePath() 
	{ 
	    return credentialsFilePath;
	}
	
	public String getDriverPath() 
	{ 
	    return driverPath;
	}
	
	public String getScreenShotFolderPath() 
	{ 
	    return screenShotFolderPath;
	}

}
