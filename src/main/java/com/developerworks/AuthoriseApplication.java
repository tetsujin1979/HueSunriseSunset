package com.developerworks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;

public class AuthoriseApplication {

    private PHHueSDK phHueSDKInstance;
    
    public AuthoriseApplication() {

        PHHueSDK phHueSDK = PHHueSDK.create();
        phHueSDK.getNotificationManager().registerSDKListener(this.listener);
    	phHueSDKInstance = PHHueSDK.getInstance();

    }

    public void findBridges() throws InterruptedException {
    	
        phHueSDKInstance = PHHueSDK.getInstance();
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDKInstance.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);
        
    }

    private PHSDKListener listener = new PHSDKListener() {

		public void onAccessPointsFound(List<PHAccessPoint> accessPoints) {
			
			System.out.println("Access Points found");
			// Will only connect to the first hub on your network
			phHueSDKInstance.connect(accessPoints.get(0));
			
		}

		public void onAuthenticationRequired(PHAccessPoint accessPoint) {
			
			System.out.println("Authentication needed - press button on hub");
            phHueSDKInstance.startPushlinkAuthentication(accessPoint);
            
		}

		public void onBridgeConnected(PHBridge bridge, String username) {
			
			System.out.println("Connected to hub");
            String bridgeIpAddress =  bridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            String properties = String.format("bridgeIPAddress=%s\nusername=%s\nlatitude=\nlongitude=\n", bridgeIpAddress, username);
            // Write the necessary values to a properties file and exit
            try {
				Files.write(Paths.get("./bridge.properties"), properties.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            System.out.println("Properties file written.");
            System.exit(0);

		}

		public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
		}

		public void onConnectionLost(PHAccessPoint accessPoint) {
		}

		public void onConnectionResumed(PHBridge phBridge) {
		}

		public void onError(int arg0, String arg1) {
		}

		public void onParsingErrors(List<PHHueParsingError> errors) {
		} 

    };

    public static void main(String[] args) throws InterruptedException {

    	AuthoriseApplication hueController = new AuthoriseApplication();
    	hueController.findBridges();

    }

}
