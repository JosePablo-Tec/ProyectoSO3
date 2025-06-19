package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping("/create")
    public ResponseEntity<String> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String currentDir = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
        String basePath = currentDir + username + ".json";
        System.out.println("Solicitud recibida para usuario: " + username); 

        try {
            Map<String, Object> estructura = new HashMap<>();
            estructura.put("nombre", username);
            estructura.put("estructura", new HashMap<>()); // Vacía al inicio
            Files.createDirectories(Paths.get(currentDir)); // Crea la carpeta si no existe
            FileWriter writer = new FileWriter(basePath);
            writer.write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(estructura));
            writer.close();
            System.out.println("Archivo creado en: " + basePath); 

            return ResponseEntity.ok("Usuario " + username + " creado correctamente.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al crear el usuario: " + e.getMessage());
        }
    }
}
