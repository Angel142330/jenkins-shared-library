/**
 * Validador principal del pipeline
 * Orquesta las validaciones según la rama (feature, develop, master)
 */

def call(Map config = [:]) {
    def branch = config.branch ?: env.BRANCH_NAME ?: 'develop'
    def exceptionList = config.exceptionList ?: []
    
    echo """
╔═══════════════════════════════════════════════════════╗
║         VALIDADOR DE PIPELINE - SHARED LIBRARY       ║
║                                                       ║
║  Rama: ${branch.padRight(43)} ║
╚═══════════════════════════════════════════════════════╝
    """
    
    try {
        if (branch.startsWith('feature/')) {
            validarFeature(branch, exceptionList)
        } else if (branch == 'develop') {
            validarDevelop(branch, exceptionList)
        } else if (branch == 'master' || branch == 'main') {
            validarMaster(branch, exceptionList)
        } else {
            echo "⚠️  Rama no reconocida, aplicando validación básica..."
            validarBasico(branch)
        }
        
        echo """
╔═══════════════════════════════════════════════════════╗
║                  ✅ PIPELINE EXITOSO                  ║
╚═══════════════════════════════════════════════════════╝
        """
    } catch (Exception e) {
        echo """
╔═══════════════════════════════════════════════════════╗
║                  ❌ PIPELINE FALLIDO                  ║
║                                                       ║
║  Error: ${e.message.take(40).padRight(40)} ║
╚═══════════════════════════════════════════════════════╝
        """
        throw e
    }
}

/**
 * Validación para ramas feature/*
 * - Solo ejecuta Kiuwan
 * - Si Kiuwan es true, se puede borrar el análisis temporal
 */
def validarFeature(String branch, List exceptionList) {
    echo """
📋 VALIDACIÓN RAMA FEATURE
   → Solo se ejecuta análisis Kiuwan
   → No se ejecutan pruebas unitarias en esta rama
    """
    
    stage('Kiuwan - Feature') {
        def resultado = kiuwanValidator(
            branch: branch,
            deleteAfterAnalysis: true,
            exceptionList: exceptionList
        )
        
        if (resultado.success) {
            echo "  ✓ Kiuwan validado correctamente"
        }
    }
}

/**
 * Validación para rama develop
 * - NO ejecuta Kiuwan para unitarias
 * - Lee el pom.xml para decidir si ejecutar pruebas unitarias
 * - Si skipTests=false: compila, ejecuta pruebas e inserta en BBDD
 * - Si skipTests=true: solo compila
 */
def validarDevelop(String branch, List exceptionList) {
    echo """
📋 VALIDACIÓN RAMA DEVELOP
   → Revisa pom.xml para configuración de pruebas unitarias
   → skipTests=false: Compila + Pruebas + Insertar BBDD
   → skipTests=true: Solo compila
    """
    
    stage('Pruebas Unitarias - Develop') {
        def resultado = pruebasUnitarias(
            pomFile: 'pom.xml',
            insertarBBDD: true,
            exceptionList: exceptionList
        )
        
        if (resultado.success) {
            if (resultado.skipped) {
                echo "  ✓ Compilación completada (tests saltados)"
            } else {
                echo "  ✓ Pruebas unitarias completadas y guardadas en BBDD"
            }
        }
    }
}

/**
 * Validación para rama master
 * - Ejecuta TODAS las pruebas
 * - Kiuwan, unitarias, regresión
 * - Puede usar fichero de configuración para excepciones
 */
def validarMaster(String branch, List exceptionList) {
    echo """
📋 VALIDACIÓN RAMA MASTER
   → Se ejecutan TODAS las validaciones
   → 1. Análisis Kiuwan
   → 2. Pruebas Unitarias
   → 3. Pruebas de Regresión
    """
    
    // Leer configuración obligatoria si existe
    def configObligatoria = leerConfiguracionMaster()
    
    stage('1. Kiuwan - Master') {
        def resultado = kiuwanValidator(
            branch: branch,
            deleteAfterAnalysis: false,
            exceptionList: configObligatoria.exceptionKiuwan ?: exceptionList,
            failOnError: configObligatoria.kiuwanObligatorio
        )
        
        if (resultado.success) {
            echo "  ✓ Kiuwan validado"
        }
    }
    
    stage('2. Pruebas Unitarias - Master') {
        def resultado = pruebasUnitarias(
            pomFile: 'pom.xml',
            insertarBBDD: true,
            exceptionList: configObligatoria.exceptionUnitarias ?: exceptionList,
            failOnError: configObligatoria.unitariasObligatorias
        )
        
        if (resultado.success) {
            echo "  ✓ Pruebas unitarias completadas"
        }
    }
    
    stage('3. Pruebas Regresión - Master') {
        def resultado = pruebasRegresion(
            suites: ['smoke', 'regression', 'e2e', 'performance'],
            exceptionList: configObligatoria.exceptionRegresion ?: exceptionList,
            failOnError: configObligatoria.regresionObligatoria
        )
        
        if (resultado.success) {
            echo "  ✓ Pruebas de regresión completadas"
        }
    }
}

def validarBasico(String branch) {
    echo "Ejecutando validación básica para rama: ${branch}"
    
    stage('Compilación Básica') {
        echo "  → Compilando proyecto..."
        sleep(2)
        echo "  ✓ Compilación completada"
    }
}

/**
 * Lee fichero de configuración obligatoria para master
 */
def leerConfiguracionMaster() {
    def configFile = 'pipeline-config.json'
    
    if (fileExists(configFile)) {
        echo "  → Leyendo configuración obligatoria desde ${configFile}"
        def config = readJSON file: configFile
        return config
    } else {
        echo "  → No se encontró ${configFile}, usando configuración por defecto"
        return [
            kiuwanObligatorio: true,
            unitariasObligatorias: true,
            regresionObligatoria: true,
            exceptionKiuwan: [],
            exceptionUnitarias: [],
            exceptionRegresion: []
        ]
    }
}

