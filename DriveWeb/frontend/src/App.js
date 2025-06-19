import { useState } from "react";
import Inicio from "./components/Inicio";

function App() {
  const [usuario, setUsuario] = useState(null);

  return (
    <div>
      {usuario ? (
        <h2>Bienvenido, {usuario}</h2>
      ) : (
        <Inicio onLogin={setUsuario} />
      )}
    </div>
  );
}

export default App;
