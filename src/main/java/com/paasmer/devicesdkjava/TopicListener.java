package com.paasmer.devicesdkjava;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.paasmer.devicesdkjava.util.SampleUtil;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class TopicListener extends AWSIotTopic {
	 private static final String PropertyFile = "config.properties";
	 private GpioController gpio;
	public TopicListener(String topic, AWSIotQos qos) {
		super(topic, qos);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onMessage(AWSIotMessage message) {
		// TODO Auto-generated method stub
	//	System.out.println(message.getStringPayload());
		gpio=GpioFactory.getInstance();
		String msg = message.getStringPayload();
		
		String[] s2= msg.split("\\s");
		String feedName=s2[0];
		String state = s2[1];
		
		Properties prop = new Properties();
		InputStream input = null;

		try {
			URL resource = SampleUtil.class.getResource(PropertyFile); //.class.getResource(PropertyFile);
			input = resource.openStream();//new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			String feeds= prop.getProperty("feed");
			
			JSONArray jArray=new JSONArray(feeds);
			for (int i=0;i<jArray.length();i++) {
				JSONObject jObject=jArray.getJSONObject(i);
				if(jObject.getString("name").equals(feedName)) {
					System.out.println(jObject.getString("pin")+","+state);
					
					Pin pin=RaspiPin.getPinByName("GPIO "+jObject.getString("pin"));
					GpioPinDigitalOutput gdo=gpio.provisionDigitalOutputPin(pin,"my led");
					System.out.println(gdo.getState());
					if(state.equals("on")) {
						gdo.high();
						System.out.println(gdo.getState());
					}else {
						gdo.low();
					
						
						System.out.println(gdo.getState());
					}
					
					gpio.unprovisionPin(gdo);
					
				}
				
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}catch(JSONException e) { 
			e.printStackTrace();
		}finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//System.out.println(feedName);
		
	}

}
