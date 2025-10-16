/**
 * Ejecutor de pruebas unitarias
 * Lee configuración del pom.xml y ejecuta según corresponda
 */

def call(Map config = [:]) {
    def skipTests = config.skipTests ?: false
    def pomFile = config.pomFile ?: 'pom.xml'
    def exceptionList = config.exceptionList ?: []
    def insertarBBDD = config.insertarBBDD ?: false
    
    echo "🧪 Iniciando validador de pruebas unitarias..."
    
    try {
        // Verificar si está excepcionado
        def excepcionado = exceptionList.contains(env.BRANCH_NAME) || exceptionList.contains('*')
        
        if (excepcionado) {
            echo "✅ Rama excepcionada - Continuando sin pruebas unitarias"
            return [success: true, skipped: true, excepcionado: true]
        }
        
        // Leer configuración del pom.xml si existe
        if (fileExists(pomFile)) {
            def skipFromPom = leerConfiguracionPom(pomFile)
            skipTests = skipFromPom
        }
        
        if (skipTests) {
            echo "⏭️  Saltando pruebas unitarias según configuración"
            compilar()
            return [success: true, skipped: true]
        } else {
            echo "▶️  Ejecutando pruebas unitarias..."
            compilar()
            def resultado = ejecutarPruebas()
            
            if (resultado.success) {
                echo "✅ Pruebas unitarias exitosas: ${resultado.passed}/${resultado.total}"
                
            if (insertarBBDD) {
                // Usar función global de inserción de métricas
                insertarMetricasBBDD(
                    tipo: 'UNITARIA',
                    resultado: resultado,
                    branch: env.BRANCH_NAME,
                    umbral: 80
                )
            }
                
                return resultado
            } else {
                echo "❌ Pruebas unitarias fallidas: ${resultado.failed}/${resultado.total} tests fallaron"
                if (config.failOnError != false) {
                    error("Pruebas unitarias fallaron")
                }
                return resultado
            }
        }
    } catch (Exception e) {
        echo "❌ Error ejecutando pruebas unitarias: ${e.message}"
        throw e
    }
}

def leerConfiguracionPom(String pomFile) {
    echo "  → Leyendo ${pomFile}..."
    
    def pomContent = readFile(pomFile)
    
    // Buscar propiedad maven.test.skip o skipTests
    if (pomContent.contains('<maven.test.skip>true</maven.test.skip>') || 
        pomContent.contains('<skipTests>true</skipTests>')) {
        echo "  → Configuración encontrada: skipTests = true"
        return true
    } else {
        echo "  → Configuración encontrada: skipTests = false"
        return false
    }
}

def compilar() {
    echo "  → Compilando proyecto..."
    sleep(2) // Simular compilación
    echo "  → Compilación completada ✓"
}

def ejecutarPruebas() {
    echo "  → Ejecutando suite de tests..."
    sleep(3) // Simular ejecución de tests
    
    // Simular resultados (100% de éxito - siempre pasa)
    def random = new Random()
    def total = 50 + random.nextInt(50)
    
    // SIEMPRE PASA - 0 fallos
    def failed = 0
    def passed = total
    
    return [
        success: true,  // Siempre true
        total: total,
        passed: passed,
        failed: failed,
        timestamp: new Date()
    ]
}

def insertarResultadosBBDD(Map resultado) {
    sleep(1) // Simular inserción en BBDD
    
    // Preparar datos completos para BBDD
    def commit = env.GIT_COMMIT ?: 'N/A'
    def idProyecto = env.JOB_NAME ?: 'proyecto-demo'
    def aplicativo = idProyecto.tokenize('/')[0] // Nombre del job sin rama
    def resultadoTest = resultado.success ? 'SUCCESS' : 'FAILURE'
    def timestamp = resultado.timestamp
    def umbral = 80 // % de tests que deben pasar
    def excepcion = resultado.success ? null : 'Tests fallidos'
    def descripcionExcepcion = resultado.success ? null : "Fallaron ${resultado.failed} de ${resultado.total} tests"
    def entorno = env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main' ? 'PRODUCCION' : 
                  env.BRANCH_NAME == 'develop' ? 'DESARROLLO' : 'FEATURE'
    
    echo "  → Resultados guardados en BBDD ✓"
    echo "    ┌─────────────────────────────────────────┐"
    echo "    │ Datos insertados en BBDD:               │"
    echo "    ├─────────────────────────────────────────┤"
    echo "    │ Commit:            ${commit.take(10)}   │"
    echo "    │ ID Proyecto:       ${idProyecto}        │"
    echo "    │ Aplicativo:        ${aplicativo}        │"
    echo "    │ Resultado Test:    ${resultadoTest}     │"
    echo "    │ Tests Ejecutados:  ${resultado.total}   │"
    echo "    │ Tests Exitosos:    ${resultado.passed}  │"
    echo "    │ Tests Fallidos:    ${resultado.failed}  │"
    echo "    │ Timestamp:         ${timestamp}         │"
    echo "    │ Umbral:            ${umbral}%           │"
    echo "    │ Excepción:         ${excepcion ?: 'N/A'}│"
    echo "    │ Desc. Excepción:   ${descripcionExcepcion ?: 'N/A'}│"
    echo "    │ Entorno:           ${entorno}           │"
    echo "    └─────────────────────────────────────────┘"
}

