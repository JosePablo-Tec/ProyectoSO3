import { useState, useEffect } from "react";
import "./Directorio.css";

function Directorio({ usuario, ruta: rutaInicial, contenido: contenidoInicial, volver }) {
  const [ruta, setRuta] = useState(rutaInicial || "/raiz");
  const [contenido, setContenido] = useState(contenidoInicial || []);
  const [mensaje, setMensaje] = useState("");
  const [mostrarModal, setMostrarModal] = useState(false);
  const [nombreNuevoDir, setNombreNuevoDir] = useState("");
  const [historial, setHistorial] = useState([]);

  useEffect(() => {
    fetch("/api/user/ruta", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: usuario, ruta }),
    })
      .then((res) => res.json())
      .then((data) => setContenido(data))
      .catch((err) => {
        console.error(err);
        setMensaje("Error al cargar el contenido del directorio.");
      });
  }, [ruta]);

  const crearDirectorio = () => {
    if (!nombreNuevoDir.trim()) return;

    fetch("/api/user/mkdir", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: usuario,
        nombreDirectorio: nombreNuevoDir,
        ruta,
      }),
    })
      .then((res) => res.text())
      .then((msg) => {
        setMensaje(msg);
        setContenido((prev) => [
          ...prev,
          { tipo: "directorio", nombre: nombreNuevoDir, contenido: [] },
        ]);
        setNombreNuevoDir("");
        setMostrarModal(false);
      })
      .catch((err) => {
        console.error(err);
        alert("Error al crear el directorio");
      });
  };

  const handleArchivo = (e) => {
    const archivo = e.target.files[0];
    if (!archivo || !archivo.name.endsWith(".txt")) {
      alert("Solo se permiten archivos .txt");
      return;
    }

    const lector = new FileReader();
    lector.onload = () => {
      const contenidoArchivo = lector.result;

      fetch("/api/user/upload", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: usuario,
          nombreArchivo: archivo.name.split(".")[0],
          extension: "txt",
          contenido: contenidoArchivo,
          ruta,
        }),
      })
        .then((res) => res.text())
        .then((msg) => {
          setMensaje(msg);
          const nuevoArchivo = {
            tipo: "archivo",
            nombre: archivo.name.split(".")[0],
            extension: "txt",
          };
          setContenido((prev) => [...prev, nuevoArchivo]);
        })
        .catch((err) => {
          console.error(err);
          alert("Error al subir el archivo");
        });
    };
    lector.readAsText(archivo);
  };

  const entrarADirectorio = (nombre) => {
    const nuevaRuta = ruta + "/" + nombre;
    setHistorial((prev) => [...prev, ruta]);
    setRuta(nuevaRuta);
  };

  const volverAtras = () => {
    if (historial.length === 0) {
      volver();
    } else {
      const nuevaRuta = historial[historial.length - 1];
      setHistorial((prev) => prev.slice(0, -1));
      setRuta(nuevaRuta);
    }
  };

  const renderContenido = (contenido) => {
    return (Array.isArray(contenido) ? contenido : []).map((item, i) =>
      item.tipo === "archivo" ? (
        <li key={i}>📄 {item.nombre}.{item.extension}</li>
      ) : (
        <li key={i} className="carpeta-clic" onClick={() => entrarADirectorio(item.nombre)}>
          📁 {item.nombre}
        </li>
      )
    );
  };

  return (
    <div className="directorio-container">
      <h2>{usuario} - {ruta}</h2>
      <div className="botones">
        <button onClick={volverAtras}>🔙 Volver</button>
        <button onClick={() => setMostrarModal(true)}>📁 Crear directorio</button>
        <label className="subir-archivo-btn">
          📤 Subir archivo
          <input type="file" accept=".txt" onChange={handleArchivo} hidden />
        </label>
      </div>

      <ul className="contenido-lista">{renderContenido(contenido)}</ul>
      {mensaje && <p className="mensaje">{mensaje}</p>}

      {mostrarModal && (
        <div className="modal">
          <div className="modal-contenido">
            <h3>Nuevo directorio</h3>
            <input
              type="text"
              placeholder="Nombre del directorio"
              value={nombreNuevoDir}
              onChange={(e) => setNombreNuevoDir(e.target.value)}
            />
            <div className="modal-botones">
              <button onClick={crearDirectorio}>Crear</button>
              <button onClick={() => setMostrarModal(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Directorio;
