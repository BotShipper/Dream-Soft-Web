package dreamsoft.dreamsoftweb.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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

        // 3. Fallback: load từ resources
        InputStream is = getClass().getResourceAsStream("/protoc/bin/" + exeName);
        if (is == null) {
            throw new IllegalStateException("❌ protoc not found! Please:\n" +
                    "- Set PROTOC_PATH environment variable, or\n" +
                    "- Install protoc in system PATH, or\n" +
                    "- Include protoc binary in resources/protoc/bin/");
        }

        Path tmp = Files.createTempFile("protoc-", exeName);
        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
        tmp.toFile().setExecutable(true);
        return tmp;
    }

    // Method mới: tìm protogen (protobuf-net)
    private Path findProtogen() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        String exeName = isWindows ? "protogen.exe" : "protogen";

        // Tìm trong PATH system
        String[] systemPaths;
        if (isWindows) {
            String userProfile = System.getenv("USERPROFILE");
            String localAppData = System.getenv("LOCALAPPDATA");

            systemPaths = new String[]{
                    userProfile + "\\.dotnet\\tools\\protogen.exe",
                    "C:\\Program Files\\dotnet\\tools\\protogen.exe",
                    localAppData + "\\Microsoft\\dotnet\\tools\\protogen.exe",
                    "C:\\protobuf-net\\protogen.exe"
            };
        } else {
            String home = System.getProperty("user.home");
            systemPaths = new String[]{
                    home + "/.dotnet/tools/protogen",
                    "/usr/local/bin/protogen",
                    "/usr/bin/protogen"
            };
        }

        for (String path : systemPaths) {
            if (path != null) {
                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    System.out.println("✅ Found protogen at: " + p);
                    return p;
                }
            }
        }

        throw new IllegalStateException("❌ protogen not found! Install it via:\n" +
                "dotnet tool install -g protobuf-net.Protogen\n" +
                "Then restart your application.");
    }

    // Method chính với option chọn compiler
    public Path generate(MultipartFile uploadedFile, String prototext, String lang,
                         CSharpCompiler csharpCompiler) throws Exception {
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

        // Generate Java
        if ("java".equalsIgnoreCase(lang) || "both".equalsIgnoreCase(lang)) {
            Path javaOut = outDir.resolve("java");
            Files.createDirectories(javaOut);
            runProtoc(baseCmd, protoFile, javaOut, "--java_out=");
        }

        // Generate C# với compiler được chọn
        if ("csharp".equalsIgnoreCase(lang) || "both".equalsIgnoreCase(lang)) {
            Path csOut = outDir.resolve("csharp");
            Files.createDirectories(csOut);

            if (csharpCompiler == CSharpCompiler.PROTOBUF_NET) {
                runProtobufNet(protoFile, csOut);
            } else {
                runProtoc(baseCmd, protoFile, csOut, "--csharp_out=");
            }
        }

        Path zip = tmpDir.resolve("generated.zip");
        zipFolder(outDir, zip);

        return zip;
    }

    // Overload để giữ tương thích với code cũ (mặc định dùng protobuf-net)
    public Path generate(MultipartFile uploadedFile, String prototext, String lang) throws Exception {
        return generate(uploadedFile, prototext, lang, CSharpCompiler.PROTOBUF_NET);
    }

    // Method mới: generate C# với protobuf-net
    private void runProtobufNet(Path protoFile, Path outDir) throws Exception {
        Path protogenPath = findProtogen();

        // protogen cần relative path, không phải absolute path
        List<String> cmd = new ArrayList<>();
        cmd.add(protogenPath.toString());
        cmd.add("--csharp_out=" + outDir.toAbsolutePath().toString());

        // ❌ KHÔNG DÙNG: cmd.add(protoFile.toAbsolutePath().toString());
        // ✅ DÙNG: chỉ tên file, và chạy từ thư mục chứa file
        cmd.add(protoFile.getFileName().toString());

        System.out.println("🔧 Running: " + String.join(" ", cmd));

        try {
            // Set working directory là thư mục chứa file .proto
            runProcess(cmd, protoFile.getParent());
            System.out.println("✅ Generated C# files in: " + outDir);
        } catch (Exception e) {
            System.err.println("❌ protogen failed: " + e.getMessage());
            throw e;
        }
    }

    private void runProcess(List<String> cmd, Path workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            is.transferTo(baos);
        }

        int exitCode = p.waitFor();
        String output = baos.toString();

        if (!output.isEmpty()) {
            System.out.println("📋 Process output:\n" + output);
        }

        if (exitCode != 0) {
            throw new IllegalStateException("❌ Command failed with exit code " + exitCode + ":\n" + output);
        }
    }

    public String previewGeneratedCode(String prototext, String lang,
                                       CSharpCompiler csharpCompiler) throws Exception {
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

            if (csharpCompiler == CSharpCompiler.PROTOBUF_NET) {
                runProtobufNet(protoFile, csOut);
            } else {
                runProtoc(baseCmd, protoFile, csOut, "--csharp_out=");
            }
            return readFiles(csOut);
        }
        return "❌ Unsupported language: " + lang;
    }

    // Overload - mặc định dùng protobuf-net
    public String previewGeneratedCode(String prototext, String lang) throws Exception {
        return previewGeneratedCode(prototext, lang, CSharpCompiler.PROTOBUF_NET);
    }

    private void runProtoc(List<String> baseCmd, Path protoFile, Path outDir, String outFlag) throws Exception {
        List<String> cmd = new ArrayList<>(baseCmd);
        cmd.add(outFlag + outDir);
        cmd.add(protoFile.toString());
        runProcess(cmd);
    }

    private void runProcess(List<String> cmd) throws Exception {
        runProcess(cmd, null);
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

    // Thêm enum để chọn C# compiler
    public enum CSharpCompiler {
        GOOGLE_PROTOC,      // protoc --csharp_out (mặc định)
        PROTOBUF_NET        // protogen (protobuf-net)
    }
}