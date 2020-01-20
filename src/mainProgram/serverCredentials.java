package mainProgram;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.CSVReader;
import Beans.columnRowStructureBean;
import dbConnectivity.dbConnect;

public class serverCredentials {
    
	
	public String[] getCredentials(Connection con, List<String[]> newDataFile, columnRowStructureBean columnStructureBeanJr) {
		
		String[] tempCredentials = new String[4];
		ResultSet rs = null;
		
		//Get url from csv
		String url = newDataFile.get(columnStructureBeanJr.getRowWebsite())[1];
		
		//connect to database to get webaddressid
		dbConnect dbConnectJr = new dbConnect();
		rs = dbConnectJr.dataBaseController(con, 8, null, null, null, null, null, 0, url, 0, null, 0); //0 is not used
		
		//get web address id from resultSet
		int webAddressId = 0;
		try{
			while(rs.next()) {
				webAddressId = rs.getInt("web_address_id");
				break;
			}
		}catch(Exception e) {e.printStackTrace();}
		
		//connect to database to get credentials using web addressid
		rs = dbConnectJr.dataBaseController(con, 9, null, null, null, null, null, 0, null, webAddressId, null, 0);
		try {
			rs.absolute(1);
			tempCredentials[0]= rs.getString("hostname"); //hostname
			tempCredentials[1]= rs.getString("username"); //username
			tempCredentials[2]= rs.getString("password"); //password
			tempCredentials[3]= rs.getString("ppk_filepath"); //keyPath
		}catch(Exception e) {e.printStackTrace();}
		
		return tempCredentials;
	}
	
	public List<String[]> readCredentialFile(String filePath){
		
		List<String[]> dataFile = new ArrayList<>();
		String[] tempA;
		
		try {
			FileReader fileReader = new FileReader(filePath);
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
