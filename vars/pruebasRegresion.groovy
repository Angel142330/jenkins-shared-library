/**
 * Ejecutor de pruebas de regresión
 * Utilizado principalmente en rama master
 */

def call(Map config = [:]) {
    def exceptionList = config.exceptionList ?: []
    def suites = config.suites ?: ['smoke', 'regression', 'e2e']
    
    echo "🔄 Iniciando pruebas de regresión..."
    
    try {
        // Verificar si está excepcionado
        def excepcionado = exceptionList.contains(env.BRANCH_NAME) || exceptionList.contains('*')
        
        if (excepcionado) {
            echo "✅ Rama excepcionada - Continuando sin pruebas de regresión"
            return [success: true, skipped: true, excepcionado: true]
        }
        
        def resultados = []
        def todosExitosos = true
        
        for (suite in suites) {
            echo "▶️  Ejecutando suite: ${suite}"
            def resultado = ejecutarSuite(suite)
            resultados << resultado
            
            if (!resultado.success) {
                todosExitosos = false
                echo "❌ Suite ${suite} falló"
            } else {
                echo "✅ Suite ${suite} completada exitosamente"
            }
        }
        
        if (todosExitosos) {
            echo "✅ Todas las pruebas de regresión exitosas"
            return [success: true, resultados: resultados]
        } else {
            echo "❌ Algunas pruebas de regresión fallaron"
            if (config.failOnError != false) {
                error("Pruebas de regresión fallaron")
            }
            return [success: false, resultados: resultados]
        }
    } catch (Exception e) {
        echo "❌ Error ejecutando pruebas de regresión: ${e.message}"
        throw e
    }
}

def ejecutarSuite(String suite) {
    echo "  → Ejecutando suite ${suite}..."
    sleep(3) // Simular ejecución
    
    def random = new Random()
    def total = 20 + random.nextInt(30)
    def failed = random.nextInt(3)
    def passed = total - failed
    
    return [
        suite: suite,
        success: failed == 0,
        total: total,
        passed: passed,
        failed: failed
    ]
}

