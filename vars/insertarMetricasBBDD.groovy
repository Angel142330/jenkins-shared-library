/**
 * Función global para insertar métricas REALES en MySQL
 * Puede ser llamada desde cualquier validador
 */

import com.jenkins.mysql.MySQLConnection
import com.jenkins.mysql.MetricsInserter

def call(Map config = [:]) {
    def tipoValidacion = config.tipo ?: 'UNITARIA'
    def resultado = config.resultado ?: [:]
    def branch = config.branch ?: env.BRANCH_NAME ?: 'unknown'
    def usarBBDDReal = config.usarBBDDReal != false // Activar por defecto
    
    echo "💾 Insertando métricas en BBDD MySQL..."
    
    // Preparar datos completos
    def commit = env.GIT_COMMIT ?: env.GIT_REVISION ?: 'N/A'
    def buildNumber = env.BUILD_NUMBER ?: '0'
    def idProyecto = env.JOB_NAME ?: 'proyecto-demo'
    def aplicativo = idProyecto.tokenize('/')[0]
    def resultadoTest = resultado.success ? 'SUCCESS' : 'FAILURE'
    def timestamp = new Date()
    def umbral = config.umbral ?: 80
    def excepcion = resultado.success ? null : (resultado.excepcion ?: 'Test fallido')
    def descripcionExcepcion = resultado.success ? null : (resultado.descripcionExcepcion ?: obtenerDescripcionError(resultado))
    def entorno = obtenerEntorno(branch)
    
    // Intentar inserción REAL en MySQL
    if (usarBBDDReal) {
        try {
            def inserter = new MetricsInserter(this, [
                host: 'localhost',
                port: '3306',
                database: 'jenkins_metrics',
                user: 'jenkins_user',
                password: 'Jenkins@2025!'
            ])
            
            // 1. Insertar ejecución principal
            def executionId = inserter.insertPipelineExecution([
                commit: commit,
                buildNumber: buildNumber,
                idProyecto: idProyecto,
                aplicativo: aplicativo,
                rama: branch,
                resultado: resultadoTest,
                timestamp: timestamp,
                entorno: entorno,
                duracion: 0
            ])
            
            // 2. Insertar métricas específicas según tipo
            switch(tipoValidacion) {
                case 'KIUWAN':
                    inserter.insertKiuwanMetrics(executionId, [
                        commit: commit,
                        idProyecto: idProyecto,
                        aplicativo: aplicativo,
                        rama: branch,
                        score: resultado.score ?: 0,
                        estado: resultado.estado ?: 'OK',
                        umbral: 70,
                        excepcionado: resultado.excepcionado ?: false,
                        excepcion: excepcion,
                        descripcionExcepcion: descripcionExcepcion,
                        timestamp: timestamp,
                        entorno: entorno
                    ])
                    break
                    
                case 'UNITARIA':
                    inserter.insertUnitariasMetrics(executionId, [
                        commit: commit,
                        idProyecto: idProyecto,
                        aplicativo: aplicativo,
                        rama: branch,
                        total: resultado.total ?: 0,
                        passed: resultado.passed ?: 0,
                        failed: resultado.failed ?: 0,
                        skipped: 0,
                        cobertura: 0.0,
                        umbral: umbral,
                        resultado: resultadoTest,
                        excepcion: excepcion,
                        descripcionExcepcion: descripcionExcepcion,
                        timestamp: timestamp,
                        entorno: entorno
                    ])
                    break
                    
                case 'REGRESION':
                    inserter.insertRegresionMetrics(executionId, [
                        commit: commit,
                        idProyecto: idProyecto,
                        aplicativo: aplicativo,
                        rama: branch,
                        suite: resultado.suite ?: 'unknown',
                        total: resultado.total ?: 0,
                        passed: resultado.passed ?: 0,
                        failed: resultado.failed ?: 0,
                        umbral: 100,
                        resultado: resultadoTest,
                        excepcion: excepcion,
                        descripcionExcepcion: descripcionExcepcion,
                        timestamp: timestamp,
                        entorno: entorno
                    ])
                    break
            }
            
            // Cerrar conexión
            inserter.close()
            
            echo """
    ╔═══════════════════════════════════════════════════════════════╗
    ║           ✅ INSERCIÓN REAL EN MySQL - ${tipoValidacion.padRight(26)}║
    ╚═══════════════════════════════════════════════════════════════╝
    
    📊 Datos Insertados:
       • Execution ID:       ${executionId}
       • Commit:             ${commit.take(12)}
       • Build:              #${buildNumber}
       • Proyecto:           ${idProyecto}
       • Rama:               ${branch}
       • Resultado:          ${resultadoTest}
       • Entorno:            ${entorno}
       • Timestamp:          ${timestamp}
    
    ✅ Datos guardados en MySQL: jenkins_metrics.${tipoValidacion.toLowerCase()}_metrics
            """
            
            return [
                success: true,
                executionId: executionId,
                timestamp: timestamp,
                metodo: 'MySQL'
            ]
            
        } catch (Exception e) {
            echo "⚠️  Error conectando a MySQL: ${e.message}"
            echo "   Continuando sin inserción en BBDD..."
            
            // Si falla, solo mostrar lo que se hubiera insertado
            mostrarDatosSimulados(tipoValidacion, commit, buildNumber, idProyecto, aplicativo, branch, resultadoTest, timestamp, entorno, resultado, umbral, excepcion, descripcionExcepcion)
            
            return [
                success: false,
                error: e.message,
                metodo: 'Simulado'
            ]
        }
    } else {
        // Modo simulación (sin inserción real)
        mostrarDatosSimulados(tipoValidacion, commit, buildNumber, idProyecto, aplicativo, branch, resultadoTest, timestamp, entorno, resultado, umbral, excepcion, descripcionExcepcion)
        
        return [
            success: true,
            metodo: 'Simulado'
        ]
    }
}

def mostrarDatosSimulados(tipoValidacion, commit, buildNumber, idProyecto, aplicativo, branch, resultadoTest, timestamp, entorno, resultado, umbral, excepcion, descripcionExcepcion) {
    echo """
    ╔═══════════════════════════════════════════════════════════════╗
    ║           DATOS QUE SE INSERTARÍAN - ${tipoValidacion.padRight(22)}║
    ╚═══════════════════════════════════════════════════════════════╝
    
    📊 Datos Generales:
       • Commit:             ${commit.take(12)}
       • Build Number:       #${buildNumber}
       • ID Proyecto:        ${idProyecto}
       • Aplicativo:         ${aplicativo}
       • Rama:               ${branch}
       • Resultado:          ${resultadoTest}
       • Timestamp:          ${timestamp}
       • Entorno:            ${entorno}
    
    📈 Métricas ${tipoValidacion}:
${formatearMetricas(resultado, tipoValidacion)}
    
    ⚠️  Excepciones:
       • Excepción:          ${excepcion ?: 'N/A'}
       • Descripción:        ${descripcionExcepcion ?: 'N/A'}
    """
}

def formatearMetricas(Map resultado, String tipo) {
    def output = ""
    switch(tipo) {
        case 'KIUWAN':
            output += "       • Score: ${resultado.score ?: 0}\n"
            output += "       • Estado: ${resultado.estado ?: 'OK'}\n"
            break
        case 'UNITARIA':
            output += "       • Total: ${resultado.total ?: 0}\n"
            output += "       • Passed: ${resultado.passed ?: 0}\n"
            output += "       • Failed: ${resultado.failed ?: 0}\n"
            break
        case 'REGRESION':
            output += "       • Suite: ${resultado.suite ?: 'unknown'}\n"
            output += "       • Total: ${resultado.total ?: 0}\n"
            output += "       • Passed: ${resultado.passed ?: 0}\n"
            output += "       • Failed: ${resultado.failed ?: 0}\n"
            break
    }
    return output
}

def obtenerEntorno(String branch) {
    if (branch == 'master' || branch == 'main') {
        return 'PRODUCCION'
    } else if (branch == 'develop') {
        return 'DESARROLLO'
    } else if (branch.startsWith('feature/')) {
        return 'FEATURE'
    } else if (branch.startsWith('hotfix/')) {
        return 'HOTFIX'
    } else {
        return 'OTROS'
    }
}

def obtenerDescripcionError(Map resultado) {
    if (resultado.failed && resultado.total) {
        return "Fallaron ${resultado.failed} de ${resultado.total} tests"
    } else if (resultado.score) {
        return "Score insuficiente: ${resultado.score}/100"
    } else {
        return "Error en validación"
    }
}
