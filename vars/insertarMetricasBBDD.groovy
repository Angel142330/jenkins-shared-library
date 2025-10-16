/**
 * Función global para insertar métricas en BBDD
 * Puede ser llamada desde cualquier validador
 */

def call(Map config = [:]) {
    def tipoValidacion = config.tipo ?: 'UNITARIA'
    def resultado = config.resultado ?: [:]
    def branch = config.branch ?: env.BRANCH_NAME ?: 'unknown'
    
    echo "💾 Insertando métricas en BBDD..."
    sleep(1) // Simular inserción
    
    // Preparar datos completos para BBDD
    def commit = env.GIT_COMMIT ?: env.GIT_REVISION ?: 'N/A'
    def buildNumber = env.BUILD_NUMBER ?: '0'
    def idProyecto = env.JOB_NAME ?: 'proyecto-demo'
    def aplicativo = idProyecto.tokenize('/')[0] // Nombre del job sin rama
    def resultadoTest = resultado.success ? 'SUCCESS' : 'FAILURE'
    def timestamp = new Date()
    def umbral = config.umbral ?: 80
    def excepcion = resultado.success ? null : (resultado.excepcion ?: 'Test fallido')
    def descripcionExcepcion = resultado.success ? null : (resultado.descripcionExcepcion ?: obtenerDescripcionError(resultado))
    def entorno = obtenerEntorno(branch)
    
    // Datos específicos según tipo de validación
    def metricas = [:]
    switch(tipoValidacion) {
        case 'KIUWAN':
            metricas = [
                score: resultado.score ?: 0,
                estado: resultado.estado ?: 'UNKNOWN'
            ]
            break
        case 'UNITARIA':
            metricas = [
                total: resultado.total ?: 0,
                passed: resultado.passed ?: 0,
                failed: resultado.failed ?: 0,
                cobertura: resultado.cobertura ?: 0
            ]
            break
        case 'REGRESION':
            metricas = [
                suites: resultado.suites ?: [],
                totalTests: resultado.totalTests ?: 0,
                passedTests: resultado.passedTests ?: 0,
                failedTests: resultado.failedTests ?: 0
            ]
            break
    }
    
    // Mostrar datos que se insertarían en BBDD
    echo """
    ╔═══════════════════════════════════════════════════════════════╗
    ║           INSERCIÓN EN BBDD - ${tipoValidacion.padRight(30)}║
    ╚═══════════════════════════════════════════════════════════════╝
    
    📊 Datos Generales:
       • Commit:             ${commit.take(12)}
       • Build Number:       #${buildNumber}
       • ID Proyecto:        ${idProyecto}
       • Aplicativo:         ${aplicativo}
       • Rama:               ${branch}
       • Resultado:          ${resultadoTest}
       • Timestamp:          ${timestamp}
       • Umbral:             ${umbral}%
       • Entorno:            ${entorno}
    
    📈 Métricas Específicas:
${formatearMetricas(metricas)}
    
    ⚠️  Excepciones:
       • Excepción:          ${excepcion ?: 'N/A'}
       • Descripción:        ${descripcionExcepcion ?: 'N/A'}
    
    ✅ Datos insertados correctamente en BBDD
    """
    
    return [
        success: true,
        registroId: "REG-${buildNumber}-${System.currentTimeMillis()}",
        timestamp: timestamp
    ]
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

def formatearMetricas(Map metricas) {
    def output = ""
    metricas.each { key, value ->
        if (value instanceof List) {
            output += "       • ${key.capitalize()}: [${value.size()} items]\n"
        } else {
            output += "       • ${key.capitalize()}: ${value}\n"
        }
    }
    return output
}

