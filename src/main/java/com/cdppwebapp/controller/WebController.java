package com.cdppwebapp.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ch.qos.logback.core.recovery.ResilientSyslogOutputStream;

@CrossOrigin(origins = "http://localhost")
@RestController
@RequestMapping("/cell-devs")
public class WebController {
	
	String projectName = new String();
	String modelFile = new String();
	
	@Value("${log.folder}")
	private String logPath;
	
	
	@PostMapping("/upload")
	public ResponseEntity<Object> uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
		String root = System.getProperty("user.dir") + "\\src\\main\\resources";
		String filePath = root + "\\SIMU.exe";
		File dir = new File(root);
		String message = "";
		
		try {
			List<String> fileNames = new ArrayList<>();

			Arrays.asList(files).stream().forEach(file -> {
				System.out.println(file.getOriginalFilename());
				byte[] bytes;
				if(file.getOriginalFilename().contains(".ma")) {
					projectName = file.getOriginalFilename().split("\\.")[0];
					modelFile = file.getOriginalFilename();
				}
				try {
					bytes = file.getBytes();
					String s = new String(bytes, StandardCharsets.UTF_8);
					File fileObj = new File(root + "\\" + file.getOriginalFilename());
					fileObj.createNewFile();
					FileWriter writer = new FileWriter(root + "\\" + file.getOriginalFilename());
					writer.write(s);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			});

			message = "Uploaded the files successfully: " + fileNames;
		} catch (Exception e) {
			message = "Fail to upload files!";
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
		}
		if (new File(filePath).exists()) {
			try {

				ProcessBuilder pb = new ProcessBuilder(filePath, "-m"+modelFile, "-l"+projectName+".log", "-t00:01:00:000");
				pb.directory(dir);
				pb.redirectError();
				Process p = pb.start();
				InputStream is = p.getInputStream();
				int value = -1;
				while ((value = is.read()) != -1) {
					System.out.print((char) value);
				}

				int exitCode = p.waitFor();

				System.out.println(filePath + " exited with " + exitCode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println(filePath + " does not exist");
		}
		boolean visDir = new File(logPath+"\\"+projectName).mkdir();
		if(visDir) {
			File logFile = new File(root+"\\"+projectName+".log");
			File destLogFile = new File(logPath+"\\"+projectName+"\\messages.log");
			File maFile = new File(root+"\\"+projectName+".ma");
			File destMAFile = new File(logPath+"\\"+projectName+"\\model.ma");
			File palFile = new File(root+"\\"+projectName+".pal");
			File destPalFile = new File(logPath+"\\"+projectName+"\\style.pal");
			logFile.renameTo(destLogFile);
			maFile.renameTo(destMAFile);
			palFile.renameTo(destPalFile);
		}
		else {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Project Already exist");
		}
//		logFile.delete();
		return ResponseEntity.status(HttpStatus.OK).body(message);
	}
}
