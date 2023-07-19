package com.site.scrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

public class Starter {
	
	public static WebDriver ui;
	private static Properties props = new Properties();
	
	public static void main(String[] args) throws InterruptedException, IOException {
//		ClassLoader loader = Thread.currentThread().getContextClassLoader(); 
		System.out.println("propfile = " + args[0]);
//		InputStream stream = loader.getResourceAsStream(args[0]);
		props.load(new FileInputStream(new File(args[0])));
		
		Scrapper scrapper = new  Scrapper(props);
//		if (args.length > 0) {
//			String distt = args[0];
//			String area = args[1];
//			String propNo = args[2];
//			scrapper.searchAndExtract(distt, area, Integer.valueOf(propNo));
//			return;
//		}
		try {
			String webDriverLoc = props.getProperty("webdriver.location");
			System.out.println("webdriver location = " + webDriverLoc);
			if (webDriverLoc == null) {
				webDriverLoc = "/Users/ashish/Downloads/chromedriver_mac_arm64/chromedriver";
			}
			System.setProperty("webdriver.chrome.driver", webDriverLoc);
	        //to launch the chrome browser window
	        //maximize the browser window
//			ui = new ChromeDriver();
//	        ui.manage().window().maximize();
//			process();
			
			scrapper.scrapForMumbai();
		} finally {
			//ui.close();
		}
		
	}

	private static void process() throws InterruptedException, IOException {
		String exportFolder = props.getProperty("export.folder");
		if (exportFolder == null) {
			exportFolder = "/Users/ashish/Desktop/";
		}
		
		CSVWriter writer = new CSVWriter(new FileWriter(new File( exportFolder + "export.csv")));
		ui.get("https://freesearchigrservice.maharashtra.gov.in/");
		Select distt = new Select( ui.findElement(By.id("ddlDistrict")));
		distt.selectByIndex(2);
		Thread.sleep(200);
		
		ui.findElement(By.id("txtAreaName")).click();
		Thread.sleep(200);
		
		Set<String> vills = new HashSet<>();
		for (char ch = 'a'; ch <= 'z'; ch++ ) {
			// typing area name in text box
			ui.findElement(By.id("txtAreaName")).clear();
			ui.findElement(By.id("txtAreaName")).click();
			ui.findElement(By.id("txtAreaName")).sendKeys(String.valueOf(ch));
			ui.findElement(By.id("txtAreaName")).click();
			ui.findElement(By.xpath("/html/body/center/form/div[3]/div/div/div[2]/div/div[2]/div/div[2]/div/div[3]/div[2]/div")).click();
			Thread.sleep(2000);
			
			Select areaSelect = new Select(ui.findElement(By.xpath("//*[@id=\"ddlareaname\"]")));
			List<WebElement> areaOptions = areaSelect.getOptions();
			for (int ai = 1; ai < areaOptions.size(); ai++) {
				System.out.println(areaOptions.get(ai).getText());
				vills.add(areaOptions.get(ai).getText());
			}
			
		}
		System.out.println(vills);
		System.out.println(new ObjectMapper().writeValueAsString(vills)	);
		
		ui.findElement(By.id("txtAreaName")).sendKeys("b");
		Thread.sleep(200);
		ui.findElement(By.id("txtAreaName")).click();
		
		ui.findElement(By.xpath("/html/body/center/form/div[3]/div/div/div[2]/div/div[2]/div/div[2]/div/div[3]/div[2]/div")).click();
		Thread.sleep(3000);
		Select areaSelect = new Select(ui.findElement(By.xpath("//*[@id=\"ddlareaname\"]")));
		areaSelect.selectByIndex(1);
		
		ui.findElement(By.id("txtAttributeValue")).click();
		Thread.sleep(200);
		ui.findElement(By.id("txtAttributeValue")).sendKeys("0");
		Thread.sleep(200);
		
		String captcha = ui.findElement(By.id("txtCaptcha1")).getAttribute("value");
		Thread.sleep(200);
		ui.findElement(By.id("txtImg")).click();
		Thread.sleep(200);
		ui.findElement(By.id("txtImg")).sendKeys(captcha);
		Thread.sleep(200);
		ui.findElement(By.id("btnSearch")).click();
		
		Thread.sleep(15000);
//		waitTillDisappear("/html/body/center/form/div[3]/div/div/div[2]/div/div[2]/div/div[2]/div/div[6]/div[5]/div/span");
		
		ArrayList<String> tabs = new ArrayList<String>(ui.getWindowHandles()); 
		ui.switchTo().window(tabs.get(0));
		
		WebElement table = ui.findElement(By.id("RegistrationGrid"));
		List<WebElement> rows = table.findElements(By.tagName("tr"));
		List<WebElement> headers = table.findElements(By.tagName("th"));
		
		List<String> csvHeader = headers.stream().map(el -> el.getText().toString()).collect(Collectors.toList());
		String[] arr = new String[csvHeader.size()];
		arr = csvHeader.toArray(arr);
		writer.writeNext(arr);
		for (int row = 1; row < rows.size() - 2; row++) {
			WebElement rowElement = rows.get(row);
			List<WebElement> colElements = rowElement.findElements(By.tagName("td"));
			List<String> csvRow = colElements.stream().map(el -> el.getText().toString()).collect(Collectors.toList());
			arr = new String[csvRow.size()];
			arr = csvRow.toArray(arr);
			writer.writeNext(arr);
			
		}
		writer.close();
		
//		List<Map<String, String>> data = new ArrayList<Map<String,String>>();
//		for (int row = 1; row < rows.size() - 2; row++) {
//			WebElement rowElement = rows.get(row);
//			List<WebElement> colElements = rowElement.findElements(By.tagName("td"));
//			Map<String, String> map = new HashMap<String, String>();
//			data.add(map);
//			for (int col = 0; col < headers.size(); col++) {
//				map.put(headers.get(col).getText(), colElements.get(col).getText());
//			}			
//		}
//		
//		System.out.println(data);
	}

	private static void waitTillDisappear(String xpath) throws InterruptedException {
		while(ui.findElements(By.xpath(xpath)).size() > 0) {
			Thread.sleep(200);
		}
	}

}
