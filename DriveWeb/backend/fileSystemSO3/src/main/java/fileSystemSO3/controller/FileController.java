package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import fileSystemSO3.util.EspacioUtils;

@RestController
@RequestMapping("/api/user")
public class FileController {

    @PostMapping("/upload")
  public ResponseEntity<String> subirArchivo(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String nombre = body.get("nombreArchivo");
    String extension = body.get("extension");
    String contenido = body.get("contenido");
    String ruta = body.get("ruta");

    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String jsonStr = Files.readString(Paths.get(pathJson));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenidoActual = (List<Map<String, Object>>) actual.get("contenido");

      int tamanoNuevo = contenido.length();
      int espacioOcupado = EspacioUtils.calcularEspacio(estructura);
      int tamanoMax = (int) usuario.get("tamanoTotal");

      Map<String, Object> archivoExistente = null;
      for (Map<String, Object> item : contenidoActual) {
        if ("archivo".equals(item.get("tipo")) &&
            nombre.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))) {
          archivoExistente = item;
          break;
        }
      }

      if (archivoExistente != null) {
        int tamanoAnterior = (int) archivoExistente.get("tamano");
        int nuevoUso = espacioOcupado - tamanoAnterior + tamanoNuevo;
        if (nuevoUso > tamanoMax) {
          return ResponseEntity.badRequest().body("Espacio insuficiente para reemplazar el archivo.");
        }
        contenidoActual.remove(archivoExistente);
      } else {
        if (espacioOcupado + tamanoNuevo > tamanoMax) {
          return ResponseEntity.badRequest().body("Espacio insuficiente. No se puede subir el archivo.");
        }
      }

      Map<String, Object> archivoNuevo = new HashMap<>();
      archivoNuevo.put("tipo", "archivo");
      archivoNuevo.put("nombre", nombre);
      archivoNuevo.put("extension", extension);
      archivoNuevo.put("contenido", contenido);
      archivoNuevo.put("fechaCreacion", LocalDateTime.now().toString());
      archivoNuevo.put("fechaModificacion", LocalDateTime.now().toString());
      archivoNuevo.put("tamano", tamanoNuevo);

      contenidoActual.add(archivoNuevo);

      mapper.writeValue(Paths.get(pathJson).toFile(), usuario);

      return ResponseEntity.ok("Archivo subido correctamente.");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
    }
  }

  
}
