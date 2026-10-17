const contenedor = document.querySelector("#productos");
const estado = document.querySelector("#estado");
const plantilla = document.querySelector("#plantilla-producto");
const panelFormulario = document.querySelector("#panel-formulario");
const formulario = document.querySelector("#formulario-producto");
const mensajeFormulario = document.querySelector("#mensaje-formulario");
const botonMostrarFormulario = document.querySelector("#mostrar-formulario");
const botonCerrarFormulario = document.querySelector("#cerrar-formulario");
const panelTecnico = document.querySelector("#panel-tecnico");
const versionVisible = document.querySelector("#version-visible");

const configuracion = window.APP_CONFIG ?? {};
const modo = configuracion.modo === "estatico" ? "estatico" : "api";
const apiBase = normalizarBase(configuracion.apiBase ?? "./api");
const datosUrl = configuracion.datos ?? "./data/productos.json";
const esModoApi = modo === "api";

configurarInterfaz();
cargarProductos();

if (esModoApi) {
    botonMostrarFormulario.addEventListener("click", () => {
        panelFormulario.classList.remove("oculto");
        formulario.elements.nombre.focus();
    });

    botonCerrarFormulario.addEventListener("click", () => {
        panelFormulario.classList.add("oculto");
        mensajeFormulario.textContent = "";
    });

    formulario.addEventListener("submit", crearProducto);
    cargarInformacionInstancia();
}

function configurarInterfaz() {
    document.documentElement.dataset.modo = modo;

    if (!esModoApi) {
        aplicarVersion(configuracion.version);
        botonMostrarFormulario.hidden = true;
        panelFormulario.hidden = true;
        panelTecnico.hidden = true;
    }
}

async function cargarProductos() {
    try {
        const url = esModoApi ? `${apiBase}/productos` : datosUrl;
        const respuesta = await fetch(url);

        if (!respuesta.ok) {
            throw new Error(`HTTP ${respuesta.status}`);
        }

        const productos = await respuesta.json();
        pintarProductos(productos);
        estado.textContent = esModoApi
            ? `${productos.length} productos`
            : `${productos.length} productos · catálogo estático`;
    } catch (error) {
        console.error("No se pudo cargar el catálogo", error);
        estado.textContent = "No disponible";
        contenedor.innerHTML = `
            <p class="error">
                No se ha podido cargar el catálogo. Comprueba que los datos estén disponibles.
            </p>`;
    }
}

function pintarProductos(productos) {
    contenedor.replaceChildren();

    for (const producto of productos) {
        const fragmento = plantilla.content.cloneNode(true);
        const tarjeta = fragmento.querySelector(".tarjeta");
        const imagen = fragmento.querySelector(".imagen-producto");
        const placeholder = fragmento.querySelector(".imagen-placeholder");
        const botonEliminar = fragmento.querySelector(".boton-eliminar");

        fragmento.querySelector(".nombre").textContent = producto.nombre;
        fragmento.querySelector(".descripcion").textContent = producto.descripcion ?? "Sin descripción";
        fragmento.querySelector(".precio").textContent = formatearPrecio(producto.precio);

        if (producto.imagenUrl) {
            imagen.src = construirUrlImagen(producto.imagenUrl);
            imagen.alt = `Imagen de ${producto.nombre}`;
            imagen.hidden = false;
            placeholder.hidden = true;
            imagen.addEventListener("error", () => {
                imagen.hidden = true;
                placeholder.hidden = false;
                placeholder.textContent = esModoApi
                    ? "Imagen no disponible en esta instancia"
                    : "Imagen no disponible";
            });
        }

        if (esModoApi) {
            botonEliminar.addEventListener("click", () => eliminarProducto(producto));
        } else {
            botonEliminar.hidden = true;
        }

        tarjeta.dataset.productoId = producto.id;
        contenedor.append(fragmento);
    }
}

async function crearProducto(evento) {
    evento.preventDefault();
    mensajeFormulario.textContent = "Guardando…";
    mensajeFormulario.className = "mensaje-formulario";

    const datos = new FormData(formulario);

    try {
        const respuesta = await fetch(`${apiBase}/productos`, {
            method: "POST",
            body: datos
        });

        if (!respuesta.ok) {
            throw new Error(await obtenerMensajeError(respuesta));
        }

        formulario.reset();
        formulario.elements.precio.value = "0";
        mensajeFormulario.textContent = "Producto añadido correctamente.";
        mensajeFormulario.classList.add("exito");
        await cargarProductos();
    } catch (error) {
        console.error("No se pudo crear el producto", error);
        mensajeFormulario.textContent = error.message || "No se ha podido guardar el producto.";
        mensajeFormulario.classList.add("fallo");
    }
}

async function eliminarProducto(producto) {
    const confirmado = window.confirm(`¿Eliminar “${producto.nombre}”?`);
    if (!confirmado) {
        return;
    }

    try {
        const respuesta = await fetch(`${apiBase}/productos/${producto.id}`, {
            method: "DELETE"
        });

        if (!respuesta.ok && respuesta.status !== 404) {
            throw new Error(await obtenerMensajeError(respuesta));
        }

        await cargarProductos();
    } catch (error) {
        console.error("No se pudo eliminar el producto", error);
        window.alert(error.message || "No se ha podido eliminar el producto.");
    }
}

async function obtenerMensajeError(respuesta) {
    try {
        const cuerpo = await respuesta.json();
        return cuerpo.detail ?? cuerpo.title ?? `HTTP ${respuesta.status}`;
    } catch {
        return `HTTP ${respuesta.status}`;
    }
}

function construirUrlImagen(ruta) {
    if (/^https?:\/\//i.test(ruta)) {
        return ruta;
    }

    if (!esModoApi) {
        return ruta;
    }

    return `${apiBase}${ruta.startsWith("/") ? "" : "/"}${ruta}`;
}

function normalizarBase(base) {
    return base.endsWith("/") ? base.slice(0, -1) : base;
}

function formatearPrecio(precio) {
    return new Intl.NumberFormat("es-ES", {
        style: "currency",
        currency: "EUR"
    }).format(precio);
}

async function cargarInformacionInstancia() {
    const datoInstancia = document.querySelector("#dato-instancia");
    const datoVersion = document.querySelector("#dato-version");
    const datoAlmacenamiento = document.querySelector("#dato-almacenamiento");

    try {
        const respuesta = await fetch(`${apiBase}/instancia`);
        if (!respuesta.ok) {
            throw new Error(`HTTP ${respuesta.status}`);
        }

        const informacion = await respuesta.json();
        datoInstancia.textContent = informacion.host;
        datoVersion.textContent = informacion.version;
        datoAlmacenamiento.textContent = informacion.almacenamiento;
        aplicarVersion(informacion.version);
    } catch (error) {
        console.error("No se pudo cargar la información de la instancia", error);
        datoInstancia.textContent = "No disponible";
        datoVersion.textContent = "No disponible";
        datoAlmacenamiento.textContent = "No disponible";
    }
}


function aplicarVersion(version) {
    if (!version) {
        return;
    }

    versionVisible.textContent = `v${version}`;
    versionVisible.hidden = false;
}
