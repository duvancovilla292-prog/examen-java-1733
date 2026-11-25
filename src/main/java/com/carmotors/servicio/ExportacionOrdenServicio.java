package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ExportacionOrdenDAO;
import com.carmotors.dao.jdbc.ExportacionOrdenDAOJdbc;
import com.carmotors.modelo.OrdenServicioExportada;
import com.carmotors.excepcion.RecursoNoEncontradoException;
import com.carmotors.modelo.Cliente;
import com.carmotors.modelo.DetalleRepuesto;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.Vehiculo;
import com.carmotors.vista.Formato;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ExportacionOrdenServicio {
    private static final String DOBLE = "===============================================================";
    private static final String SIMPLE = "------------------------------------------------------------------";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportacionOrdenDAO dao;

    public ExportacionOrdenServicio() {
        this.dao = new ExportacionOrdenDAOJdbc(ConexionFactory.getInstancia());
    }


    public Path exportarOrden(int servicioId) throws SQLException, IOException {
        OrdenServicioExportada datos = dao.buscarCompletaPorId(servicioId);
        if (datos == null) {
            throw new RecursoNoEncontradoException("No existe ningún servicio registrado con el ID " + servicioId
                    + ". Verifique el número e intente de nuevo.");
        }
        String contenido = construirReporte(datos);
        // Ruta relativa = raíz del proyecto (directorio desde donde se ejecuta la aplicación).
        Path archivo = Path.of("orden_servicio_" + servicioId + ".txt");
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write(contenido);  // CREATE + TRUNCATE por defecto: si ya existía se reemplaza
        }
        return archivo;
    }

    /** Arma el texto completo del reporte, sección por sección. */
    public String construirReporte(OrdenServicioExportada datos) {
        OrdenServicio orden = datos.getOrden();
        Cliente cliente = datos.getCliente();
        Vehiculo vehiculo = datos.getVehiculo();
        StringBuilder texto = new StringBuilder();

        // Encabezado
        linea(texto, DOBLE);
        linea(texto, "                ORDEN DE SERVICIO: Nro. " + orden.getId());
        linea(texto, DOBLE);
        linea(texto, "Fecha de Emisión: " + LocalDateTime.now().format(FORMATO_FECHA));
        linea(texto, "Fecha del Servicio: " + orden.getFechaRecepcion());
        linea(texto, "Taller: CarMotors");
        linea(texto, "");

        // Cliente y vehículo
        linea(texto, "-- DATOS DEL CLIENTE --");
        linea(texto, etiqueta("Nombre:") + cliente.getNombre());
        linea(texto, etiqueta("Documento:") + cliente.getDocumento());
        linea(texto, "");
        linea(texto, "-- DATOS DEL VEHÍCULO --");
        linea(texto, etiqueta("Placa:") + vehiculo.getPlaca());
        linea(texto, etiqueta("Marca:") + valor(vehiculo.getMarca()));
        linea(texto, etiqueta("Modelo:") + valor(vehiculo.getModelo()));
        linea(texto, "");

        // Detalle de la reparación
        titulo(texto, "DETALLE DE LA REPARACIÓN");
        linea(texto, "Descripción del Problema:");
        linea(texto, valor(orden.getDescripcionProblema()));
        linea(texto, "");

        // Tabla de repuestos
        titulo(texto, "REPUESTOS UTILIZADOS");
        linea(texto, "| Cant. | Descripción del Repuesto     | P. Unit.  | Subtotal   |");
        linea(texto, SIMPLE);
        if (datos.getRepuestos().isEmpty()) {
            linea(texto, "| (No se utilizaron repuestos en este servicio)");
        }
        for (DetalleRepuesto repuesto : datos.getRepuestos()) {
            linea(texto, String.format("| %-5d | %-29s| %-9s | %-10s |",
                    repuesto.getCantidad(),
                    recortar(repuesto.getNombreRepuesto(), 29),
                    pesos(repuesto.getPrecioUnitario()),
                    pesos(repuesto.subtotal())));
        }
        linea(texto, SIMPLE);
        linea(texto, "");

        // Resumen de costos
        titulo(texto, "RESUMEN DE COSTOS");
        linea(texto, String.format("%-25s%s", "Subtotal Repuestos:", pesos(datos.totalRepuestos())));
        linea(texto, String.format("%-25s%s", "Mano de Obra:", pesos(orden.getCostoManoObra())));
        linea(texto, SIMPLE);
        linea(texto, String.format("%-25s%s", "TOTAL A PAGAR:", pesos(datos.totalAPagar())));
        linea(texto, DOBLE);
        return texto.toString();
    }

    // ------------------------------------------------------------ Apoyo de formato

    private void linea(StringBuilder texto, String contenido) {
        texto.append(contenido).append(System.lineSeparator());
    }

    // Título de sección entre dos líneas de guiones.
    private void titulo(StringBuilder texto, String nombre) {
        linea(texto, SIMPLE);
        linea(texto, "                 " + nombre);
        linea(texto, SIMPLE);
    }

    // Etiqueta alineada a 22 caracteres: "Nombre:               ".
    private String etiqueta(String nombre) {
        return String.format("%-22s", nombre);
    }

    // Reutiliza Formato.pesos ("$80.000") y agrega el espacio del formato pedido ("$ 80.000").
    private String pesos(int valor) {
        return Formato.pesos(valor).replace("$", "$ ");
    }

    private String recortar(String texto, int ancho) {
        String valor = valor(texto);
        return valor.length() > ancho ? valor.substring(0, ancho) : valor;
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

}

