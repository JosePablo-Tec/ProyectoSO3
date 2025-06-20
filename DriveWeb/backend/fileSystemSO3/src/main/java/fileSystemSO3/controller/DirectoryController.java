package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import fileSystemSO3.util.EspacioUtils;


@RestController
@RequestMapping("/api/user")

public class DirectoryController {
  @PostMapping("/ruta")
  public ResponseEntity<?> getContenidoRuta(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String ruta = body.get("ruta");

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";
    try {
      String contenidoJSON = Files.readString(Paths.get(basePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta no encontrada");

      List<Map<String, Object>> resultado = (List<Map<String, Object>>) actual.get("contenido");
      return ResponseEntity.ok(resultado);
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al acceder al usuario.");
    }
  }

  @PostMapping("/mkdir")
  public ResponseEntity<String> crearDirectorio(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String nombreDirectorio = body.get("nombreDirectorio");
    String ruta = body.get("ruta");

    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String contenidoJSON = Files.readString(Paths.get(filePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");

      contenido.removeIf(d -> d.get("tipo").equals("directorio") && d.get("nombre").equals(nombreDirectorio));

      Map<String, Object> nuevoDirectorio = new HashMap<>();
      nuevoDirectorio.put("tipo", "directorio");
      nuevoDirectorio.put("nombre", nombreDirectorio);
      nuevoDirectorio.put("contenido", new ArrayList<>());

      contenido.add(nuevoDirectorio);

      mapper.writeValue(Paths.get(filePath).toFile(), json);

      return ResponseEntity.ok("Directorio creado con éxito.");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al crear el directorio: " + e.getMessage());
    }
  }

}
