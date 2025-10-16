/**
 * Validador de Kiuwan para Jenkins Shared Library
 * Simula análisis de calidad de código con Kiuwan
 */

def call(Map config = [:]) {
    def branch = config.branch ?: env.BRANCH_NAME ?: 'develop'
    def deleteAfterAnalysis = config.deleteAfterAnalysis ?: false
    def exceptionList = config.exceptionList ?: []
    
    echo "🔍 Ejecutando validador Kiuwan para rama: ${branch}"
    
    try {
        // Simular análisis de Kiuwan
        def resultado = analizarKiuwan(branch, exceptionList)
        
        if (resultado.estado == 'OK' || resultado.excepcionado) {
            echo "✅ Kiuwan: ${resultado.mensaje}"
            
            if (deleteAfterAnalysis) {
                echo "🗑️  Borrando análisis temporal de Kiuwan..."
                sleep(1) // Simular borrado
            }
            
            return [success: true, data: resultado]
        } else {
            echo "❌ Kiuwan: ${resultado.mensaje}"
            if (config.failOnError != false) {
                error("Análisis de Kiuwan falló")
            }
            return [success: false, data: resultado]
        }
    } catch (Exception e) {
        echo "❌ Error ejecutando Kiuwan: ${e.message}"
        throw e
    }
}

def analizarKiuwan(String branch, List exceptionList) {
    echo "  → Analizando calidad de código..."
    sleep(2) // Simular análisis
    
    // Simular resultados (100% de éxito - siempre pasa)
    def random = new Random()
    
    // SIEMPRE genera score entre 75-95 (siempre pasa)
    def score = 75 + random.nextInt(21) // 75-95
    
    // Verificar si está en lista de excepciones
    def excepcionado = exceptionList.contains(branch) || exceptionList.contains('*')
    
    if (excepcionado) {
        return [
            estado: 'OK',
            score: score,
            mensaje: "Rama ${branch} está en lista de excepciones - Marcado como OK",
            excepcionado: true
        ]
    }
    
    // Siempre retorna OK (score siempre >= 70)
    return [
        estado: 'OK',
        score: score,
        mensaje: "Calidad de código aceptable (Score: ${score}/100)",
        excepcionado: false
    ]
}

