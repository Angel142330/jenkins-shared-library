package com.jenkins.mysql

import java.text.SimpleDateFormat

/**
 * Clase para insertar métricas en MySQL
 */
class MetricsInserter {
    
    private MySQLConnection db
    private def script // Referencia al script de Jenkins para logs
    
    MetricsInserter(def script, Map dbConfig = [:]) {
        this.script = script
        this.db = new MySQLConnection(dbConfig)
    }
    
    /**
     * Insertar ejecución de pipeline
     */
    def insertPipelineExecution(Map data) {
        script.echo "📊 Insertando ejecución de pipeline en BBDD..."
        
        def query = """
            INSERT INTO pipeline_executions 
            (commit_hash, build_number, id_proyecto, aplicativo, rama, resultado, timestamp, entorno, duracion_segundos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        
        def timestamp = formatTimestamp(data.timestamp ?: new Date())
        
        def params = [
            data.commit ?: 'N/A',
            data.buildNumber ?: '0',
            data.idProyecto ?: 'unknown',
            data.aplicativo ?: 'unknown',
            data.rama ?: 'unknown',
            data.resultado ?: 'SUCCESS',
            timestamp,
            data.entorno ?: 'DESARROLLO',
            data.duracion ?: 0
        ]
        
        try {
            db.executeUpdate(query, params)
            def executionId = db.getLastInsertId()
            script.echo "✅ Execution ID: ${executionId}"
            return executionId
        } catch (Exception e) {
            script.echo "❌ Error insertando pipeline execution: ${e.message}"
            throw e
        }
    }
    
    /**
     * Insertar métricas de Kiuwan
     */
    def insertKiuwanMetrics(def executionId, Map data) {
        script.echo "📈 Insertando métricas Kiuwan..."
        
        def query = """
            INSERT INTO kiuwan_metrics 
            (execution_id, commit_hash, id_proyecto, aplicativo, rama, score, estado, umbral, 
             excepcionado, excepcion, descripcion_excepcion, timestamp, entorno)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        
        def timestamp = formatTimestamp(data.timestamp ?: new Date())
        
        def params = [
            executionId,
            data.commit ?: 'N/A',
            data.idProyecto ?: 'unknown',
            data.aplicativo ?: 'unknown',
            data.rama ?: 'unknown',
            data.score ?: 0,
            data.estado ?: 'OK',
            data.umbral ?: 70,
            data.excepcionado ? 1 : 0,
            data.excepcion ?: null,
            data.descripcionExcepcion ?: null,
            timestamp,
            data.entorno ?: 'DESARROLLO'
        ]
        
        try {
            db.executeUpdate(query, params)
            script.echo "✅ Métricas Kiuwan insertadas"
            return true
        } catch (Exception e) {
            script.echo "❌ Error insertando métricas Kiuwan: ${e.message}"
            return false
        }
    }
    
    /**
     * Insertar métricas de pruebas unitarias
     */
    def insertUnitariasMetrics(def executionId, Map data) {
        script.echo "📈 Insertando métricas unitarias..."
        
        def query = """
            INSERT INTO unitarias_metrics 
            (execution_id, commit_hash, id_proyecto, aplicativo, rama, total_tests, passed_tests, 
             failed_tests, skipped_tests, cobertura_porcentaje, umbral, resultado, excepcion, 
             descripcion_excepcion, timestamp, entorno)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        
        def timestamp = formatTimestamp(data.timestamp ?: new Date())
        
        def params = [
            executionId,
            data.commit ?: 'N/A',
            data.idProyecto ?: 'unknown',
            data.aplicativo ?: 'unknown',
            data.rama ?: 'unknown',
            data.total ?: 0,
            data.passed ?: 0,
            data.failed ?: 0,
            data.skipped ?: 0,
            data.cobertura ?: 0.0,
            data.umbral ?: 80,
            data.resultado ?: 'SUCCESS',
            data.excepcion ?: null,
            data.descripcionExcepcion ?: null,
            timestamp,
            data.entorno ?: 'DESARROLLO'
        ]
        
        try {
            db.executeUpdate(query, params)
            script.echo "✅ Métricas unitarias insertadas"
            return true
        } catch (Exception e) {
            script.echo "❌ Error insertando métricas unitarias: ${e.message}"
            return false
        }
    }
    
    /**
     * Insertar métricas de regresión
     */
    def insertRegresionMetrics(def executionId, Map data) {
        script.echo "📈 Insertando métricas regresión (suite: ${data.suite})..."
        
        def query = """
            INSERT INTO regresion_metrics 
            (execution_id, commit_hash, id_proyecto, aplicativo, rama, suite_name, total_tests, 
             passed_tests, failed_tests, umbral, resultado, excepcion, descripcion_excepcion, 
             timestamp, entorno)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        
        def timestamp = formatTimestamp(data.timestamp ?: new Date())
        
        def params = [
            executionId,
            data.commit ?: 'N/A',
            data.idProyecto ?: 'unknown',
            data.aplicativo ?: 'unknown',
            data.rama ?: 'unknown',
            data.suite ?: 'unknown',
            data.total ?: 0,
            data.passed ?: 0,
            data.failed ?: 0,
            data.umbral ?: 100,
            data.resultado ?: 'SUCCESS',
            data.excepcion ?: null,
            data.descripcionExcepcion ?: null,
            timestamp,
            data.entorno ?: 'DESARROLLO'
        ]
        
        try {
            db.executeUpdate(query, params)
            script.echo "✅ Métricas regresión insertadas"
            return true
        } catch (Exception e) {
            script.echo "❌ Error insertando métricas regresión: ${e.message}"
            return false
        }
    }
    
    /**
     * Formatear timestamp para MySQL
     */
    private String formatTimestamp(Date date) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(date)
    }
    
    /**
     * Cerrar conexión
     */
    void close() {
        db.close()
    }
}

