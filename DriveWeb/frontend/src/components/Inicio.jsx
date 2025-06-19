import { useState } from "react";

function Inicio({ onLogin }) {
  const [nombre, setNombre] = useState("");
  const [mensaje, setMensaje] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    fetch("/api/user/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: nombre }),
    })
      .then((res) => res.text())
      .then((data) => {
        setMensaje(data);
        onLogin(nombre); // Para ir a la página principal
      })
      .catch((err) => {
        setMensaje("Error al crear el usuario");
        console.error(err);
      });
  };

  return (
    <div>
      <h2>Bienvenido al Drive Web</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Ingrese su nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          required
        />
        <button type="submit">Crear Drive</button>
      </form>
      <p>{mensaje}</p>
    </div>
  );
}

export default Inicio;
