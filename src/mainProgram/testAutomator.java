package mainProgram;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aventstack.extentreports.MediaEntityBuilder;
import Beans.columnRowStructureBean;
import Beans.storagePathsBean;


public class testAutomator {

	String driverPath;
	String screenShotFolderPath;
	String highSeverityError = "";
	boolean failedFlag = false;
	boolean ignoreSeverity;
	boolean isSoftBlockOnProcess = false;
	int universeId;
	
	public WebDriver initiateDriver(String webSite) {
		
		storagePathsBean storagePathsBeanJr = new storagePathsBean();
		driverPath = storagePathsBeanJr.getDriverPath();
		System.setProperty("webdriver.chrome.driver", driverPath );
		ChromeOptions options = new ChromeOptions();
		options.addArguments("headless");
        options.addArguments("window-size=1200x600");
        WebDriver driver = new ChromeDriver(options); //headless mode
		//WebDriver driver = new ChromeDriver(); //window pop-up mode
		getWebsite(webSite, driver);
		return driver;
	}
	
	public void getWebsite(String webSite, WebDriver driver) {
			driver.get(webSite); //make this dynamic
			driver.manage().window().maximize();
	}
	
	public String[] startAutomation(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData, boolean ignoreSeverity, int universeId) {
		
		//set ignoreSeverity value
		this.ignoreSeverity = ignoreSeverity;
		
		//set universeId value
		this.universeId = universeId;
		
		//evaluate test data if and only if failedFlag has not been set to TRUE
		if(!failedFlag) { 
			perRowData = testTypeDeterminer(columnStructureBeanJr, driver, perRowData); 
		}
		else {
			perRowData = testCasePending(columnStructureBeanJr, perRowData);
		}
	
		return perRowData;
	}
	
	public String[] testTypeDeterminer(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
		
		//check if severity exists
		checkSeverityController(driver, perRowData, columnStructureBeanJr); 
		
		//determine web element nature form current test data
		if(perRowData[columnStructureBeanJr.getColWebElementNature()].toLowerCase().equals("id")) {
			perRowData = testById(columnStructureBeanJr, driver, perRowData);
		}
		else if(perRowData[columnStructureBeanJr.getColWebElementNature()].toLowerCase().equals("class")) {
			perRowData = testByClass(columnStructureBeanJr, driver, perRowData); 
		}
		else if(perRowData[columnStructureBeanJr.getColWebElementNature()].toLowerCase().equals("name")) {
			perRowData = testByName(columnStructureBeanJr, driver, perRowData); 
		}
		else if(perRowData[columnStructureBeanJr.getColWebElementNature()].toLowerCase().equals("xpath")) {
			perRowData = testByXpath(columnStructureBeanJr, driver, perRowData); 
		}
		
		return perRowData;
	}
	
	public String[] testCasePending(columnRowStructureBean columnStructureBeanJr, String[] perRowData) {
		perRowData[columnStructureBeanJr.getColRemarks()] = "Pending";
		return perRowData;
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////MAIN LOGIC////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public String[] testById(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
		
		try {
			
			if(!explicitWait("id", driver, perRowData[columnStructureBeanJr.getColWebElementName()])) {
				
				if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("input")) {
					driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
					driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(perRowData[columnStructureBeanJr.getColInputOutputValue()]);
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("click")) {
					driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("hover")) {
					Actions action = new Actions(driver);
					WebElement btn = driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()]));
					action.moveToElement(btn).perform();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("select")) {
					WebElement selectionObject = driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()]));
					Select options = new Select(selectionObject);
					List<WebElement> optionList = options.getOptions();
					
					for(int i=0; i<optionList.size(); i++) {
						
						if(optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))) {
							optionList.get(i).click();
							break;
						}
						else if(!optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))&&i==optionList.size()-1) {
							perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 2);
							return perRowData;
						}
						
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("compare")) {
					String expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					
					//get comparing value from web element
					String tempA = driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					if(tempA.length()<=1) {
						tempA = driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).getAttribute("value");
					}
			
					if(!tempA.equalsIgnoreCase(expectedVal)) {
						perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 3);
						return perRowData;
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("datepicker")) {
					
					//get date input from current row of data and split it into year, month, and day
					String[] dateArray = dateSplitter(perRowData, columnStructureBeanJr);

					//click calendar
					driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).click(); // change this only for other types
					sleep(2000);
					
					//set year, month, and date on calendar widget
					setCalendar(driver, dateArray);
					
				}
			}
			
			perRowData = successfulTestCaseHandler(perRowData, driver, columnStructureBeanJr);
			return perRowData;
			
		}catch(Exception e) {
			perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, e, 1);
			return perRowData;
		}
	}
	
	public String[] testByClass(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
		
		try {
			
			if(!explicitWait("class", driver, perRowData[columnStructureBeanJr.getColWebElementName()])) {
				if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("input")) {
					driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
					driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(perRowData[columnStructureBeanJr.getColInputOutputValue()]);
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("click")) {
					driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("hover")) {
					Actions action = new Actions(driver);
					WebElement btn = driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()]));
					action.moveToElement(btn).perform();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("select")) {
					WebElement selectionObject = driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()]));
					Select options = new Select(selectionObject);
					List<WebElement> optionList = options.getOptions();
					
					for(int i=0; i<optionList.size(); i++) {
						
						if(optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))) {
							optionList.get(i).click();
							break;
						}
						else if(!optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))&&i==optionList.size()-1) {
							perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 2);
							return perRowData;
						}
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("compare")) {
					String expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					
					//get comparing value from web element
					String tempA = driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					if(tempA.length()<=1) {
						tempA = driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).getAttribute("value");
					}
				
					if(!tempA.equalsIgnoreCase(expectedVal)) {
						perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 3);
						return perRowData;
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("datepicker")) {
					
					//get date input from current row of data and split it into year, month, and day
					String[] dateArray = dateSplitter(perRowData, columnStructureBeanJr);

					//click calendar
					driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).click(); // change this only for other types
					sleep(2000);
					
					//set year, month, and date on calendar widget
					setCalendar(driver, dateArray);
					
				}
			}
				
			perRowData = successfulTestCaseHandler(perRowData, driver, columnStructureBeanJr);
			return perRowData;
			
		}catch(Exception e) {
			perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, e, 1);
			return perRowData;
		}
	}
	
	public String[] testByName(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
		
		try {
			if(!explicitWait("name", driver, perRowData[columnStructureBeanJr.getColWebElementName()])) {
				if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("input")) {
					driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
					driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(perRowData[columnStructureBeanJr.getColInputOutputValue()]);
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("click")) {
					driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("hover")) {
					Actions action = new Actions(driver);
					WebElement btn = driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()]));
					action.moveToElement(btn).perform();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("select")) {
					WebElement selectionObject = driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()]));
					Select options = new Select(selectionObject);
					List<WebElement> optionList = options.getOptions();
					
					for(int i=0; i<optionList.size(); i++) {
						
						if(optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))) {
							optionList.get(i).click();
							break;
						}
						
						else if(!optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))&&i==optionList.size()-1) {
							perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 2);
							return perRowData;
						}
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("compare")) {
					String expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					
					//get comparing value from web element
					String tempA = driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					if(tempA.length()<=1) {
						tempA = driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).getAttribute("value");
					}
				
					if(!tempA.equalsIgnoreCase(expectedVal)) {
						perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 3);
						return perRowData;
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("datepicker")) {
					
					//get date input from current row of data and split it into year, month, and day
					String[] dateArray = dateSplitter(perRowData, columnStructureBeanJr);

					//click calendar
					driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).click(); // change this only for other types
					sleep(2000);
					
					//set year, month, and date on calendar widget
					setCalendar(driver, dateArray);
					
				}
			}
			
			perRowData = successfulTestCaseHandler(perRowData, driver, columnStructureBeanJr);
			return perRowData;
			
		}catch(Exception e) {
			perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, e,1);
			return perRowData;
		}
	}
	
	public String[] testByXpath(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
			
		try {
			
			if(!explicitWait("xpath", driver, perRowData[columnStructureBeanJr.getColWebElementName()])) {

				if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("input")) {
					driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
					driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(perRowData[columnStructureBeanJr.getColInputOutputValue()]);
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("click")) {
					driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).click();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("hover")) {
					Actions action = new Actions(driver);
					WebElement btn = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()]));
					action.moveToElement(btn).perform();
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("select")) {
					WebElement selectionObject = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()]));
					Select options = new Select(selectionObject);
					List<WebElement> optionList = options.getOptions();
					
					for(int i=0; i<optionList.size(); i++) {
						
						if(optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))) {
							optionList.get(i).click();
							break;
						}
						
						else if(!optionList.get(i).getText().replaceAll("\\s", "").equalsIgnoreCase(perRowData[columnStructureBeanJr.getColInputOutputValue()].replaceAll("\\s", ""))&&i==optionList.size()-1) {
							perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 2);
							return perRowData;
						}
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("compare")) {
					String expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					
					//get comparing value from web element
					String tempA = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					if(tempA.length()<=1) {
						tempA = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).getAttribute("value");
					}
				
					if(!tempA.equalsIgnoreCase(expectedVal)) {
						perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, null, 3);
						return perRowData;
					}
				}
				else if(perRowData[columnStructureBeanJr.getColNatureOfAction()].equals("datepicker")) {
					
					//get date input from current row of data and split it into year, month, and day
					String[] dateArray = dateSplitter(perRowData, columnStructureBeanJr);

					//click calendar
					driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).click(); // change this only for other types
					sleep(2000);
					driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).click(); //double-click because sometimes it loads on the first click
					sleep(2000);
					
					//set year, month, and date on calendar widget
					setCalendar(driver, dateArray);
					
				}
			}
			
			perRowData = successfulTestCaseHandler(perRowData, driver, columnStructureBeanJr);
			return perRowData;
			
		}catch(Exception e) {
			perRowData = errorCatcher(perRowData, columnStructureBeanJr, driver, e,1);
			return perRowData;
		}
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////PASSED/FAILED HANDLERS////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public String[] successfulTestCaseHandler(String[] perRowData, WebDriver driver, columnRowStructureBean columnStructureBeanJr) {
		
		//hit enter if triggerEnter is true
		triggerEnter(columnStructureBeanJr, perRowData, driver);
		
		//check for alerts everytime
		checkAlert(driver, perRowData, columnStructureBeanJr);
		
		//marks extentTest as passed
		String desiredScreenShotPath = produceScreenshot(perRowData, columnStructureBeanJr, driver, 1, null);
		
		//mark current testCase data as passed
		perRowData = passedCatcher(perRowData, columnStructureBeanJr, desiredScreenShotPath);
		
		//check if your recent actions has been constrained by a soft or hardblock
		softBlockController(columnStructureBeanJr, driver, perRowData);
		
		return perRowData;
	}
	
	public String[] passedCatcher(String[] perRowData, columnRowStructureBean columnStructureBeanJr, String desiredScreenShotPath) {
		
		if(!isSoftBlockOnProcess) {
			numberGenerator numberGeneratorJr = new numberGenerator();
			perRowData[columnStructureBeanJr.getColRemarks()] = "Passed"; //if perRowData survives try-catch block
			perRowData[columnStructureBeanJr.getColLogField()] = "-";
			perRowData[columnStructureBeanJr.getColTimeStamp()] = numberGeneratorJr.generateDateAndMoment();
			perRowData[columnStructureBeanJr.getColScPath()] = desiredScreenShotPath;
		}
		return perRowData;
	}
	
	public String[] errorCatcher(String[] perRowData, columnRowStructureBean columnStructureBeanJr, WebDriver driver, Exception e, int errorType) {
		/*1: errors from failure to click, hover, or send input
		 *2: errors from unmatched select action
		 *3: errors from unmatched compare action
		 */
		
		if(!isSoftBlockOnProcess) {
			
			String concatError = "";
			String errorMessages = "";
			String desiredScreenShotPath = "";
			String expectedVal = "";
			
			numberGenerator numberGeneratorJr = new numberGenerator();
			
			if(errorType==1) {
				
				if(universeId==1) {
					concatError = concatErrorString(e.getMessage());
					errorMessages = "WUI ERROR:\n"+ checkWuiError(driver) + "\n" + "JAVA ERROR:\n " + concatError;
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}else {
					concatError = concatErrorString(e.getMessage());
					errorMessages = "JAVA ERROR:\n " + concatError;
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}
				
			}else if(errorType==2) {
				
				if(universeId==1) {
					expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					errorMessages = "WUI ERROR:\n"+ checkWuiError(driver) + "\n" + "JAVA ERROR:\n " + " Expected value '" + expectedVal + "' does not match any values from selector";
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}else {
					expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					errorMessages = "JAVA ERROR:\n " + " Expected value '" + expectedVal + "' does not match any values from selector";
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}
				
				
			}else if(errorType==3){
				
				if(universeId==1) {
					//get expected value
					expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					//get comparing value from web element
					String tempA = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					errorMessages = "WUI ERROR:\n"+ checkWuiError(driver) + "\n" + "JAVA ERROR:\n " + "Expected Value '" + expectedVal + "' does not match '" + tempA + "'";
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}else {
					//get expected value
					expectedVal = perRowData[columnStructureBeanJr.getColInputOutputValue()];
					//get comparing value from web element
					String tempA = driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).getText();
					errorMessages = "JAVA ERROR:\n " + "Expected Value '" + expectedVal + "' does not match '" + tempA + "'";
					desiredScreenShotPath = produceScreenshot(null, null, driver, 2, errorMessages);
				}
				
				
			}
			
			perRowData[columnStructureBeanJr.getColRemarks()] = "Failed";
			perRowData[columnStructureBeanJr.getColLogField()] = errorMessages;
			perRowData[columnStructureBeanJr.getColTimeStamp()] = numberGeneratorJr.generateDateAndMoment();
			perRowData[columnStructureBeanJr.getColScPath()] = desiredScreenShotPath;
			failedAlert();
			
		}
		
		return perRowData;
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////DATEPICKER RELATED ACTIONS////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void setCalendar(WebDriver driver, String[] dateArray) {
		
		//set year in calendar widget
		setYear(driver, dateArray[0]);
		//set month in calendar widget
		setMonth(driver, dateArray[1]);
		//set day in calendar widget
		sleep(1500); //requires a bit of loading after clicking month
		setDay(driver, dateArray[2]);
	}
	
	public void setYear(WebDriver driver, String year) {
		/*FOR ACQUIRING YEAR*/
		WebDriverWait wait = new WebDriverWait(driver,10);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cal_year")));
		driver.findElement(By.id("cal_year")).click();
		driver.findElement(By.id("cal_year")).sendKeys(year);
		driver.findElement(By.id("cal_year")).sendKeys(Keys.ENTER);
	}
	
	public void setMonth(WebDriver driver, String month) {
	
		//click month section
		driver.findElement(By.id("calmonthname")).click();
		//go to month list section
		WebElement monthListElements = driver.findElement(By.id("monthList"));
		//get li tag from month list
		List<WebElement> monthList = monthListElements.findElements(By.tagName("li"));
		
		for(int i=0; i<monthList.size();i++) {
			if(Integer.parseInt(month)!=10) {
				if(i==Integer.parseInt(month.replaceAll("0", ""))-1) {
					monthList.get(i).click();
					break;
				}
			}else {
				if(i==Integer.parseInt(month)-1) {
					monthList.get(i).click();
					break;
				}
			}
		}
		
	}
	
	public void setDay(WebDriver driver, String day) {
		
		//find webelement "picker"
		WebElement div = driver.findElement(By.className("picker"));
		//find table attribute from webelement picker
		WebElement table = div.findElement(By.tagName("table"));
		//findtbody attribute from attribute table
		WebElement tbody = table.findElement(By.tagName("tbody"));
		//find all tr attributes from attribute tbody
		List<WebElement> tr = tbody.findElements(By.tagName("tr"));
		
		//iterate through all tr attributes and aquire td elements from each
		//iterate through all td attributes and break if td's text is equal to expected input
		outerloop:
		for(int i=0; i<tr.size(); i++) {
			List<WebElement> days = tr.get(i).findElements(By.tagName("td"));
			for(int x=0; x<days.size(); x++) {
				if(days.get(x).getText().equals(Integer.toString(Integer.parseInt(day))) && days.get(x).getAttribute("class").toLowerCase().contains("this_month")) {
					days.get(x).click();
					break outerloop;
				}
			}
		}
		driver.switchTo().defaultContent();
	}
	
	public String[] dateSplitter(String[] perRowData, columnRowStructureBean columnStructureBeanJr) {
		
		//get input values from current row data
		String date = perRowData[columnStructureBeanJr.getColInputOutputValue()];
		
		//split input values using "-"
        String[] dateArray = date.split("-");  //0: year 1: month 2: day
        
        return dateArray;
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////SUCCESSFUL/FAILED TESTCASE RELATED ACTIONS////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void triggerEnter(columnRowStructureBean columnStructureBeanJr, String[] perRowData, WebDriver driver) {
		
		String triggerEnterColumn = perRowData[columnStructureBeanJr.getColTriggerEnter()];
		String WebElementNatureColumn = perRowData[columnStructureBeanJr.getColWebElementNature()];
		
		if(triggerEnterColumn.toLowerCase().contains("true")){
			sleep(1500);
			if(WebElementNatureColumn.toLowerCase().equals("id")) {
				driver.findElement(By.id(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(Keys.ENTER);
			}else if(WebElementNatureColumn.toLowerCase().equals("class")) {
				driver.findElement(By.className(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(Keys.ENTER);
			}else if(WebElementNatureColumn.toLowerCase().equals("name")) {
				driver.findElement(By.name(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(Keys.ENTER);
			}else if(WebElementNatureColumn.toLowerCase().equals("xpath")) {
				driver.findElement(By.xpath(perRowData[columnStructureBeanJr.getColWebElementName()])).sendKeys(Keys.ENTER);
			}
			
			driver.findElement(By.tagName("body")).click(); //some elements requires interacting with other elements to save even if trigger enter is set to true
		}
		
	}
	
	public void checkAlert(WebDriver driver, String[] perRowData, columnRowStructureBean columnStructureBeanJr) {
		
		if(perRowData[columnStructureBeanJr.getColLabel()].equalsIgnoreCase("submit button")) {
			try{
				sleep(1500);
				driver.switchTo().alert().accept();
				driver.switchTo().defaultContent();
			}catch(Exception e) {/*nothing to do here*/}
		}
		
	}
	
	public void failedAlert() {
			failedFlag = true;
	}
	
	public String produceScreenshot(String[] perRowData, columnRowStructureBean columnStructureBeanJr, WebDriver driver, int updateType, String errorMessages) {

		//Extent reporter marks as "passed", uses notes cell as information of test, and takes screenshot if necessary
		/* 1: produces screenshot if necessary (for passed)
		 * 2: produces screenshot (for failed)
		 */
		
		String desiredScreenShotPath = "";
		
		if(!isSoftBlockOnProcess) {
			
			try {
				if(updateType==1) {

					if(perRowData[columnStructureBeanJr.getColScreenCapture()].toLowerCase().equals("true")) {
						desiredScreenShotPath = screenCapture(driver);
						MediaEntityBuilder.createScreenCaptureFromPath(desiredScreenShotPath).build();
						MediaEntityBuilder.createScreenCaptureFromPath(desiredScreenShotPath).build();
					}
					
				}else if(updateType==2) {
					
						desiredScreenShotPath = screenCapture(driver);
						MediaEntityBuilder.createScreenCaptureFromPath(desiredScreenShotPath).build();
						MediaEntityBuilder.createScreenCaptureFromPath(desiredScreenShotPath).build();
				}
				
			}catch(Exception e) {/*nothing to do here*/}
			
		}
		
		return desiredScreenShotPath;
	}
	
	public String screenCapture(WebDriver driver) {
		
		numberGenerator numberGeneratorJr = new numberGenerator();
		storagePathsBean storagePathsBeanJr = new storagePathsBean();
		screenShotFolderPath = storagePathsBeanJr.getScreenShotFolderPath();
		
		//take a screenshot
		File srcPath = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		String desiredScreenShotPath = screenShotFolderPath + numberGeneratorJr.generateRandomNumber()+".png"; 
		
		//copy screenshot from srcpath to desiredpath
		try {
			FileHandler.copy(srcPath, new File(desiredScreenShotPath));
			srcPath.delete();
		} catch (IOException e) {
			
		}
		
		return desiredScreenShotPath;
	}
	
	public void softBlockController(columnRowStructureBean columnStructureBeanJr, WebDriver driver, String[] perRowData) {
		
		//re-perform previous test data if softBlock is true and if note column is "check button"
		if(universeId==1&&perRowData[columnStructureBeanJr.getColLabel()].equalsIgnoreCase("check button")&&!isSoftBlockOnProcess) {
			if(softBlockChecker(driver, perRowData)){
				isSoftBlockOnProcess = true;
				testTypeDeterminer(columnStructureBeanJr, driver, perRowData); 
				isSoftBlockOnProcess = false; //reset value to false if softBlockController initiates a softBlock check which changes the value to true
			}
		}
		
	}
	
	public boolean softBlockChecker(WebDriver driver, String[] perRowData){
		
		boolean tempA = false;
		
		try{
			
			WebDriverWait wait = new WebDriverWait(driver,8); //repeatedly pool messenger element for 8 seconds. Do nothing if it cannot be pooled.
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("messenger")));
			
			WebElement messengerError = driver.findElement(By.className("messenger"));
			List<WebElement> actualError = messengerError.findElements(By.tagName("li"));
			
			for (int i = 0; i < actualError.size(); i++)
			{
			   if( actualError.get(i).findElement(By.tagName("label")).getAttribute("class").equalsIgnoreCase("error")) {
				   return tempA; //will be immediately returned if id error is encountered
			   }
			}
			
			tempA = true; //value will be changed to true if no error has been encountered
			
		}catch(Exception e){/*nothing to do here*/}
		
		return tempA;
		
	}
	
	public String checkWuiError(WebDriver driver) {
		
		String NL = "\n";
		String errors = highSeverityError+NL; //has no string value if there is no high severity
		
		try{
			WebElement messengerError = driver.findElement(By.className("messenger"));
			List<WebElement> actualError = messengerError.findElements(By.tagName("li"));
			
			
			for (int i = 0; i < actualError.size(); i++)
			{
				if(actualError.get(i).findElement(By.tagName("label")).getAttribute("class").equalsIgnoreCase("error")) {
					errors+=actualError.get(i).findElement(By.tagName("label")).getText() + NL;
				}
			    
			}
		}catch(Exception e){/*nothing to do here*/}
		return errors;
	}
	
	public String concatErrorString (String errorMessage) {
		
		String newErrorMessage = "";
		int ctr = 0;
		
		for(int i=0; i<errorMessage.length();i++) {
			if(errorMessage.charAt(i) == '}' || errorMessage.charAt(i) == '{') {
				ctr++;
			}
			if(ctr==2) {
				newErrorMessage = errorMessage.substring(0,i+1);
				System.out.println(errorMessage.substring(0,i+1));
				break;
			}
		}
		
		System.out.println(newErrorMessage);
		return newErrorMessage;
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////WAIT RELATED ACTIONS////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void sleep(int n) {
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean explicitWait(String webElementNature, WebDriver driver, String webElementName) {
	    
		WebElement element = null;
		Wait<WebDriver> wait = new WebDriverWait(driver,15).withMessage("Selenium can't locate the element: " + webElementName);
		boolean skip = false;
		try {
		    if(webElementNature.equals("id")) {
		    	element = driver.findElement(By.id(webElementName));
		    	wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(webElementName)));
		    }else if(webElementNature.equals("class")) {
		    	element = driver.findElement(By.className(webElementName));
		    	wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(webElementName)));
		    }else if(webElementNature.equals("name")) {
		    	element = driver.findElement(By.name(webElementName));
		    	wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(webElementName)));
		    }else if(webElementNature.equals("xpath")){
		    	element = driver.findElement(By.xpath(webElementName));
		    	wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(webElementName)));
		    }
		    
		}catch(Exception e) {
			skip = tryIfInivisible(driver, element);
		}
		
		sleep(100); 
		return skip;
	}
	
	public boolean tryIfInivisible(WebDriver driver, WebElement element) {
		
		boolean tempA = true;
		try {
			JavascriptExecutor js = (JavascriptExecutor)driver;
			js.executeScript("arguments[0].click();", element);
		}catch(Exception e) {
			tempA = false;
		}
		
		return tempA;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////OTHERS////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void logOutWebsite(WebDriver driver) {
		try {
			driver.findElement(By.xpath("/html[1]/body[1]/div[1]/ul[2]/li[8]")).click();
		}catch(Exception e){
			System.out.println("logout button does not exist");
		}
		
	}
	
	public boolean failedAlertFrontChecker(columnRowStructureBean columnStructureBeanJr, String[] perRowData) {
		
		if(perRowData[columnStructureBeanJr.getColRemarks()].equalsIgnoreCase("Failed") || perRowData[columnStructureBeanJr.getColRemarks()].equalsIgnoreCase("Pending")) {
			return true;
		}else {
			return false;
		}
		
	}
	
	public void checkSeverityController(WebDriver driver, String[] perRowData, columnRowStructureBean columnStructureBeanJr) {
		
		//check severity only if test data is on "submit button" portion and ignoreSeverity is TRUE
		if(universeId == 1&&perRowData[columnStructureBeanJr.getColLabel()].equalsIgnoreCase("submit button")&&ignoreSeverity==true) {
			sleep(3000);
			checkSeverityCmbsg(driver);
			checkSeverityUob(driver);
		}
		
	}
	
	public void checkSeverityCmbsg(WebDriver driver) {
		
		try{
			WebElement severityRowIndicator = driver.findElement(By.id("gdv0i.1.1.1."));
			List<WebElement> severityRowIndicatorData = severityRowIndicator.findElements(By.tagName("td"));
			
			for (int i = 0; i < severityRowIndicatorData.size(); i++)
			{
			    if(severityRowIndicatorData.get(i).getText().toLowerCase().equals("medium")){
			    	//select constraint checkbbox and clarify button
			    	driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[2]/div[2]/div[1]/div[2]/div[2]/table[1]/tbody[1]/tr[3]/td[4]/input[1]")).click();
			    	sleep(1000);
			    	driver.findElement(By.id("btn_X_x_1")).click(); //clarify cases button
			    	sleep(3000);
			    	
			    	//switch driver to modal
			    	driver.switchTo().frame("xguiframe");
			    	driver.findElement(By.name("reasonC")).click(); //reason text box
			    	sleep(1000);
			    	driver.findElement(By.name("reasonC")).sendKeys("clarify case"); //reason text box
			    	sleep(3000);
			    	driver.findElement(By.id("btn_U__14")).click(); //save button
			    	sleep(3000);
			    	
			    	//return driver to default screen
			    	driver.switchTo().defaultContent();
			    	sleep(2000);
			    	
			    }
			}
		}catch(Exception e){/*nothing to do here*/}
		
	}
	
	public void checkSeverityUob(WebDriver driver) {
		
		int high = 0;
		int medium = 0;
		String finalTblName = "";
		String[] tblName = new String[2];
		tblName[0] = "gdv0i";
		tblName[1] = "r_2d3";//tbl name if all values are either low and medium
		
		
		//check name of existing table that contains severity levels
		for(int i=0; i<tblName.length;i++) {
			try {
				driver.findElement(By.id(tblName[i]));
				finalTblName = tblName[i];
				break;
			}catch(Exception e) {/*notinhg to do here*/}
		}
		
		//get data per table row
		try{
			WebElement table = driver.findElement(By.id(finalTblName));
			WebElement tbody = table.findElement(By.tagName("tbody"));
			List<WebElement> tr = tbody.findElements(By.tagName("tr"));
			
			for(int i=0; i<tr.size(); i++) {
				List<WebElement> data = tr.get(i).findElements(By.tagName("td"));
				for(int x=0; x<data.size(); x++) {
					if(data.get(x).getText().equalsIgnoreCase("medium")) {
						medium++;
						try {
							data.get(x+2).findElement(By.tagName("input")).click(); //error if there is no button to click when high severity
						}catch(Exception e){/*nothing to do here*/}
					}else if(data.get(x).getText().equalsIgnoreCase("high")) {
						high++;
					}
				}
			}
			
			//no high, 1 or more medium, regardless of low
			if(high==0 && medium>0) { 
				driver.findElement(By.id("btn_X_x_1")).click(); //clarify cases button
		    	sleep(2000);
		    	try {
		    		WebElement element = driver.findElement(By.id("btn_w_X5_"));
		    		JavascriptExecutor js = (JavascriptExecutor)driver;
					js.executeScript("arguments[0].click();", element);
		    	}catch(Exception e) {/*nothing to do here*/}
		    	
		    	
		    	driver.findElement(By.id("form.reasonC")).sendKeys("clarify case"); //reason text box
		    	sleep(1000);
		    	driver.findElement(By.id("btn_U__14")).click(); //save button
		    	driver.switchTo().defaultContent();
		    
		    //if high is present, regardless of medium and low
			}else if(high>0) { 
				highSeverityError = "One or more cases have high severity.";
			}
			
	    	//if all are low, nothing will be performed
			
		}catch(Exception e){/*nothing to do here*/}
		
	}

}


