
package com.carmotors.modelo;

import java.util.List;

public class OrdenServicioExportada {
    private final OrdenServicio orden;              // fecha, descripción, mano de obra
    private final Vehiculo vehiculo;                // placa, marca, modelo
    private final Cliente cliente;                  // nombre, documento
    private final List<DetalleRepuesto> repuestos;  // cantidad y precio unitario de venta

    public OrdenServicioExportada(OrdenServicio orden, Vehiculo vehiculo, Cliente cliente,
                                  List<DetalleRepuesto> repuestos) {
        this.orden = orden;
        this.vehiculo = vehiculo;
        this.cliente = cliente;
        this.repuestos = List.copyOf(repuestos);  // copia inmutable
    }

    /** Suma de los subtotales (cantidad x precio unitario) de todos los repuestos. */
    public int totalRepuestos() {
        return repuestos.stream()
                .mapToInt(DetalleRepuesto::subtotal)
                .sum();
    }

    /** Repuestos + mano de obra. */
    public int totalAPagar() {
        return totalRepuestos() + orden.getCostoManoObra();
    }

    public OrdenServicio getOrden() {
        return orden;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<DetalleRepuesto> getRepuestos() {
        return repuestos;
    }
}
