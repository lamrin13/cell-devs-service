package component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;

public class WebServiceGenerator {

	@Value("${cadmium.runtime.folder}")
	String root;
	private static WebServiceGenerator engine;
	private Template template = null;
	Map<String, Object> dataMap;

	private WebServiceGenerator(String templatePath) throws IOException {
		init(templatePath);
		
	}

	private void init(String templatePath) throws IOException {
		Version v = new Version("2.3.0");
		Configuration cfg = new Configuration(v);
		cfg.setDirectoryForTemplateLoading(new File(root));
//		cfg.setTemplateLoader(TemplateLoader);
		try {
			template = cfg.getTemplate(templatePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Execption from here");
			e.printStackTrace();
		}

	}

	public static WebServiceGenerator get(String templatePath) throws IOException {
		engine = new WebServiceGenerator(templatePath);
		return engine;
	}

	public WebServiceGenerator buildData(HashMap<String, Object> map) {
		dataMap = map;

		return engine;
	}

	public void writeFile(String filePath) {
		Writer file = null;
		try {
			file = new FileWriter(new File(filePath));
			template.process(dataMap, file);
			file.flush();
			System.out.println("Success");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}