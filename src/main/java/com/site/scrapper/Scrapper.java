package com.site.scrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.opencsv.CSVWriter;

public class Scrapper {

	private static final String url = "https://freesearchigrservice.maharashtra.gov.in/";
//	private static final String loadingXpath = "/html/body/center/form/div[3]/div/div/div[2]/div/div[2]/div/div[2]/div/div[6]/div[5]/div/span";

	public WebDriver ui;

	private CSVWriter writer;

	private WebDriverWait wait;

	private ExcelWriter excel = new ExcelWriter();
	
	private int indexIIMazRetryCount = 10;
	private long maxWaitTimeoutInSec = 40l;
	private int propSearchAttempt = 0;
	private int maxPropSearchAttempt = 5;
	private String mainTab;
	private String exportFolder;
	private Properties props;
	
	private boolean scrapManual;

	public Scrapper(Properties props) throws IOException {
		this.props = props;
		this.scrapManual = "true".equalsIgnoreCase(this.props.getProperty("scrapmanual"));
		String webDriverLoc = props.getProperty("webdriver.location");
		if (webDriverLoc == null) {
			webDriverLoc = "/Users/ashish/Downloads/chromedriver_mac_arm64/chromedriver";
		}
		System.setProperty("webdriver.chrome.driver", webDriverLoc);
		// to launch the chrome browser window
		ui = new ChromeDriver();
		ui.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
		wait = new WebDriverWait(ui, Duration.ofSeconds(maxWaitTimeoutInSec));
		// maximize the browser window
		ui.manage().window().maximize();
		ui.get(url);
		mainTab = ui.getWindowHandle();
		exportFolder = props.getProperty("export.folder");
		System.out.println("exportFolder = " + exportFolder);
		if (exportFolder == null) {
			exportFolder = "/Users/ashish/Desktop/";
		}
		writer = new CSVWriter(new FileWriter(new File(exportFolder + "export.csv")));
	}

	public void scrapForMaharashtra() throws IOException {

	}

	public void scrapForMumbai() throws IOException {
		try {

			if (scrapManual) {
				searchAndExtract(
						props.getProperty("manual.distt"), 
						props.getProperty("manual.area"), 
						Integer.valueOf(props.getProperty("manual.propNo"))
						);
			} else {
				Map<String, Object> districts = MetaReader.getDisttMap(props.getProperty("district.file"));
				for (Map.Entry<String, Object> entry : districts.entrySet()) {
					String disttVal = entry.getKey();
					@SuppressWarnings("unchecked")
					List<String> areas = (List<String>) entry.getValue();
					for (String area : areas) {
						for (int propNo = 0; propNo < 10; propNo++) {
							searchAndExtract(disttVal, area, propNo);
						}
					}
				}
			}
		} catch (Exception e) {
			if (ui.getTitle().equalsIgnoreCase("Service Unavailable")) {
				ui.close();
				scrapForMumbai();
			}
			e.printStackTrace();
		} finally {
			writer.close();
			excel.exportFile(exportFolder);
		}
	}

	public void searchAndExtract(String disttVal, String area, int propNo) throws InterruptedException, IOException {
		// select district
		if (ui.findElements(By.xpath("//input[@id=\"btnSearch\"]")).size() == 0) {
			Thread.sleep(200);
			ui.findElement(By.id("btnCancel")).click();
		}
		new Select(ui.findElement(By.id("ddlDistrict"))).selectByValue(disttVal);

		// enter village text to search the village
		ui.findElement(By.id("txtAreaName")).clear();
		Thread.sleep(500);
		ui.findElement(By.id("txtAreaName")).click();
		Thread.sleep(500);
		ui.findElement(By.id("txtAreaName")).sendKeys(String.valueOf(area));
		Thread.sleep(500);
		ui.findElement(By.id("txtAreaName")).click();

		// click out of textbox to loose focus.
		ui.findElement(By.id("lblSelectVillage")).click();
		Thread.sleep(200);
		wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("//select[@id=\"ddlareaname\"]/option"), 1));

		// select area in drop down
		new Select(ui.findElement(By.xpath("//select[@id=\"ddlareaname\"]"))).selectByValue(area);

		// Enter Property No.
		ui.findElement(By.id("txtAttributeValue")).click();
		ui.findElement(By.id("txtAttributeValue")).sendKeys(String.valueOf(propNo));

		Thread.sleep(300);
		// get and enter captcha
		String captcha = ui.findElement(By.id("txtCaptcha1")).getAttribute("value");
		ui.findElement(By.id("txtImg")).click();
		ui.findElement(By.id("txtImg")).clear();
		ui.findElement(By.id("txtImg")).sendKeys(captcha);

		Thread.sleep(300);
		// click on search button
		ui.findElement(By.xpath("//input[@id=\"btnSearch\"]")).click();

		// wait for resposne to appear
		try {
		wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("//table[@id=\"RegistrationGrid\"]"), 0));
		} catch (TimeoutException e) {
			if (ui.findElements(By.id("lblMsgCTS")).size() > 0) {
				closeTabAndFocusToMailTab();
				ui.findElement(By.id("btnCancel")).click();
				return;
			}
				//retry
				if (propSearchAttempt > maxPropSearchAttempt) {
					throw new RuntimeException("tried searching property " + propSearchAttempt + ", still could not find the result. Make sure website in up and running");
				}
				ui.findElement(By.id("btnCancel")).click();
				searchAndExtract(disttVal, area, propNo);
				propSearchAttempt ++;
		}
		propSearchAttempt = 0;
//		Thread.sleep(15000);
		// close new tabs and switch to main tab.
		closeTabAndFocusToMailTab();
		
		

		extractTable();
		
		// click on cancel
		Thread.sleep(200);
		ui.findElement(By.id("btnCancel")).click();
		Thread.sleep(300);
	}

	private void closeTabAndFocusToMailTab() {
		String mainTab = ui.getWindowHandle();
		for (String tab : ui.getWindowHandles()) {
			if (!tab.equals(mainTab)) {
				ui.switchTo().window(tab);
				break;
			}
		}
		ui.close();
		ui.switchTo().window(mainTab);
	}

	private void extractTable() throws InterruptedException, IOException {
		WebElement table = ui.findElement(By.id("RegistrationGrid"));
		List<WebElement> rows = table.findElements(By.tagName("tr"));
		int rowsCount = rows.size();
		
		Optional<WebElement> pageIndexContainer = rows.stream().filter(row -> { return "left".equals(row.getAttribute("align"));}).findAny();
		int currPage = 0;
		int totalPages = 0;
		if (pageIndexContainer.isPresent()) {
			totalPages = pageIndexContainer.get().findElement(By.tagName("tbody")).findElements(By.tagName("td")).size();
		}
		do {
			//extract table
			String[] arr;
			for (int currRow = 1; currRow < rowsCount - 2; currRow++) {
				ui.switchTo().window(mainTab);
				table = ui.findElement(By.id("RegistrationGrid"));
				rows = table.findElements(By.tagName("tr"));
				WebElement rowElement = rows.get(currRow);
				Thread.sleep(500);
				List<WebElement> colElements = rowElement.findElements(By.tagName("td"));
				List<String> csvRow = colElements.stream().map(el -> el.getText().toString()).collect(Collectors.toList());
				csvRow.add(String.valueOf(currPage + 1));
				csvRow.addAll(getIndexII(colElements.get(colElements.size() - 1).findElements(By.className("Button")).get(0)));
				
				arr = new String[csvRow.size()];
				arr = csvRow.toArray(arr);
				excel.writeRow(csvRow);
				writer.writeNext(arr);
			}
			currPage++;
		} while (currPage < totalPages);
		
		
	}

	private List<String> getIndexII(WebElement indexIIButton) throws InterruptedException {
		indexIIButton.click();
		Thread.sleep(500);
		List<String> indexII = new ArrayList<>();
		for (int i = 0; i < indexIIMazRetryCount; i++) {
			if (i==0) {
				switchToNewTab();
			}
			List<WebElement> table = ui.findElements(By.tagName("table"));
			if (table.size() <= 0) {
				ui.navigate().refresh();
				Thread.sleep(700);
				continue;
			}
			wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("/html/body/table[1]/tbody/tr/td[1]/table/tbody/tr[1]/td/font"), 0));
			// indexII No
			indexII.add(ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[1]/table/tbody/tr[1]/td/font")).getText());
			// date
			indexII.add(ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[1]/table/tbody/tr[2]/td/font")).getText());
			
			//printToPdf(ui.getCurrentUrl());
			
			// दुय्यम निबंधक
			String val = ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[3]/table/tbody/tr[1]/td")).getText();
			if (val.contains(":")) {
				val = val.substring(val.indexOf(":") + 1).trim();
			}
			indexII.add(val);
			// दस्त क्रमांक
			val = ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[3]/table/tbody/tr[2]/td")).getText();
			if (val.contains(":")) {
				val = val.substring(val.indexOf(":") + 1).trim();
			}
			indexII.add(val);
			

			// नोदंणी
			val = ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[3]/table/tbody/tr[3]/td")).getText();
			if (val.contains(":")) {
				val = val.substring(val.indexOf(":") + 1).trim();
			}
			indexII.add(val);
			
			// Regn
			val = ui.findElement(By.xpath("/html/body/table[1]/tbody/tr/td[3]/table/tbody/tr[4]/td")).getText();
			if (val.contains(":")) {
				val = val.substring(val.indexOf(":") + 1).trim();
			}
			indexII.add(val);
			
			// विलेखाचा प्रकार
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[1]/td[2]")).getText());
			// मोबदला
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[2]/td[2]")).getText());
			// बाजारभाव(भाडेपटटयाच्या बाबतितपटटाकार आकारणी देतो की पटटेदार ते नमुद करावे)
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[3]/td[2]")).getText());
			// भू-मापन,पोटहिस्सा व घरक्रमांक(असल्यास)
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[4]/td[2]")).getText());
			// क्षेत्रफळ
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[5]/td[2]")).getText());
			// आकारणी किंवा जुडी देण्यात असेल तेव्हा.
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[6]/td[2]")).getText());
			// दस्तऐवज करुन देणा-या/लिहून ठेवणा-या पक्षकाराचे नाव किंवा दिवाणी न्यायालयाचा हुकुमनामा किंवा आदेश असल्यास,प्रतिवादिचे नाव व पत्ता.
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[7]/td[2]")).getText());
			// दस्तऐवज करुन घेणा-या पक्षकाराचे व किंवा दिवाणी न्यायालयाचा हुकुमनामा किंवा आदेश असल्यास,प्रतिवादिचे नाव व पत्ता
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[8]/td[2]")).getText());
			// दस्तऐवज करुन दिल्याचा दिनांक
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[9]/td[2]")).getText());
			// दस्त नोंदणी केल्याचा दिनांक
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[10]/td[2]")).getText());
			// अनुक्रमांक,खंड व पृष्ठ
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[11]/td[2]")).getText());
			// बाजारभावाप्रमाणे मुद्रांक शुल्क
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[12]/td[2]")).getText());
			// बाजारभावाप्रमाणे नोंदणी शुल्क
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[13]/td[2]")).getText());
			// शेरा
			if (ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[14]")).findElements(By.tagName("td")).size() > 1) {
				indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[14]/td[2]")).getText());
			} else {
				indexII.add("");
			}
			// मुल्यांकनासाठी विचारात घेतलेला तपशील:-:
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[15]/td[2]")).getText());
			// मुद्रांक शुल्क आकारताना निवडलेला अनुच्छेद :- :
			indexII.add(ui.findElement(By.xpath("/html/body/table[3]/tbody/tr[16]/td[2]")).getText());

			break;
		}
		ui.close();
		ui.switchTo().window(mainTab);
		return indexII;
	}

	private void printToPdf(String currentUrl) {
		try {
			ChromeOptions chromeOptions = new ChromeOptions();
			// PrintToPDF is only supported in headless mode
			chromeOptions.setHeadless(true);
			chromeOptions.setCapability("platformName", "Mac OS X");
			chromeOptions.setCapability("browserVersion", "latest");
			
			Map<String, Object> sauceOptions = new HashMap<>();
			sauceOptions.put("name", "printPageWithChrome");
//		sauceOptions.put("username", userName);
//		sauceOptions.put("accessKey", accessKey);
			chromeOptions.setCapability("sauce:options", sauceOptions);
			
			RemoteWebDriver driver = new RemoteWebDriver(new URL(currentUrl), chromeOptions);
			Path printPage = Paths.get("src/test/screenshots/PrintPageChrome.pdf");
			
			driver.get("https://www.saucedemo.com/v1/inventory.html");
			Pdf print = driver.print(new PrintOptions());
			Files.write(printPage, OutputType.BYTES.convertFromBase64Png(print.getContent()));
			driver.quit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void switchToNewTab() {
		String mainTab = ui.getWindowHandle();
		for (String tab : ui.getWindowHandles()) {
			if (!tab.equals(mainTab)) {
				ui.switchTo().window(tab);
				break;
			}
		}
	}

}
