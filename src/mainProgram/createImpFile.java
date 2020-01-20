package mainProgram;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class createImpFile {
	
	LinkedHashMap<String, String> impFileTemplate;
	
	public String impController(String testCaseNumber, WebDriver driver, String filePath, String tapImportStatus) {
		
		//Container of file path for the generated IMP file
		String[] tempValueStorage;
		String newImpFilePath = "";
		
		try {
			
			//generate IMP template
			createHashMap();
			
			//Acquires transaction code from current driver. 
			String transactionCode = getTransactionCodePath(driver);
			
			//Generates IMP values and returns the path of the IMP file
			tempValueStorage = impValues(transactionCode, tapImportStatus);
			
			//Generate IMP file
			newImpFilePath = generateImpFile(tempValueStorage, filePath, testCaseNumber);
			
		}catch(Exception e) {
			System.out.println(testCaseNumber + "'s IMP file generation failed");
		}
		
		return newImpFilePath;
	}
	
	public String getTransactionCodePath(WebDriver driver) {
		
		String finalFieldSetName = "";
		String[] fieldSetName = new String[3];
		fieldSetName[0] = "/html[1]/body[1]/div[3]/div[1]/div[2]/div[1]/fieldset[1]";
		fieldSetName[1] = "/html[1]/body[1]/div[3]/div[1]/div[2]/div[1]/table[1]/tbody[1]/tr[1]/td[2]/fieldset[1]";
		fieldSetName[2] = "/html[1]/body[1]/div[3]/div[1]/div[2]/div[1]/table[1]/tbody[1]/tr[1]/td[1]/fieldset[1]/table[1]/tbody[1]/tr[1]/td[1]";
		
		//check name of existing table that contains severity levels
		for(int i=0; i<fieldSetName.length;i++) {
			try {
				driver.findElement(By.xpath(fieldSetName[i]));
				finalFieldSetName = fieldSetName[i];
				break;
			}catch(Exception e) {/*nothing to do here*/}
		}
		
		WebElement fieldSet = driver.findElement(By.xpath(finalFieldSetName));
		WebElement table = fieldSet.findElement(By.tagName("table"));
		WebElement tbody = table.findElement(By.tagName("tbody"));
		List<WebElement> rowData = tbody.findElements(By.tagName("tr"));
		String tempA="temporaryString";
		
		for (WebElement row : rowData) {
	        List<WebElement> cells = row.findElements(By.tagName("td"));
	        here:
	        for (int i=0;i<cells.size();i++) {
	        	try{
	        		if(cells.get(i).findElement(By.tagName("label")).getText().toLowerCase().equals("transaction code")) {
	        			tempA = cells.get(i+1).getText();
	        			System.out.println("Transaction Code: " + tempA);
	        			return tempA;
	        		}
	        	}catch(Exception e) {
	        		continue here;
	        	}
	            
	        }
	    }
		return tempA;
	}
	
	public String[] impValues(String transactionCode, String tapImportStatus) {
		String[] impFileValues = new String[impFileTemplate.size()];
		
		//Assign values to imp template
		for(int i=0;i<impFileValues.length;i++) {
			if(i==0) {
				impFileValues[i]= "";
			}else if(i==1) {
				impFileValues[i]= "DELIMITED";
			}else if(i==2) {
				impFileValues[i]= ";";
			}else if(i==3) {
				impFileValues[i]= ".";
			}else if(i==4) {
				impFileValues[i]= ",";
			}else if(i==5) {
				impFileValues[i]= "YYYYMMDD";
			}else if(i==6) {
				impFileValues[i]= "ON_PTF";
			}else if(i==7) {
				impFileValues[i]= "ext_operation";
			}else if(i==8) {
				impFileValues[i]= "code status_e";
			}else if(i==9) {
				impFileValues[i]= transactionCode+";"+tapImportStatus;
			}
		}
		
		return impFileValues;
	}
	
	public String generateImpFile(String[] impFileValues, String filePath, String testCaseNumber) {
		int ctr = 0;
		
		for (String i : impFileTemplate.keySet()) {
			  impFileTemplate.put(i, impFileValues[ctr]);
			  ctr++;
		}
		
		String newFilePath = filePath+"\\"+ testCaseNumber +".imp";
		File file = new File(newFilePath); 
        
		try {
        	//Create new file
			FileWriter writer = new FileWriter(file);
 
	        file.createNewFile();
	        for (String i : impFileTemplate.keySet()) {
	        	writer.write(i+impFileTemplate.get(i)+"\n");
	    	}
	        
	        System.out.println(testCaseNumber + "'s IMP file generation successful");
            writer.close();
        }catch(Exception e){
        	e.getMessage();
        }
		
		return newFilePath;
	}
	
	public void createHashMap() {
		this.impFileTemplate = new LinkedHashMap<String, String>();
		impFileTemplate.put("FLH", "");
		impFileTemplate.put("SET DATAFORMAT ", "");
		impFileTemplate.put("SET SEPARATOR ", "");
		impFileTemplate.put("SET DECIMAL ", "");
		impFileTemplate.put("SET THOUSAND ", "");
		impFileTemplate.put("SET DATEFORMAT ", "");
		impFileTemplate.put("SET AUTOMATIC_FUSION ", "");
		impFileTemplate.put("CMD UPDATE ", "");
		impFileTemplate.put("ATT ", "");
		impFileTemplate.put("DAT ", "");
	}

}
