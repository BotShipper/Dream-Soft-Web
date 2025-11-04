package dreamsoft.dreamsoftweb.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ProtocCompilerService {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "proto-converter\\";

    public void validateProto(String protoContent) throws Exception {
        if (protoContent == null || protoContent.trim().isEmpty()) {
            throw new Exception("Proto content không được để trống");
        }

        if (!protoContent.contains("syntax")) {
            throw new Exception("Thiếu khai báo 'syntax'. Ví dụ: syntax = \"proto3\";");
        }

        if (!protoContent.matches("(?s).*syntax\\s*=\\s*\"proto[23]\"\\s*;.*")) {
            throw new Exception("Syntax không hợp lệ. Phải là: syntax = \"proto2\"; hoặc syntax = \"proto3\";");
        }

        long openBraces = protoContent.chars().filter(ch -> ch == '{').count();
        long closeBraces = protoContent.chars().filter(ch -> ch == '}').count();
        if (openBraces != closeBraces) {
            throw new Exception("Số lượng dấu ngoặc {} không khớp");
        }

        if (!protoContent.contains("message") && !protoContent.contains("enum")) {
            throw new Exception("Proto file phải chứa ít nhất một message hoặc enum");
        }
    }

    public byte[] compileAndZip(String protoContent, String filename) throws Exception {
        validateProto(protoContent);

        String workDir = TEMP_DIR + UUID.randomUUID().toString() + "\\";
        new File(workDir).mkdirs();

        try {
            // Write proto file
            String protoFile = workDir + filename;
            Files.write(Paths.get(protoFile), protoContent.getBytes());

            // Create output directories
            String javaOutDir = workDir + "java\\";
            String csharpOutDir = workDir + "csharp\\";
            new File(javaOutDir).mkdirs();
            new File(csharpOutDir).mkdirs();

            // Check if protoc is installed
            if (!isProtocInstalled()) {
                throw new Exception("Protoc compiler chưa được cài đặt trên server. " +
                        "Vui lòng cài đặt protoc từ: https://github.com/protocolbuffers/protobuf/releases");
            }

            // Compile to Java
            executeProtoc(workDir, protoFile, javaOutDir, "java");

            // Compile to C#
            executeProtoc(workDir, protoFile, csharpOutDir, "csharp");

            // Create ZIP file
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // Add original proto file
            addToZip(zos, protoFile, filename);

            // Add Java files
            addDirectoryToZip(zos, new File(javaOutDir), "java/");

            // Add C# files
            addDirectoryToZip(zos, new File(csharpOutDir), "csharp/");

            // Add README
            String readme = generateReadme(filename);
            addTextToZip(zos, "README.txt", readme);

            zos.close();
            return baos.toByteArray();

        } finally {
            // Cleanup
            deleteDirectory(new File(workDir));
        }
    }

    private boolean isProtocInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("protoc --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            return line != null && line.contains("libprotoc");
        } catch (Exception e) {
            return false;
        }
    }

    private void executeProtoc(String workDir, String protoFile, String outDir, String language) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("protoc");
        command.add("--" + language + "_out=" + outDir);
        command.add("--proto_path=" + workDir);
        command.add(protoFile);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Lỗi khi compile " + language + ": " + output.toString());
        }

        // Check if files were generated
        File outDirFile = new File(outDir);
        if (!outDirFile.exists() || outDirFile.listFiles() == null || outDirFile.listFiles().length == 0) {
            throw new Exception("Không có file " + language + " nào được generate. Output: " + output.toString());
        }
    }

    private void addToZip(ZipOutputStream zos, String filePath, String zipEntryName) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return;

        zos.putNextEntry(new ZipEntry(zipEntryName));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    private void addDirectoryToZip(ZipOutputStream zos, File dir, String prefix) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, prefix + file.getName() + "/");
            } else {
                String entryName = prefix + file.getName();
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private void addTextToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private String generateReadme(String filename) {
        return "Proto Converter - Generated Files\n" +
                "=====================================\n\n" +
                "Generated on: " + new java.util.Date() + "\n" +
                "Original file: " + filename + "\n\n" +
                "Files included:\n" +
                "- " + filename + " (Original Proto file)\n" +
                "- java/ (Java generated files by protoc)\n" +
                "- csharp/ (C# generated files by protoc)\n\n" +
                "Generated using Google Protocol Buffers Compiler (protoc)\n" +
                "Tool: Proto Converter with Spring Boot\n\n" +
                "Note: These files are generated by the official protoc compiler\n" +
                "and include full Protocol Buffers functionality including:\n" +
                "- Serialization/Deserialization\n" +
                "- Builder pattern\n" +
                "- Wire format support\n" +
                "- All standard protobuf features\n";
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
