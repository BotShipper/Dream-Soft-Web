package dreamsoft.dreamsoftweb.controller;

import dreamsoft.dreamsoftweb.service.ProtocCompilerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proto")
@RequiredArgsConstructor
public class ProtocCompilerController {

    private final ProtocCompilerService protocService;

    @PostMapping("/convert")
    public ResponseEntity<?> convertProto(@RequestBody Map<String, String> request) {
        try {
            String protoContent = request.get("protoContent");
            if (protoContent == null || protoContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Proto content không được để trống"));
            }

            byte[] zipFile = protocService.compileAndZip(protoContent, "Generated.proto");

            ByteArrayResource resource = new ByteArrayResource(zipFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=proto_generated.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAndConvert(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File không được để trống"));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.endsWith(".proto") && !originalFilename.endsWith(".txt"))) {
                return ResponseEntity.badRequest().body(createErrorResponse("Chỉ chấp nhận file .proto hoặc .txt"));
            }

            String protoContent = new String(file.getBytes());
            byte[] zipFile = protocService.compileAndZip(protoContent, originalFilename);

            ByteArrayResource resource = new ByteArrayResource(zipFile);

            String zipFilename = originalFilename.replace(".proto", "").replace(".txt", "") + "_generated.zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFilename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateProto(@RequestBody Map<String, String> request) {
        try {
            String protoContent = request.get("protoContent");
            protocService.validateProto(protoContent);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Proto file hợp lệ");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
