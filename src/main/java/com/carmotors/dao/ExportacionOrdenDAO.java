package com.carmotors.dao;
import com.carmotors.modelo.OrdenServicioExportada;
import java.sql.SQLException;

public interface ExportacionOrdenDAO {

    OrdenServicioExportada buscarCompletaPorId(int servicioId) throws SQLException;
}
