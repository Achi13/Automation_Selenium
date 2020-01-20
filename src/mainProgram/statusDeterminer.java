package mainProgram;
public class statusDeterminer {

	public String[] determineStatus(boolean failureFlag, String[] perRowData) {
		
		if(failureFlag) {
			perRowData[1]="Failed";
		}else {
			perRowData[1]="Passed";
		}
		
		return perRowData;
	}
}
