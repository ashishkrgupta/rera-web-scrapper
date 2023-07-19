package com.site.scrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetaReader {
	
	public static Map<String, Object> getDisttMap(String filepath) throws StreamReadException, DatabindException, IOException {
		return new ObjectMapper().readValue(new FileInputStream(new File(filepath)), Map.class);
	}

}
