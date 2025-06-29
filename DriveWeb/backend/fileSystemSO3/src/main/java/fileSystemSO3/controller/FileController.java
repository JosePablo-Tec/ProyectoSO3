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


  @PostMapping("/share")
public ResponseEntity<String> compartirArchivo(@RequestBody Map<String, String> body) {
    String usuarioOrigen = body.get("usuarioOrigen");
    String usuarioDestino = body.get("usuarioDestino");
    String rutaArchivo = body.get("ruta"); 

    String base = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String pathOrigen = base + usuarioOrigen + ".json";
    String pathDestino = base + usuarioDestino + ".json";

    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonOrigen = mapper.readValue(Files.readString(Paths.get(pathOrigen)), Map.class);
        Map<String, Object> jsonDestino = mapper.readValue(Files.readString(Paths.get(pathDestino)), Map.class);

        Map<String, Object> estructuraOrigen = (Map<String, Object>) jsonOrigen.get("estructura");
        Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructuraOrigen, rutaArchivo);

        if (archivo == null) return ResponseEntity.badRequest().body("Archivo no encontrado o no es .txt");

        Map<String, Object> estructuraDestino = (Map<String, Object>) jsonDestino.get("estructura");
        Map<String, Object> dirCompartida = (Map<String, Object>) estructuraDestino.get("compartida");

        if (dirCompartida == null) {
            return ResponseEntity.badRequest().body("No se encontró la carpeta 'compartida' en el usuario destino.");
        }

        List<Map<String, Object>> contenidoCompartida = (List<Map<String, Object>>) dirCompartida.get("contenido");
        if (contenidoCompartida == null) {
            return ResponseEntity.status(500).body("La carpeta 'compartida' no tiene contenido.");
        }

        for (Map<String, Object> item : contenidoCompartida) {
            if (item.get("tipo").equals("archivo") &&
                item.get("nombre").equals(archivo.get("nombre")) &&
                item.get("extension").equals(archivo.get("extension"))) {
                return ResponseEntity.badRequest().body("Ya existe un archivo con ese nombre en la carpeta compartida.");
            }
        }

        // Copiar el archivo
        contenidoCompartida.add(new HashMap<>(archivo));

        // Guardar JSON destino
        mapper.writeValue(Paths.get(pathDestino).toFile(), jsonDestino);

        return ResponseEntity.ok("Archivo compartido con éxito.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al compartir archivo: " + e.getMessage());
    }
}


  @PostMapping("/delete")
  public ResponseEntity<String> eliminarArchivo(@RequestBody Map<String, String> payload) {
    String username = payload.get("username");
    String ruta = payload.get("ruta"); // ejemplo: /raiz/compartida/ejemplo.txt

    if (username == null || ruta == null) {
        return ResponseEntity.badRequest().body("Datos incompletos");
    }

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String pathJson = basePath + username + ".json";

    try {
        // Cargar JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> usuario = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
        Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

        // Dividir ruta en partes
        String[] partes = ruta.split("/");
        if (partes.length < 2) {
            return ResponseEntity.badRequest().body("Ruta inválida");
        }

        String archivoNombre = partes[partes.length - 1];
        String nombreSinExt = archivoNombre.contains(".") ? archivoNombre.substring(0, archivoNombre.lastIndexOf(".")) : archivoNombre;
        String extension = archivoNombre.contains(".") ? archivoNombre.substring(archivoNombre.lastIndexOf(".") + 1) : "";

        // Ruta del directorio que contiene el archivo
        String rutaPadre = ruta.substring(0, ruta.lastIndexOf("/"));

        Map<String, Object> dirPadre = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, rutaPadre);
        if (dirPadre == null) {
            return ResponseEntity.badRequest().body("No se encontró el directorio padre");
        }

        List<Map<String, Object>> contenido = (List<Map<String, Object>>) dirPadre.get("contenido");
        boolean eliminado = contenido.removeIf(item ->
            "archivo".equals(item.get("tipo")) &&
            nombreSinExt.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))
        );

        if (!eliminado) {
            return ResponseEntity.status(404).body("Archivo no encontrado");
        }

        // Guardar cambios
        mapper.writeValue(Paths.get(pathJson).toFile(), usuario);
        return ResponseEntity.ok("Archivo eliminado exitosamente.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al eliminar el archivo: " + e.getMessage());
    }
  }

    @PostMapping("/ver")
  public ResponseEntity<String> verArchivo(@RequestBody Map<String, String> body) {
  String username = body.get("username");
  String ruta = body.get("ruta");

  try {
    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> json = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
    Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

    Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructura, ruta);
    if (archivo == null) return ResponseEntity.status(404).body("Archivo no encontrado");

    return ResponseEntity.ok((String) archivo.get("contenido"));
  } catch (IOException e) {
    return ResponseEntity.status(500).body("Error: " + e.getMessage());
  }
}

@PostMapping("/editar")
public ResponseEntity<String> editarArchivo(@RequestBody Map<String, String> body) {
  String username = body.get("username");
  String ruta = body.get("ruta");
  String nuevoContenido = body.get("contenido");

  try {
    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> json = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
    Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

    Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructura, ruta);
    if (archivo == null) return ResponseEntity.status(404).body("Archivo no encontrado");

    archivo.put("contenido", nuevoContenido);
    archivo.put("tamano", nuevoContenido.length());
    archivo.put("fechaModificacion", LocalDateTime.now().toString());

    mapper.writeValue(Paths.get(pathJson).toFile(), json);

    return ResponseEntity.ok("Archivo actualizado con éxito");
  } catch (IOException e) {
    return ResponseEntity.status(500).body("Error: " + e.getMessage());
  }
}





}
