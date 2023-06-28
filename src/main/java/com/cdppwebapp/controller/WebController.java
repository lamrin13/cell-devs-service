package com.cdppwebapp.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import component.Simulation;
import component.FilesResponse;
import component.ZipFile;

@CrossOrigin(origins = "http://localhost")
@RestController
@RequestMapping("/cell-devs")
public class WebController {

	String projectName = new String();
	String modelFile = new String();

	@Value("${log.folder}")
	private String logPath;

	@Value("${runtime.folder}")
	private String root;

	@PostMapping("/upload")
	public ResponseEntity<byte[]> uploadFiles(@RequestParam("files") MultipartFile[] files,
			@RequestParam("simTime") String simTime, @RequestParam(required = false) String macro,
			@RequestParam(required = false) String simulator,
			@RequestParam(required = false, defaultValue = "false") boolean debug) throws IOException {

		Simulation simulation = new Simulation();
		UUID uuid = UUID.randomUUID();
		String projectID = uuid.toString();
		simulation.writeFiles(files, root + "/" + projectID, macro);

		ResponseEntity<byte[]> output = simulator.equals("santi") ? simulation.runLinux(root, projectID, simTime, debug)
				: simulation.runWindows(root, projectID, simTime, debug);
		return output;
	}

	@GetMapping("projects")
	public ResponseEntity<Object> getProjects() {
		String projects[] = (new File(logPath)).list();
		return ResponseEntity.status(HttpStatus.OK).body(projects);
	}

	@GetMapping("project/{projectName}")
	public ResponseEntity<byte[]> getProjectFiles(@PathVariable String projectName) throws IOException {
		List<File> fileList = new ArrayList<File>();
		File f = new File(logPath + "/" + projectName);
		for (String s : f.list()) {
			fileList.add(new File(logPath + "/" + projectName + "/" + s));
		}
		ZipFile zf = new ZipFile(fileList);
		FilesResponse fr = new FilesResponse(projectName + ".zip", zf.toByteArray());
		return fr.response;
	}
}
