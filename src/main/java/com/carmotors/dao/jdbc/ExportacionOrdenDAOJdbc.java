package com.carmotors.dao.jdbc;


import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ExportacionOrdenDAO;
import com.carmotors.modelo.OrdenServicioExportada;
import com.carmotors.modelo.Cliente;
import com.carmotors.modelo.DetalleRepuesto;
import com.carmotors.modelo.EstadoReserva;
import com.carmotors.modelo.EstadoServicio;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.OrdenServicioBuilder;
import com.carmotors.modelo.TipoMantenimiento;
import com.carmotors.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExportacionOrdenDAOJdbc implements ExportacionOrdenDAO {

    private static final String SQL_ORDEN_COMPLETA = "SELECT s.id, s.vehiculo_id, s.mantenimiento_tipo_id, "
            + "s.fecha_recepcion, s.fecha_entrega, s.descripcion_problema, s.costo_mano_obra, s.estado_id, "
            + "v.cliente_id, v.placa, v.marca, v.modelo, "
            + "c.nombre, c.documento_identidad, c.telefono, c.email, "
            + "sr.repuesto_id, sr.cantidad_usada, sr.precio_unitario_venta, sr.estado AS estado_repuesto, "
            + "r.nombre AS nombre_repuesto "
            + "FROM servicios s "
            + "INNER JOIN vehiculos v ON v.id = s.vehiculo_id "
            + "INNER JOIN clientes c ON c.id = v.cliente_id "
            + "LEFT JOIN servicios_repuestos sr ON sr.servicio_id = s.id "
            + "LEFT JOIN repuestos r ON r.id = sr.repuesto_id "
            + "WHERE s.id = ? "
            + "ORDER BY r.nombre";

    private final ConexionFactory conexiones;

    public ExportacionOrdenDAOJdbc(ConexionFactory conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public OrdenServicioExportada buscarCompletaPorId(int servicioId) throws SQLException {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ORDEN_COMPLETA)) {
            sentencia.setInt(1, servicioId);
            try (ResultSet filas = sentencia.executeQuery()) {
                OrdenServicio orden = null;
                Vehiculo vehiculo = null;
                Cliente cliente = null;
                List<DetalleRepuesto> repuestos = new ArrayList<>();
                while (filas.next()) {
                    if (orden == null) {

                        orden = leerOrden(filas);
                        vehiculo = new Vehiculo(filas.getInt("vehiculo_id"), filas.getInt("cliente_id"),
                                filas.getString("placa"), filas.getString("marca"), filas.getString("modelo"));
                        cliente = new Cliente(filas.getInt("cliente_id"), filas.getString("nombre"),
                                filas.getString("documento_identidad"), filas.getString("telefono"),
                                filas.getString("email"));
                    }
                    int repuestoId = filas.getInt("repuesto_id");
                    if (!filas.wasNull()) {
                        repuestos.add(new DetalleRepuesto(servicioId, repuestoId, filas.getString("nombre_repuesto"),
                                filas.getInt("cantidad_usada"), filas.getInt("precio_unitario_venta"),
                                EstadoReserva.valueOf(filas.getString("estado_repuesto"))));
                    }
                }

                return orden == null ? null : new OrdenServicioExportada(orden, vehiculo, cliente, repuestos);
            }
        }
    }

    private OrdenServicio leerOrden(ResultSet filas) throws SQLException {
        return new OrdenServicioBuilder()
                .id(filas.getInt("id"))
                .vehiculoId(filas.getInt("vehiculo_id"))
                .tipoMantenimiento(TipoMantenimiento.desdeId(filas.getInt("mantenimiento_tipo_id")))
                .fechaRecepcion(filas.getString("fecha_recepcion"))
                .fechaEntrega(filas.getString("fecha_entrega"))
                .descripcionProblema(filas.getString("descripcion_problema"))
                .costoManoObra(filas.getInt("costo_mano_obra"))
                .estado(EstadoServicio.desdeId(filas.getInt("estado_id")))
                .build();
    }



}
