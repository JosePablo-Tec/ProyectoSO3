package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

  @PostMapping("/create")
  public ResponseEntity<String> createUser(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String sizeStr = body.get("size");
    String currentDir = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String basePath = currentDir + username + ".json";

    try {
      int size = Integer.parseInt(sizeStr);
      if (size <= 0) return ResponseEntity.badRequest().body("El tamaño debe ser mayor a cero.");

      Map<String, Object> raiz = new HashMap<>();
      raiz.put("tipo", "directorio");
      raiz.put("nombre", "raiz");
      raiz.put("contenido", new ArrayList<>());

      Map<String, Object> compartida = new HashMap<>();
      compartida.put("tipo", "directorio");
      compartida.put("nombre", "compartida");
      compartida.put("contenido", new ArrayList<>());

      Map<String, Object> estructura = new HashMap<>();
      estructura.put("raiz", raiz);
      estructura.put("compartida", compartida);

      Map<String, Object> usuario = new HashMap<>();
      usuario.put("nombre", username);
      usuario.put("tamanoTotal", size);
      usuario.put("fechaCreacion", LocalDateTime.now().toString());
      usuario.put("estructura", estructura);

      Files.createDirectories(Paths.get(currentDir));

      FileWriter writer = new FileWriter(basePath);
      writer.write(new ObjectMapper().writeValueAsString(usuario));
      writer.close();

      return ResponseEntity.ok("Usuario " + username + " creado correctamente.");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
  }
  
  @GetMapping("/{username}")
  public ResponseEntity<?> getUserDrive(@PathVariable String username) {
    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String contenido = Files.readString(Paths.get(filePath));
      return ResponseEntity.ok().body(contenido);
    } catch (IOException e) {
      return ResponseEntity.status(404).body("Usuario no encontrado");
    }
  }

  @PostMapping("/upload")
  public ResponseEntity<String> subirArchivo(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String nombre = body.get("nombreArchivo");
    String extension = body.get("extension");
    String contenido = body.get("contenido");
    String ruta = body.get("ruta");

    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";

    try {
      String jsonStr = Files.readString(Paths.get(pathJson));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

      Map<String, Object> actual = obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null) return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenidoActual = (List<Map<String, Object>>) actual.get("contenido");

      Map<String, Object> archivoNuevo = new HashMap<>();
      archivoNuevo.put("tipo", "archivo");
      archivoNuevo.put("nombre", nombre);
      archivoNuevo.put("extension", extension);
      archivoNuevo.put("contenido", contenido);
      archivoNuevo.put("fechaCreacion", LocalDateTime.now().toString());
      archivoNuevo.put("fechaModificacion", LocalDateTime.now().toString());
      archivoNuevo.put("tamano", contenido.length());

      contenidoActual.add(archivoNuevo);

      FileWriter writer = new FileWriter(pathJson);
      writer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(usuario));
      writer.close();

      return ResponseEntity.ok("Archivo subido correctamente.");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
    }
  }

  @PostMapping("/ruta")
  public ResponseEntity<?> getContenidoRuta(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String ruta = body.get("ruta");

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
    try {
      String contenidoJSON = Files.readString(Paths.get(basePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null) return ResponseEntity.badRequest().body("Ruta no encontrada");

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

    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";

    try {
      String contenidoJSON = Files.readString(Paths.get(filePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null) return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");

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

  private Map<String, Object> obtenerDirectorioDesdeRuta(Map<String, Object> estructura, String ruta) {
    String[] partes = ruta.replaceFirst("/", "").split("/");
    Map<String, Object> actual = (Map<String, Object>) estructura.get(partes[0]);

    for (int i = 1; i < partes.length; i++) {
      List<Map<String, Object>> hijos = (List<Map<String, Object>>) actual.get("contenido");
      boolean encontrado = false;
      for (Map<String, Object> hijo : hijos) {
        if (hijo.get("nombre").equals(partes[i]) && "directorio".equals(hijo.get("tipo"))) {
          actual = hijo;
          encontrado = true;
          break;
        }
      }
      if (!encontrado) return null;
    }
    return actual;
  }
}
