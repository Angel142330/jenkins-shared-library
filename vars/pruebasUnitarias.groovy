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
                    echo "💾 Insertando resultados en BBDD..."
                    insertarResultadosBBDD(resultado)
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
    echo "  → Resultados guardados en BBDD ✓"
    echo "    - Tests ejecutados: ${resultado.total}"
    echo "    - Tests exitosos: ${resultado.passed}"
    echo "    - Timestamp: ${resultado.timestamp}"
}

