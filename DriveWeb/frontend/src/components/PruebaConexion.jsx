// src/components/PruebaConexion.jsx
import { useEffect, useState } from "react";

function PruebaConexion() {
  const [mensaje, setMensaje] = useState("");

  useEffect(() => {
    fetch("/api/test")
      .then(res => res.text())
      .then(data => setMensaje(data))
      .catch(err => setMensaje("Error de conexión"));
  }, []);

  return <h3>{mensaje}</h3>;
}

export default PruebaConexion;
