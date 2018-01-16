package com.developerworks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.philips.lighting.hue.listener.PHScheduleListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLightState;
import com.philips.lighting.model.PHSchedule;
import com.philips.lighting.model.PHSchedule.RecurringDay;

public class UpdateSchedules {

	private enum State {
		
		SUNRISE("Sunrise", false),
		SUNSET("Sunset", true);
		
		private String name;
		
		private boolean state;
		
		private State(String name, boolean state) {
			
			this.name = name;
			this.state = state;
				
		}
		
	}
	
	private Calendar calendar;
	
	private PHHueSDK phHueSDKInstance;
	
    private PHSDKListener listener = new PHSDKListener() {

		public void onAccessPointsFound(List<PHAccessPoint> accessPoints) {
			
			System.out.println("Access Points found");
			// Will only connect to the first hub on your network
			phHueSDKInstance.connect(accessPoints.get(0));
			
		}

		public void onAuthenticationRequired(PHAccessPoint accessPoint) {
			
			System.out.println("Authentication needed");
            phHueSDKInstance.startPushlinkAuthentication(accessPoint);
            
		}

		public void onBridgeConnected(PHBridge bridge, String username) {
			
			System.out.println("Connected to bridge");
			Map<String, PHSchedule> schedules = bridge.getResourceCache().getSchedules();
			PHSchedule phSchedule = null;
			for(PHSchedule schedule: schedules.values()) {
				
				if (state.name.equals(schedule.getName())) {
					
					System.out.println(String.format("Schedule \"%s\" found.", state.name));
					phSchedule = schedule;
					
				} 
				
			}
			processSchedule(bridge, phSchedule, calendar);
						
		}

		public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
		}

		public void onConnectionLost(PHAccessPoint accessPoint) {
		}

		public void onConnectionResumed(PHBridge phBridge) {
		}

		public void onError(int errorCode, String errorString) {
		}

		public void onParsingErrors(List<PHHueParsingError> errors) {
		} 

    };

    private Properties hueProperties;
    
    private State state;
    
	public UpdateSchedules(State state) {
		
		this.state = state;
		hueProperties = new Properties();
		try (InputStream inputFile = new FileInputStream("./bridge.properties")) {

			hueProperties.load(inputFile);


		} catch (IOException ex) {
			
			ex.printStackTrace();
			
		}
    	Location location = new Location(hueProperties.getProperty("latitude"), hueProperties.getProperty("longitude"));
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
		calendar = state.state ? calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()) : calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());

		PHHueSDK phHueSDK = PHHueSDK.getInstance();
		PHAccessPoint accessPoint = new PHAccessPoint();
		accessPoint.setIpAddress(hueProperties.getProperty("bridgeIPAddress"));
		accessPoint.setUsername(hueProperties.getProperty("username"));
		phHueSDK.connect(accessPoint);
		phHueSDK.getNotificationManager().registerSDKListener(listener);
    	phHueSDKInstance = PHHueSDK.getInstance();

	}
	
    private void createSchedule(final PHBridge bridge, final Calendar calendar) {
    	
		System.out.println(String.format("Creating schedule \"%s\" with time %s", state.name, calendar.getTime()));
		PHSchedule schedule = new PHSchedule(state.name);
        
		PHLightState lightState = new PHLightState();
		lightState.setOn(state.state);
		        
		schedule.setRecurringDays(RecurringDay.RECURRING_ALL_DAY.getValue());
		schedule.setLightState(lightState);
		schedule.setLightIdentifier("1");
		schedule.setLocalTime(true);
		schedule.setDate(calendar.getTime());
		bridge.createSchedule(schedule, createPHScheduleListener());

    }
    
    private PHScheduleListener createPHScheduleListener() {
    	
    	return new PHScheduleListener() {

			public void onError(int errorCode, String errorString) {

				System.out.println(String.format("Error updating schedule \"%s\"\terrorCode: %d\terrorString: %s", state.name, errorCode, errorString));
				System.exit(-1);
				
			}

			public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {

				System.out.println("Schedule updated");
				
			}

			public void onSuccess() {
				
				System.out.println("Success.");
				System.exit(0);
				
			}

			public void onCreated(PHSchedule schedule) {
				
				System.out.println(String.format("Schedule %s created successfully.", schedule.getName()));
				System.exit(0);
				
			} 
			
		};
		
    }
    
    private void processSchedule(final PHBridge bridge, final PHSchedule schedule, Calendar calendar) {
    	
		if (schedule == null) {
			
			createSchedule(bridge, calendar);
			
		} else {
			
			System.out.println(String.format("Updating schedule \"%s\" with time %s", schedule.getName(), calendar.getTime()));
			schedule.setDate(calendar.getTime());
			schedule.setAutoDelete(Boolean.TRUE);
			bridge.updateSchedule(schedule, createPHScheduleListener());
			
		}

    }
    
	public static void main(String[] args) {
		
		if (args.length != 1) {
			
			error();
			
		} else {
			
			UpdateSchedules.State state =  UpdateSchedules.State.valueOf(args[0]);
			if (state == null) {
				
				error();
				
			}
			new UpdateSchedules(state);
				
		}
		
	}
	
	private static void error() {

		System.err.println("Usage:\n\tTo update the sunrise schedule: java -jar recipe.jar SUNRISE\n\tTo update the sunset schedule: java -jar recipe.jar SUNSET");
		System.exit(-1);
		
	}

}
