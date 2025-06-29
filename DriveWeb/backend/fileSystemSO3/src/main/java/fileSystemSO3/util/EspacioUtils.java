package fileSystemSO3.util;

import java.util.*;

public class EspacioUtils {

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerDirectorioDesdeRuta(Map<String, Object> estructura, String ruta) {
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
      if (!encontrado)
        return null;
    }
    return actual;
  }

  @SuppressWarnings("unchecked")
  public static int calcularEspacio(Map<String, Object> estructura) {
    int total = 0;
    for (Object value : estructura.values()) {
      if (value instanceof Map) {
        total += recorrerYSumar((Map<String, Object>) value);
      }
    }
    return total;
  }

  @SuppressWarnings("unchecked")
  private static int recorrerYSumar(Map<String, Object> directorio) {
    int suma = 0;
    List<Map<String, Object>> contenido = (List<Map<String, Object>>) directorio.get("contenido");
    for (Map<String, Object> item : contenido) {
      if ("archivo".equals(item.get("tipo"))) {
        suma += (int) item.getOrDefault("tamano", 0);
      } else if ("directorio".equals(item.get("tipo"))) {
        suma += recorrerYSumar(item);
      }
    }
    return suma;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerArchivoDesdeRuta(Map<String, Object> estructura, String ruta) {
    if (estructura == null || ruta == null || ruta.isEmpty()) return null;

    // Quitar slash inicial y dividir
    String[] partes = ruta.replaceFirst("^/", "").split("/");

    if (partes.length < 2) return null; // Debe tener al menos carpeta y archivo

    // Obtener carpeta principal (ej: "raiz" o "compartida")
    String carpetaPrincipal = partes[0];
    Map<String, Object> directorio = (Map<String, Object>) estructura.get(carpetaPrincipal);
    if (directorio == null) return null;

    for (int i = 1; i < partes.length - 1; i++) {
        String subdir = partes[i];
        List<Map<String, Object>> contenido = (List<Map<String, Object>>) directorio.get("contenido");
        if (contenido == null) return null;

        boolean encontrado = false;
        for (Map<String, Object> item : contenido) {
            if ("directorio".equals(item.get("tipo")) && subdir.equals(item.get("nombre"))) {
                directorio = item;
                encontrado = true;
                break;
            }
        }
        if (!encontrado) return null;
    }

    // Buscar el archivo
    String archivoNombre = partes[partes.length - 1];
    String nombreSinExt = archivoNombre.contains(".") ? archivoNombre.substring(0, archivoNombre.lastIndexOf(".")) : archivoNombre;
    String extension = archivoNombre.contains(".") ? archivoNombre.substring(archivoNombre.lastIndexOf(".") + 1) : "";

    List<Map<String, Object>> contenidoFinal = (List<Map<String, Object>>) directorio.get("contenido");
    if (contenidoFinal == null) return null;

    for (Map<String, Object> item : contenidoFinal) {
        if ("archivo".equals(item.get("tipo")) &&
            nombreSinExt.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))) {
            return item;
        }
    }

    return null;
}



}
