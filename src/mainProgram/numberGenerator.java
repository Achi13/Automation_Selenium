package mainProgram;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

public class numberGenerator {
	
	public String generateDateAndMoment(){
		//code currently being used by front-end system
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		String tempA = dateFormat.format(date).toString();
		
		return tempA;
	}
	
	public String generateRandomNumber(){
		String tempA;
		
		//generate floating number
		Random random = new Random();
	    tempA = Float.toString(random.nextFloat())+"-";

		//generate date and mo
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		tempA = tempA + dateFormat.format(date).toString();
		
		return tempA;
	}

}
