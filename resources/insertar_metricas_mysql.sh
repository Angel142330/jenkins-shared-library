#!/bin/bash

###############################################################################
# Script para insertar métricas en MySQL
# Se ejecuta desde Jenkins pipeline
###############################################################################

# Parámetros recibidos
TIPO_VALIDACION="${1:-UNITARIA}"
COMMIT="${2:-N/A}"
BUILD_NUMBER="${3:-0}"
ID_PROYECTO="${4:-proyecto}"
APLICATIVO="${5:-app}"
RAMA="${6:-main}"
RESULTADO="${7:-SUCCESS}"
ENTORNO="${8:-DESARROLLO}"
TIMESTAMP="${9:-$(date '+%Y-%m-%d %H:%M:%S')}"

# Configuración MySQL
DB_HOST="localhost"
DB_USER="jenkins_user"
DB_PASS="Jenkins@2025!"
DB_NAME="jenkins_metrics"

# Función para insertar en pipeline_executions
insertar_execution() {
    mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
    INSERT INTO pipeline_executions 
    (commit_hash, build_number, id_proyecto, aplicativo, rama, resultado, timestamp, entorno, duracion_segundos)
    VALUES 
    ('$COMMIT', '$BUILD_NUMBER', '$ID_PROYECTO', '$APLICATIVO', '$RAMA', '$RESULTADO', '$TIMESTAMP', '$ENTORNO', 0);
    " 2>/dev/null
    
    # Obtener el ID insertado
    EXECUTION_ID=$(mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -sN -e "SELECT LAST_INSERT_ID();")
    echo "$EXECUTION_ID"
}

# Función para insertar métricas de Kiuwan
insertar_kiuwan() {
    local EXECUTION_ID=$1
    local SCORE=${10:-85}
    local ESTADO=${11:-OK}
    local UMBRAL=${12:-70}
    local EXCEPCION=${13:-NULL}
    local DESC_EXCEPCION=${14:-NULL}
    
    [ "$EXCEPCION" = "NULL" ] && EXCEPCION="NULL" || EXCEPCION="'$EXCEPCION'"
    [ "$DESC_EXCEPCION" = "NULL" ] && DESC_EXCEPCION="NULL" || DESC_EXCEPCION="'$DESC_EXCEPCION'"
    
    mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
    INSERT INTO kiuwan_metrics 
    (execution_id, commit_hash, id_proyecto, aplicativo, rama, score, estado, umbral, excepcionado, excepcion, descripcion_excepcion, timestamp, entorno)
    VALUES 
    ($EXECUTION_ID, '$COMMIT', '$ID_PROYECTO', '$APLICATIVO', '$RAMA', $SCORE, '$ESTADO', $UMBRAL, FALSE, $EXCEPCION, $DESC_EXCEPCION, '$TIMESTAMP', '$ENTORNO');
    " 2>/dev/null
}

# Función para insertar métricas unitarias
insertar_unitarias() {
    local EXECUTION_ID=$1
    local TOTAL=${10:-50}
    local PASSED=${11:-50}
    local FAILED=${12:-0}
    local COBERTURA=${13:-0}
    local UMBRAL=${14:-80}
    local EXCEPCION=${15:-NULL}
    local DESC_EXCEPCION=${16:-NULL}
    
    [ "$EXCEPCION" = "NULL" ] && EXCEPCION="NULL" || EXCEPCION="'$EXCEPCION'"
    [ "$DESC_EXCEPCION" = "NULL" ] && DESC_EXCEPCION="NULL" || DESC_EXCEPCION="'$DESC_EXCEPCION'"
    
    mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
    INSERT INTO unitarias_metrics 
    (execution_id, commit_hash, id_proyecto, aplicativo, rama, total_tests, passed_tests, failed_tests, cobertura_porcentaje, umbral, resultado, excepcion, descripcion_excepcion, timestamp, entorno)
    VALUES 
    ($EXECUTION_ID, '$COMMIT', '$ID_PROYECTO', '$APLICATIVO', '$RAMA', $TOTAL, $PASSED, $FAILED, $COBERTURA, $UMBRAL, '$RESULTADO', $EXCEPCION, $DESC_EXCEPCION, '$TIMESTAMP', '$ENTORNO');
    " 2>/dev/null
}

# Función para insertar métricas de regresión
insertar_regresion() {
    local EXECUTION_ID=$1
    local SUITE=${10:-smoke}
    local TOTAL=${11:-25}
    local PASSED=${12:-25}
    local FAILED=${13:-0}
    local UMBRAL=${14:-100}
    local EXCEPCION=${15:-NULL}
    local DESC_EXCEPCION=${16:-NULL}
    
    [ "$EXCEPCION" = "NULL" ] && EXCEPCION="NULL" || EXCEPCION="'$EXCEPCION'"
    [ "$DESC_EXCEPCION" = "NULL" ] && DESC_EXCEPCION="NULL" || DESC_EXCEPCION="'$DESC_EXCEPCION'"
    
    mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
    INSERT INTO regresion_metrics 
    (execution_id, commit_hash, id_proyecto, aplicativo, rama, suite_name, total_tests, passed_tests, failed_tests, umbral, resultado, excepcion, descripcion_excepcion, timestamp, entorno)
    VALUES 
    ($EXECUTION_ID, '$COMMIT', '$ID_PROYECTO', '$APLICATIVO', '$RAMA', '$SUITE', $TOTAL, $PASSED, $FAILED, $UMBRAL, '$RESULTADO', $EXCEPCION, $DESC_EXCEPCION, '$TIMESTAMP', '$ENTORNO');
    " 2>/dev/null
}

# Ejecutar inserción principal
echo "📊 Insertando ejecución en BBDD..."
EXECUTION_ID=$(insertar_execution)

if [ -z "$EXECUTION_ID" ] || [ "$EXECUTION_ID" = "NULL" ]; then
    echo "❌ Error al insertar execution"
    exit 1
fi

echo "✅ Execution ID: $EXECUTION_ID"

# Insertar según tipo de validación
case "$TIPO_VALIDACION" in
    KIUWAN)
        echo "📈 Insertando métricas Kiuwan..."
        insertar_kiuwan "$EXECUTION_ID" "$@"
        ;;
    UNITARIA)
        echo "📈 Insertando métricas unitarias..."
        insertar_unitarias "$EXECUTION_ID" "$@"
        ;;
    REGRESION)
        echo "📈 Insertando métricas regresión..."
        insertar_regresion "$EXECUTION_ID" "$@"
        ;;
esac

echo "✅ Métricas insertadas correctamente en MySQL"
echo "   - Tabla: ${TIPO_VALIDACION,,}_metrics"
echo "   - Execution ID: $EXECUTION_ID"
echo "   - Timestamp: $TIMESTAMP"

