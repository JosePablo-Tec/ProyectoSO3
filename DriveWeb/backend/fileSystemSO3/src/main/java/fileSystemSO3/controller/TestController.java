package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("Conexión exitosa con el backend");
    }
}
