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
}
