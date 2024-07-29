package org.openbpmn.bpmn.layout;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.util.BPMNModelFactory;

import io.process.analytics.tools.bpmn.generator.BPMNLayoutGenerator;
import io.process.analytics.tools.bpmn.generator.BPMNLayoutGenerator.ExportType;
import io.process.analytics.tools.bpmn.generator.internal.FileUtils;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
public class App {

	public static void main(String[] args) {
//		if (args.length ==2) {
        Path startPath = Paths.get("C:\\Users\\AliNourEldin\\Desktop\\nala2bpmn-piplines\\promoai\\utils\\evaluation\\results"); // Change this to your directory path
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//              //  	 DELELE ALL LAYOUTED
//                	 if (file.toString().endsWith("_layout.bpmn")) {
//                		 Path fileToDelete = Paths.get(file.toString());
//                		 Files.delete(fileToDelete);
//                	 }	
                    if (file.toString().endsWith(".bpmn")) {
                        Path newFile = Paths.get(file.toString().replace(".bpmn", "_layout.bpmn"));
                        
                        try {
            				String bpmn_path = file.toString();
            				String output_path = newFile.toString();
            				System.out.println(bpmn_path);
            				System.out.println(output_path);
            				File inputFile = new File(bpmn_path);

            				String output;
            				BPMNLayoutGenerator bpmnLayoutGenerator = new BPMNLayoutGenerator();
            				output = bpmnLayoutGenerator.generateLayoutFromBPMNSemantic(FileUtils.fileContent(inputFile), ExportType.valueOf("BPMN"));

            				File outputFile = new File(output_path);
            				FileUtils.touch(outputFile);
            				Files.write(outputFile.toPath(), output.getBytes());

            			} catch (Exception e) {
            				// TODO Auto-generated catch block
            				e.printStackTrace();
            			}
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
			
//		} else {
//			System.out.println("[path, output_path]");
//		}
	}
}
