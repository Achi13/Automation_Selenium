package dbConnectivity;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public class dbConnect {
	
	public String dbUrl =  "jdbc:mysql://localhost/oneqa_db"; //kulang ng port number to
	public String username = "root";
	public String password = "aaaaaa";
	
	//query's parameters
	public String status;
	public String testCaseNumber;
	public String loginAccountId;
	public String url;
	public String clientName;
	public int webAddressId;
	public int serviceType;
	public int executionVersion;
	public long scriptId;
	public LinkedHashMap<String, Object> actualData;
	public LinkedHashMap<String, Object> footer;
	
	
	public ResultSet dataBaseController(Connection con, int serviceType, String status, String testCaseNumber, LinkedHashMap<String, Object> actualData, LinkedHashMap<String, Object> footer, String loginAccountId, int executionVersion, String url, int webAddressId, String clientName, long scriptId) {
		
		//set up values
		setQueryParameterValues(serviceType, status, testCaseNumber, actualData, footer, loginAccountId, executionVersion, url, webAddressId, clientName, scriptId);
		
		//generate a statement depending on service type
		ResultSet rs = statamentGenerationExecutionController(con);
		
		try{
			//con.close();
		}catch(Exception e) {/*nothing to do here8*/}
		
		return rs;
	}
	
	public void setQueryParameterValues(int serviceType, String status, String testCaseNumber, LinkedHashMap<String, Object> actualData, LinkedHashMap<String, Object> footer, String loginAccountId, int executionVersion, String url, int webAddressId, String clientName, long scriptId) {
		
		this.actualData = actualData;
		this.serviceType = serviceType;
		this.status = status;
		this.testCaseNumber = testCaseNumber;
		this.footer = footer;
		this.loginAccountId = loginAccountId;
		this.executionVersion = executionVersion;
		this.url = url;
		this.webAddressId = webAddressId;
		this.clientName = clientName;
		this.scriptId = scriptId;
	}

	public void loadDriver() {
		try {
			Class.forName("com.mysql.jdbc.Driver");	
		}catch(Exception e) {/*nothing to do here*/e.printStackTrace();}
	}
	
	public Connection createConnection() {
		//Create Connection to DB	
		Connection con = null;
		
    	try {
			con = DriverManager.getConnection(dbUrl,username,password);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
    	
    	return con;
	}
	
	public ResultSet statamentGenerationExecutionController(Connection con) {
		/*
		 * 1: update (status of testcase)
		 * 2: insert (perRowData into table)
		 * 3: insert (footer of a testcase into table)
		 */
		ResultSet rs = null;
		PreparedStatement stmt = null;
		
		if(serviceType==1) {
			stmt = statementToUpdateTestCaseStatus(con);
			executeQuery(2, stmt);
		}else if(serviceType==2) {
			stmt = statementToInsertActualData(con);
			executeQuery(2, stmt);
		}else if(serviceType==3) {
			stmt = statementToInsertFooterData(con);
			executeQuery(2, stmt);
		}else if(serviceType==4) {
			stmt = statementToQueryLoginAccountStatus(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==5) {
			stmt = statementToUpdateLoginAccountStatusToUnavailable(con);
			executeQuery(2, stmt);
		}else if(serviceType==6) {
			stmt = statementToUpateAllLoginAccountStatusToAvailable(con);
			executeQuery(2, stmt);
		}else if(serviceType==7) {
			stmt = statementToQueryTestCaseExecutionVersion(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==8) {
			stmt = statementToQueryWebAddressId(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==9) {
			stmt = statementToQueryTapServerCredential(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==10) {
			stmt = statementToQueryUniverseIdBasedOnClientName(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==10) {
			stmt = statementToQueryUniverseIdBasedOnClientName(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==11) {
			stmt = statementToQueryEmbeddedScriptOfTestCase(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==12) {
			stmt = statementToQueryScriptBasedOnScriptId(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==13) {
			stmt = statementToQueryLoginAccountBasedOnLoginAccountId(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==14) {
			stmt = statementToQueryScriptVariablesBasedOnScriptId(con);
			rs = executeQuery(1, stmt);
		}else if(serviceType==15) {
			stmt = statementToQueryStoredValues(con);
			rs = executeQuery(1, stmt);
		}
		
		return rs;
	}
	
	public PreparedStatement statementToQueryLoginAccountStatus(Connection con) {
		
		String query = "SELECT * FROM login_status_tracker WHERE `login_account_id` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, loginAccountId);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryTestCaseExecutionVersion(Connection con) {
		
		String query = "SELECT * FROM dependent_testcase WHERE `testcase_number` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, testCaseNumber);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryWebAddressId(Connection con) {
		
		String query = "SELECT * FROM web_address WHERE `url` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, url);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryTapServerCredential(Connection con) {
		
		String query = "SELECT * FROM client_login_account WHERE `web_address_id` = ? AND `account_type` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setInt(1, webAddressId);
 			stmt.setString(2, "tap");
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryUniverseIdBasedOnClientName(Connection con) {
		
		String query = "SELECT * FROM client WHERE `client_name` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, clientName);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryEmbeddedScriptOfTestCase(Connection con) {
		
		String query = "SELECT * FROM dependent_testcase WHERE `testcase_number` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, testCaseNumber);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryScriptBasedOnScriptId(Connection con) {
		
		String query = "SELECT * FROM script WHERE `script_id` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setLong(1, scriptId);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryLoginAccountBasedOnLoginAccountId(Connection con) {
		
		String query = "SELECT * FROM client_login_account WHERE `login_account_id` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, loginAccountId);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryScriptVariablesBasedOnScriptId(Connection con) {
		
		String query = "SELECT * FROM script_variable WHERE `script_id` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setLong(1, scriptId);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToQueryStoredValues(Connection con) {
		
		String query = "SELECT * FROM dependent_testcase WHERE `testcase_number` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, testCaseNumber);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
		
	}
	
	public PreparedStatement statementToUpdateTestCaseStatus(Connection con) {
		
		String query = "UPDATE testcase_record SET `status` = ? WHERE `testcase_number` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, status);
 			stmt.setString(2, testCaseNumber);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
	}
	
	public PreparedStatement statementToUpdateLoginAccountStatusToUnavailable(Connection con) {
		
		String query = "UPDATE login_status_tracker SET `status` = ? WHERE `login_account_id` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, status);
 			stmt.setString(2, loginAccountId);
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
	}
	
	public PreparedStatement statementToUpateAllLoginAccountStatusToAvailable(Connection con) {
		
		String query = "UPDATE login_status_tracker SET `status` = ? WHERE `status` = ?";
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			//set parameters
 			stmt.setString(1, status);
 			stmt.setString(2, "unavailable");
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
	}
	
	public PreparedStatement statementToInsertActualData(Connection con) {
		
		String query = "INSERT INTO testcase_actual_data (testcase_number, web_element_name, web_element_nature, nature_of_action, is_screen_capture, is_trigger_enter, input_output_value, label, timestamp, screenshot_path, remarks, log_field, execution_version)"
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			
			//set parameters
 			stmt.setString(1, testCaseNumber);
 			stmt.setString(2, actualData.get("WebElementName").toString());
 			stmt.setString(3, actualData.get("WebElementNature").toString());
 			stmt.setString(4, actualData.get("NatureOfAction").toString());
 			stmt.setBoolean(5, (boolean) actualData.get("ScreenCapture"));
 			stmt.setBoolean(6, (boolean) actualData.get("TriggerEnter"));
 			stmt.setString(7, actualData.get("InputOutputValue").toString());
 			stmt.setString(8, actualData.get("Label").toString());
 			stmt.setString(9, actualData.get("TimeStamp").toString());
 			stmt.setString(10, actualData.get("ScPath").toString());
 			stmt.setString(11, actualData.get("Remarks").toString());
 			stmt.setString(12, actualData.get("LogField").toString());
 			stmt.setInt(13, executionVersion);
 			
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
	}

	public PreparedStatement statementToInsertFooterData(Connection con) {
		
		String query = "INSERT INTO testcase_footer_data (testcase_number, client_name, is_ignore_severity, sender, is_server_import, testcase_status, transaction_type, url, assigned_account, tap_import_status, execution_version) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		
		PreparedStatement stmt = null;
		
		try {
			stmt = con.prepareStatement(query);
			
			//set parameters
 			stmt.setString(1, footer.get("TestCaseNumber").toString());
 			stmt.setString(2, footer.get("ClientName").toString());
 			stmt.setBoolean(3, (boolean) footer.get("IgnoreSeverity"));
 			stmt.setString(4, footer.get("Sender").toString());
 			stmt.setBoolean(5, (boolean) footer.get("ServerImport"));
 			stmt.setString(6, footer.get("TestCaseStatus").toString());
 			stmt.setString(7, footer.get("TransactionType").toString());
 			stmt.setString(8, footer.get("Website").toString());
 			stmt.setString(9, footer.get("AssignedAccount").toString());
 			stmt.setString(10, footer.get("TapImportStatus").toString());
 			stmt.setInt(11, executionVersion);
 			
		} catch (SQLException e) {/*nothing to do here*/e.printStackTrace();}
		
		return stmt;
	}

	public ResultSet executeQuery(int service, PreparedStatement stmt) {
		
		ResultSet rs = null;
		
		try {
			
			if(service==1) {
				rs = stmt.executeQuery();
			}else if(service==2) {
				stmt.executeUpdate();
			}
			
		}catch(Exception e) {e.printStackTrace();}
		
		return rs;
	}


}
