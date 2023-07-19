package com.site.scrapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelWriter {
	
	private Workbook wb;
	private Sheet sheet;
	private List<String> headers;
	private List<String> index2Headers;
	
	public ExcelWriter() {
		wb = new HSSFWorkbook();
		sheet = wb.createSheet("mumbai");
		headers = new ArrayList<>(Arrays.asList("DocNo","DName","RDate","SROName","Seller Name","Purchaser Name","Property Description","SROCode","Status", ""));
		index2Headers = Arrays.asList("Index 2 No", "Date", "दुय्यम निबंधक", "दस्त क्रमांक", "नोदंणी", "Regn", "विलेखाचा प्रकार", "मोबदला", "बाजारभाव(भाडेपटटयाच्या बाबतितपटटाकार आकारणी देतो की पटटेदार ते नमुद करावे)", 
				"भू-मापन,पोटहिस्सा व घरक्रमांक(असल्यास)", "क्षेत्रफळ", "आकारणी किंवा जुडी देण्यात असेल तेव्हा.", "दस्तऐवज करुन देणा-या/लिहून ठेवणा-या पक्षकाराचे नाव किंवा दिवाणी न्यायालयाचा हुकुमनामा किंवा आदेश असल्यास,प्रतिवादिचे नाव व पत्ता.",
				"दस्तऐवज करुन घेणा-या पक्षकाराचे व किंवा दिवाणी न्यायालयाचा हुकुमनामा किंवा आदेश असल्यास,प्रतिवादिचे नाव व पत्ता", "दस्तऐवज करुन दिल्याचा दिनांक", "दस्त नोंदणी केल्याचा दिनांक",
				"अनुक्रमांक,खंड व पृष्ठ", "बाजारभावाप्रमाणे मुद्रांक शुल्क", "बाजारभावाप्रमाणे नोंदणी शुल्क", "शेरा", "मुल्यांकनासाठी विचारात घेतलेला तपशील:-:", "मुद्रांक शुल्क आकारताना निवडलेला अनुच्छेद :- :");
		writeHeader();
	}
	
	public void writeHeader() {
		Row row = sheet.createRow(0);
		HSSFCellStyle style = (HSSFCellStyle) wb.createCellStyle();
		style.setBorderTop(BorderStyle.NONE); // double lines border
		style.setBorderBottom(BorderStyle.THICK); // single line border
		HSSFFont font = (HSSFFont) wb.createFont();
		font.setFontHeightInPoints((short) 15);
		font.setBold(true);
		style.setFont(font);  
		headers.addAll(index2Headers);
		for(int i = 0; i < headers.size(); i++) {
			Cell cell = row.createCell(i);
			cell.setCellStyle(style);
			cell.setCellValue(headers.get(i));
		}
	}
	
	public void writeRow(List<String> rowData) {
		Row row = sheet.createRow(sheet.getLastRowNum() + 1);
		for(int i = 0; i < rowData.size(); i++) {
			Cell cell = row.createCell(i);
			cell.setCellValue(rowData.get(i));
		}
	}
	
	public void exportFile(String exportFolder) throws IOException {
		FileOutputStream fileOut = new FileOutputStream(exportFolder + "export.xls");
        wb.write(fileOut);
        fileOut.close();
	}

}
