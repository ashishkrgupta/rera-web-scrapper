package com.site.scrapper;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class Licence {
	
	private final static String LICENSE_PROPERTY = "license";
	private final static String DATE_FORMAT = "yyyy-MM-dd";
	private final static int EN_DE_ITR = 5;
	
	private Properties props;
	private String licenseStr;
	
	public Licence(Properties props) {
		this.props = props;
		this.licenseStr = this.props.getProperty(LICENSE_PROPERTY);
	}
	
	public String validTill() {
		return this.decode(licenseStr);
	}
	
	public boolean isValid() {
		try {
			Date today = new SimpleDateFormat(DATE_FORMAT).parse(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
			String decodedDateStr = this.decode(licenseStr);
			Date expiryDate = new SimpleDateFormat(DATE_FORMAT).parse(decodedDateStr);
			return expiryDate.compareTo(today) >= 0;
		} catch (Exception e) {
			System.err.println(ExceptionUtils.getStackTrace(e));
			return false;
		}
	}
	
	private String encode(String decodedString) {
		String encoded = decodedString;
		Encoder encoder = Base64.getEncoder();
		for (int i = 0; i < EN_DE_ITR; i++) {
			encoded = encoder.encodeToString(encoded.getBytes());
		}
		return encoded;
	}
	
	private String decode(String encodedString) {
		byte[] decoded = encodedString.getBytes();
		Decoder decoder = Base64.getDecoder();
		for (int i = 0; i < EN_DE_ITR; i++) {
			decoded = decoder.decode(decoded);
		}
		return new String(decoded);
	}
	
	public static void main(String[] args) {
		Licence l = new Licence(null);
		String str = "2023-08-31";
		String encoded = l.encode(str);
		System.out.println(encoded);
		System.out.println(l.decode(encoded));
	}
	
}
