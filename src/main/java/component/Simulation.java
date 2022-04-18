package component;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class Simulation {

	public void writeFiles(MultipartFile[] files, String root, String macro) {
		try {
			File projectDir = new File(root);
			projectDir.mkdir();
			Arrays.asList(files).stream().forEach(file -> {
				System.out.println(file.getOriginalFilename());
				byte[] bytes;
				try {
					File fileObj;
					bytes = file.getBytes();
					String s = new String(bytes, StandardCharsets.UTF_8);
					if (file.getOriginalFilename().contains(".ma")) {
						fileObj = new File(root + "/model.ma");
					} else if (file.getOriginalFilename().contains(".pal")) {
						fileObj = new File(root + "/style.pal");
					} else if (file.getOriginalFilename().contains(".inc")) {
						fileObj = new File(root + "/" + macro);
					}
					else if(file.getOriginalFilename().contains(".stvalues")) {
						fileObj = new File(root + "/" + file.getOriginalFilename());
					}
					else {
						fileObj = new File(root + "/initial.val");
					}
					
					fileObj.createNewFile();
					FileWriter writer = new FileWriter(root + "/" + fileObj.getName());
					writer.write(s);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ResponseEntity<byte[]> runLinux(String root, String projectID, String simTime, boolean debug)
			throws IOException {
		String filePath = root + "/cd++";
		String projectDir = root + "/" + projectID;
		File dir = new File(projectDir);
		byte[] output = null;
		ContentDisposition disposition = ContentDisposition.attachment().filename("response.zip").build();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
		httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
		if (new File(filePath).exists()) {
			try {
				List<String> command = new ArrayList<String>(
						Arrays.asList(filePath, "-mmodel.ma", "-lmessages.log", "-t00:" + simTime + ":000"));
				if (debug) {
					command.add("-pparse.log");
					command.add("-vdebug.log");
				}
				ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
				;
				pb.directory(dir);
				pb.redirectError();
				Process p = pb.start();
				InputStream is = p.getInputStream();
				int value = -1;
				File stdout = new File(projectDir + "/stdout.log");
				stdout.createNewFile();
				FileWriter writer = new FileWriter(stdout);
				while ((value = is.read()) != -1) {
					System.out.print((char) value);
					writer.append((char) value);
				}
				writer.close();
				System.out.println("File written");
				int exitCode = p.waitFor();

				System.out.println(filePath + " exited with " + exitCode);
				if (exitCode == 0) {
					File logFile = new File(projectDir + "/messages.log");
					File tempLogs = new File(projectDir + "/messages.log01");
					if (!tempLogs.exists()) {
						List<File> filelist = new ArrayList<File>();
						for (File file : dir.listFiles()) {
							filelist.add(file);
						}
						ZipFile zf = new ZipFile(filelist);
						FileUtils.deleteDirectory(dir);
						return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
					}
					Files.copy(Paths.get(tempLogs.getAbsolutePath()), Paths.get(logFile.getAbsolutePath()),
							REPLACE_EXISTING);
					removeDuplicates(dir);
					output = Files.readAllBytes(Paths.get(logFile.getAbsolutePath()));
					List<File> filelist = new ArrayList<File>();

					for (File file : dir.listFiles()) {
						filelist.add(file);
					}
					ZipFile zf = new ZipFile(filelist);
					FilesResponse fr = new FilesResponse("response.zip", zf.toByteArray());
					if (Files.size(Paths.get(logFile.getAbsolutePath())) == 0) {
						FileUtils.deleteDirectory(dir);
						return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
					}
					FileUtils.deleteDirectory(dir);
					return fr.response;
				} else {
					removeDuplicates(dir);
					List<File> filelist = new ArrayList<File>();
					for (File file : dir.listFiles()) {
						filelist.add(file);
					}
					ZipFile zf = new ZipFile(filelist);
					FileUtils.deleteDirectory(dir);
					return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
				}
			} catch (Exception e) {
				output = ("Error while running simulation " + e.getMessage()).getBytes();
			}
		} else {
			System.err.println(filePath + " does not exist");
		}
		return ResponseEntity.badRequest().headers(httpHeaders).body(output);
	}

	public ResponseEntity<byte[]> runWindows(String root, String projectID, String simTime, boolean debug)
			throws IOException {
		String filePath = root + "/SIMU.EXE";
		String projectDir = root + "/" + projectID;
		File dir = new File(projectDir);
		byte[] output = "Simulation could not run".getBytes();
		ContentDisposition disposition = ContentDisposition.attachment().filename("response.zip").build();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
		httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
		if (new File(filePath).exists()) {
			try {
				List<String> command = new ArrayList<String>(
						Arrays.asList("wine", filePath, "-mmodel.ma", "-lmessages.log", "-t00:" + simTime + ":000"));
				if (debug) {
					command.add("-pparse.log");
					command.add("-vdebug.log");
				}
				ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true);
				;
				System.out.println(command.toString());
				pb.command(command);
				pb.directory(dir);
				pb.redirectError();
				Process p = pb.start();
				InputStream is = p.getInputStream();
				int value = -1;
				File stdout = new File(projectDir + "/stdout.log");
				stdout.createNewFile();
				FileWriter writer = new FileWriter(stdout);
				while ((value = is.read()) != -1) {
					System.out.print((char) value);
					writer.append((char) value);
				}
				writer.close();
				int exitCode = p.waitFor();
				System.out.println(exitCode);
				if (exitCode == 0) {
					File logFile = new File(projectDir + "/messages.log");
					output = Files.readAllBytes(Paths.get(logFile.getAbsolutePath()));
					File[] listFiles = dir.listFiles();
					List<File> filelist = new ArrayList<File>();
					for (File file : listFiles) {
						filelist.add(file);
					}
					ZipFile zf = new ZipFile(filelist);
					FilesResponse fr = new FilesResponse("response.zip", zf.toByteArray());
					if (Files.size(Paths.get(logFile.getAbsolutePath())) == 0) {
						return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
					}
					FileUtils.deleteDirectory(dir);
					return fr.response;
				} else {
					List<File> filelist = new ArrayList<File>();
					for (File file : dir.listFiles()) {
						filelist.add(file);
					}
					ZipFile zf = new ZipFile(filelist);
					FileUtils.deleteDirectory(dir);
					return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
				}
			} catch (Exception e) {
				List<File> filelist = new ArrayList<File>();
				for (File file : dir.listFiles()) {
					filelist.add(file);
				}
				ZipFile zf = new ZipFile(filelist);
				FileUtils.deleteDirectory(dir);
				return ResponseEntity.badRequest().headers(httpHeaders).body(zf.toByteArray());
			}

		} else {
			output = (filePath + " does not exist").getBytes();
		}
		return ResponseEntity.badRequest().headers(httpHeaders).body(output);
	}

	private void removeDuplicates(File dir) {
		final File[] outputFiles = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.matches("messages\\.log[0-9]+");
			}
		});
		for (final File file : outputFiles) {
			if (!file.delete()) {
				System.err.println("Can't remove " + file.getAbsolutePath());
			}
		}
	}
}
