package dreamsoft.dreamsoftweb.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ProtocService {

    private Path extractProtoc() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        String exeName = isWindows ? "protoc.exe" : "protoc";

        // 1. Thử lấy từ biến môi trường trước
        String protocEnv = System.getenv("PROTOC_PATH");
        if (protocEnv != null && !protocEnv.isEmpty()) {
            Path protocPath = Paths.get(protocEnv);
            if (Files.exists(protocPath)) {
                return protocPath;
            }
        }

        // 2. Thử tìm trong system PATH
        String[] systemPaths;
        if (isWindows) {
            systemPaths = new String[]{
                    "C:\\protoc\\bin\\protoc.exe",
                    System.getenv("PROGRAMFILES") + "\\protoc\\bin\\protoc.exe"
            };
        } else {
            systemPaths = new String[]{
                    "/usr/local/bin/protoc",
                    "/usr/bin/protoc",
                    "/opt/protoc/bin/protoc",
                    System.getProperty("user.home") + "/bin/protoc"
            };
        }

        for (String path : systemPaths) {
            if (path != null) {
                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    return p;
                }
            }
        }

        // 3. Fallback: load từ resources (cho trường hợp đóng gói trong JAR)
        InputStream is = getClass().getResourceAsStream("/protoc/bin/" + exeName);
        if (is == null) {
            throw new IllegalStateException("❌ protoc not found! Please:\n" +
                    "- Set PROTOC_PATH environment variable, or\n" +
                    "- Install protoc in system PATH, or\n" +
                    "- Include protoc binary in resources/protoc/bin/");
        }

        // Extract to temp file
        Path tmp = Files.createTempFile("protoc-", exeName);
        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
        tmp.toFile().setExecutable(true);
        return tmp;
    }

    public Path generate(MultipartFile uploadedFile, String prototext, String lang) throws Exception {
        Path protocPath = extractProtoc();

        Path tmpDir = Files.createTempDirectory("protogen-");
        Path protoFile = tmpDir.resolve("upload.proto");

        if (uploadedFile != null && !uploadedFile.isEmpty()) {
            try (InputStream in = uploadedFile.getInputStream()) {
                Files.copy(in, protoFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } else if (prototext != null && !prototext.isBlank()) {
            Files.write(protoFile, prototext.getBytes());
        } else {
            throw new IllegalArgumentException("❌ No proto provided");
        }

        Path outDir = tmpDir.resolve("out");
        Files.createDirectories(outDir);

        List<String> baseCmd = new ArrayList<>();
        baseCmd.add(protocPath.toString());
        baseCmd.add("--proto_path=" + tmpDir);

        if ("java".equalsIgnoreCase(lang) || "both".equalsIgnoreCase(lang)) {
            Path javaOut = outDir.resolve("java");
            Files.createDirectories(javaOut);
            runProtoc(baseCmd, protoFile, javaOut, "--java_out=");
        }

        if ("csharp".equalsIgnoreCase(lang) || "both".equalsIgnoreCase(lang)) {
            Path csOut = outDir.resolve("csharp");
            Files.createDirectories(csOut);
            runProtoc(baseCmd, protoFile, csOut, "--csharp_out=");
        }

        Path zip = tmpDir.resolve("generated.zip");
        zipFolder(outDir, zip);

        return zip;
    }

    public String previewGeneratedCode(String prototext, String lang) throws Exception {
        if (prototext == null || prototext.isBlank()) return "❌ No proto provided";

        Path protocPath = extractProtoc();
        Path tmpDir = Files.createTempDirectory("protogen-preview-");
        Path protoFile = tmpDir.resolve("upload.proto");
        Files.write(protoFile, prototext.getBytes());

        Path outDir = tmpDir.resolve("out");
        Files.createDirectories(outDir);

        List<String> baseCmd = List.of(protocPath.toString(), "--proto_path=" + tmpDir);

        if ("java".equalsIgnoreCase(lang)) {
            Path javaOut = outDir.resolve("java");
            Files.createDirectories(javaOut);
            runProtoc(baseCmd, protoFile, javaOut, "--java_out=");
            return readFiles(javaOut);
        } else if ("csharp".equalsIgnoreCase(lang)) {
            Path csOut = outDir.resolve("csharp");
            Files.createDirectories(csOut);
            runProtoc(baseCmd, protoFile, csOut, "--csharp_out=");
            return readFiles(csOut);
        }
        return "❌ Unsupported language: " + lang;
    }

    private void runProtoc(List<String> baseCmd, Path protoFile, Path outDir, String outFlag) throws Exception {
        List<String> cmd = new ArrayList<>(baseCmd);
        cmd.add(outFlag + outDir);
        cmd.add(protoFile.toString());
        runProcess(cmd);
    }

    private void runProcess(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            is.transferTo(baos);
        }

        if (p.waitFor() != 0) {
            throw new IllegalStateException("❌ protoc failed:\n" + baos);
        }
    }

    private void zipFolder(Path source, Path zipFile) throws IOException {
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(source).filter(f -> !Files.isDirectory(f)).forEach(path -> {
                try {
                    zs.putNextEntry(new ZipEntry(source.relativize(path).toString().replace("\\", "/")));
                    Files.copy(path, zs);
                    zs.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private String readFiles(Path dir) throws IOException {
        List<Path> files = Files.walk(dir)
                .filter(f -> !Files.isDirectory(f) && (f.toString().endsWith(".java") || f.toString().endsWith(".cs")))
                .collect(Collectors.toList());

        if (files.isEmpty()) return "(no generated files)";
        StringBuilder sb = new StringBuilder();

        for (Path f : files) {
            sb.append("// file: ").append(dir.relativize(f)).append("\n")
                    .append(Files.readString(f)).append("\n\n");
        }
        return sb.toString();
    }
}
