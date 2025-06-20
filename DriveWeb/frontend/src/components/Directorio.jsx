import { useState, useEffect } from "react";
import "./Directorio.css";

function Directorio({
  usuario,
  ruta: rutaInicial,
  contenido: contenidoInicial,
  volver,
}) {
  const [ruta, setRuta] = useState(rutaInicial || "/raiz");
  const [contenido, setContenido] = useState(contenidoInicial || []);
  const [mensaje, setMensaje] = useState("");
  const [mostrarModal, setMostrarModal] = useState(false);
  const [nombreNuevoDir, setNombreNuevoDir] = useState("");
  const [historial, setHistorial] = useState([]);
  const [espacio, setEspacio] = useState({ total: 0, usado: 0, disponible: 0 });


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
        setContenido([]);
      });
  }, [usuario, ruta]);

  useEffect(() => {
  fetch("/api/user/espacio", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: usuario }),
  })
    .then((res) => res.json())
    .then((data) => setEspacio(data))
    .catch((err) => {
      console.error(err);
      setEspacio({ total: 0, usado: 0, disponible: 0 });
    });
}, [usuario, ruta]);


  const crearDirectorio = () => {
    const yaExiste = contenido.some(
      (item) => item.tipo === "directorio" && item.nombre === nombreNuevoDir
    );
    if (
      yaExiste &&
      !window.confirm(
        "Ya existe un directorio con ese nombre. ¿Desea reemplazarlo?"
      )
    ) {
      return;
    }

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
        setContenido((prev) => {
          const filtrado = prev.filter(
            (item) =>
              !(item.tipo === "directorio" && item.nombre === nombreNuevoDir)
          );
          return [
            ...filtrado,
            { tipo: "directorio", nombre: nombreNuevoDir, contenido: [] },
          ];
        });
        setNombreNuevoDir("");
        setMostrarModal(false);
      })
      .catch((err) => {
        console.error(err);
        alert("Error al crear el directorio");
      });
  };

  const handleArchivo = async (e) => {
    const archivo = e.target.files[0];
    if (!archivo || !archivo.name.endsWith(".txt")) {
      alert("Solo se permiten archivos .txt");
      return;
    }

    const nombreArchivo = archivo.name.split(".")[0];

    const lector = new FileReader();

    lector.onload = async () => {
      const contenidoArchivo = lector.result ?? "";

      try {
        // Obtener SIEMPRE contenido actualizado
        const respuesta = await fetch("/api/user/ruta", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: usuario, ruta }),
        });

        if (!respuesta.ok) throw new Error("Error al obtener la ruta");

        const contenidoActualizado = await respuesta.json();

        const yaExiste = contenidoActualizado.some(
          (item) =>
            item.tipo === "archivo" &&
            item.nombre === nombreArchivo &&
            item.extension === "txt"
        );

        let continuar = true;
        if (yaExiste) {
          continuar = window.confirm(
            "Ya existe un archivo con ese nombre. ¿Desea reemplazarlo?"
          );
        }

        if (!continuar) return;

        // Subida
        const resUpload = await fetch("/api/user/upload", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: usuario,
            nombreArchivo,
            extension: "txt",
            contenido: contenidoArchivo,
            ruta,
          }),
        });

        const msg = await resUpload.text();
        setMensaje(msg);
        if (msg.includes("Espacio insuficiente")) {
          alert(msg);
          return;
        }

        // Actualizar contenido directamente
        const nuevaRespuesta = await fetch("/api/user/ruta", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: usuario, ruta }),
        });

        const nuevoContenido = await nuevaRespuesta.json();
        setContenido(nuevoContenido);
        actualizarEspacio();  
      } catch (error) {
        console.error("Error al subir el archivo:", error);
        alert("Ocurrió un error al subir el archivo.");
      }
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

  const actualizarEspacio = () => {
  fetch("/api/user/espacio", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: usuario }),
  })
    .then((res) => res.json())
    .then((data) => setEspacio(data))
    .catch((err) => {
      console.error("Error al actualizar espacio:", err);
    });
};


  const renderContenido = (contenido) => {
    return (Array.isArray(contenido) ? contenido : []).map((item, i) =>
      item.tipo === "archivo" ? (
        <li key={i}>
          📄 {item.nombre}.{item.extension}
        </li>
      ) : (
        <li
          key={i}
          className="carpeta-clic"
          onClick={() => entrarADirectorio(item.nombre)}
        >
          📁 {item.nombre}
        </li>
      )
    );
  };

  return (
    <div className="directorio-container">
      <h2>
        {usuario} - {ruta}
      </h2>
      <p>
      Espacio total: {espacio.total} bytes | Usado: {espacio.usado} bytes | Disponible: {espacio.disponible} bytes
    </p>

      <div className="botones">
        <button onClick={volverAtras}>🔙 Volver</button>
        <button onClick={() => setMostrarModal(true)}>
          📁 Crear directorio
        </button>
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
