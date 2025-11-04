package dreamsoft.dreamsoftweb.controller;

import dreamsoft.dreamsoftweb.service.ProtocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/proto")
@RequiredArgsConstructor
public class ProtocController {

    private final ProtocService protocService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(
            @RequestParam(value = "prototext", required = false) String prototext,
            @RequestParam(value = "protofile", required = false) MultipartFile protofile,
            @RequestParam(value = "lang", defaultValue = "java") String lang) throws Exception {

        Path zip = protocService.generate(protofile, prototext, lang);
        byte[] content = Files.readAllBytes(zip);
        String fileName = zip.getFileName().toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"generated.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @PostMapping(path = "/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> preview(@RequestParam(value = "prototext", required = false) String prototext,
                                       @RequestParam(value = "lang", defaultValue = "java") String lang) throws Exception {
        String content = protocService.previewGeneratedCode(prototext, lang);
        return Map.of("code", content);
    }
}
