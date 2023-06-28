package com.cdppwebapp.controller;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import component.FilesResponse;
import component.Model;
import component.WebServiceGenerator;
import component.ZipFile;

@CrossOrigin(origins = "http://localhost:82")
@RestController
@RequestMapping("/cadmium")
public class CadmiumController {

	@Value("${cadmium.runtime.folder}")
	String root;

	@Value("${python.path}")
	String python;

	@SuppressWarnings("unchecked")
	@PostMapping("/generate")
	public ResponseEntity<byte[]> generate(@RequestBody Model data) throws IOException, InterruptedException,
			IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		String projectID = UUID.randomUUID().toString();
		String projectDir = root + "/" + projectID;
		File dir = new File(projectDir);
		dir.mkdir();
		Files.copy(Paths.get(root + "/model_atomic.hpp"), Paths.get(dir.getAbsolutePath() + "/model_atomic.hpp"),
				REPLACE_EXISTING);
		Files.copy(Paths.get(root + "/model_coupled.hpp"), Paths.get(dir.getAbsolutePath() + "/model_coupled.hpp"));
		Files.copy(Paths.get(root + "/iestream.hpp"), Paths.get(dir.getAbsolutePath() + "/iestream.hpp"));

		File simplifiedModel = new File(projectDir + "/simplifiedModel.json");
		FileWriter writer = new FileWriter(simplifiedModel);
		ObjectWriter ow = new ObjectMapper().writer();
		writer.write(ow.writeValueAsString(data));
		writer.close();
		for (Object atomic : data.getAtomic()) {
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) atomic;
//			String a = ow.writeValueAsString(atomic).replaceAll("\"", "\\\\\"");
			String a = ow.writeValueAsString(atomic);
			generateCode(a, projectID, map.get("name") + ".hpp", "atomic");
		}
		for (Object coupled : data.getCoupled()) {
			LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) coupled;
//			String c = ow.writeValueAsString(coupled).replaceAll("\"", "\\\\\"");
			String c = ow.writeValueAsString(coupled);
			generateCode(c, projectID, map.get("name") + ".hpp", "coupled");
		}
		HashMap<String, Object> map = (HashMap<String, Object>) data.getTop();
		String absolutePath = "D:\\cog\\run-time" + "\\main.ftlh";
		writeMain(map, "main.ftlh", projectDir + "/main.cpp");
		String cmakePath = "D:\\cog\\run-time" + "\\cmake.ftlh";
		writeCMake(projectID, "cmake.ftlh", projectDir + "/CMakeLists.txt");

		return processOutput(projectID);
	}

	public void writeMain(HashMap<String, Object> map, String templatePath, String filePath) throws IOException {
		WebServiceGenerator.get(templatePath).buildData(map).writeFile(filePath);
	}

	public void writeCMake(String projectID, String templatePath, String filePath) throws IOException {
		HashMap<String, Object> map = new HashMap<>();
		map.put("project", projectID);
		map.put("CMAKE_CURRENT_SOURCE_DIR", "${CMAKE_CURRENT_SOURCE_DIR}");
		WebServiceGenerator.get(templatePath).buildData(map).writeFile(filePath);

	}

	private boolean generateCode(String data, String projectID, String modelName, String modelType)
			throws IOException, InterruptedException {

		File dir = new File(root + "/" + projectID);

		List<String> command = new ArrayList<String>(
				Arrays.asList(python, "-m", "cogapp", "-D", "fileData="+ data, "-o", modelName));
//		List<String> command = new ArrayList<String>(
//				Arrays.asList("/usr/bin/python3", "-m", "cogapp", "-D", "fileData="+ data, "-o", modelName));
		String modelfile = modelType == "atomic" ? "model_atomic.hpp" : "model_coupled.hpp";
		command.add(modelfile);
		ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
		pb.directory(dir);
		pb.redirectError();

		Process p = pb.start();
		InputStream is = p.getInputStream();
		int value = -1;
		String out = "";
		while ((value = is.read()) != -1) {
			System.out.print((char) value);
			out += (char) value;
		}
		int exitCode = p.waitFor();
		return exitCode == 0;
	}

	private ResponseEntity<byte[]> processOutput(String projectID) throws IOException {
		File output = new File(root + "/" + projectID);
		List<File> filelist = new ArrayList<File>();
		FileFilter modelFileFilter = (file) -> {
			return !file.getName().contains("model") || file.getName().contains("top_model");
		};
		for (File f : output.listFiles(modelFileFilter)) {
			byte[] raw = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
			String clean = new String(raw, StandardCharsets.UTF_8).replaceAll("/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/|//.*",
					"");
			FileWriter writer = new FileWriter(f);
			writer.write(clean);
			writer.close();
			filelist.add(f);
		}
		ZipFile zf = new ZipFile(filelist);
		FileUtils.deleteDirectory(output);
		return new FilesResponse(projectID + ".zip", zf.toByteArray()).response;
	}

}
