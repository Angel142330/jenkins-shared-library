package com.jenkins.mysql

@Grab('com.mysql:mysql-connector-j:8.2.0')
import groovy.sql.Sql
import java.sql.SQLException

/**
 * Clase para manejar conexiones MySQL desde Jenkins
 */
class MySQLConnection {
    
    private String host
    private String port
    private String database
    private String user
    private String password
    private Sql sql
    
    /**
     * Constructor
     */
    MySQLConnection(Map config = [:]) {
        this.host = config.host ?: 'localhost'
        this.port = config.port ?: '3306'
        this.database = config.database ?: 'jenkins_metrics'
        this.user = config.user ?: 'jenkins_user'
        this.password = config.password ?: 'Jenkins@2025!'
    }
    
    /**
     * Obtener conexión SQL
     */
    Sql getConnection() {
        if (sql == null) {
            try {
                def jdbcUrl = "jdbc:mysql://${host}:${port}/${database}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                sql = Sql.newInstance(jdbcUrl, user, password, 'com.mysql.cj.jdbc.Driver')
                println "✅ Conexión MySQL establecida: ${database}@${host}"
            } catch (Exception e) {
                println "❌ Error conectando a MySQL: ${e.message}"
                throw e
            }
        }
        return sql
    }
    
    /**
     * Cerrar conexión
     */
    void close() {
        if (sql != null) {
            try {
                sql.close()
                println "🔌 Conexión MySQL cerrada"
            } catch (Exception e) {
                println "⚠️  Error cerrando conexión: ${e.message}"
            }
        }
    }
    
    /**
     * Ejecutar query con manejo de errores
     */
    def executeQuery(String query, List params = []) {
        def conn = getConnection()
        try {
            if (params.isEmpty()) {
                return conn.rows(query)
            } else {
                return conn.rows(query, params)
            }
        } catch (SQLException e) {
            println "❌ Error ejecutando query: ${e.message}"
            println "Query: ${query}"
            throw e
        }
    }
    
    /**
     * Ejecutar insert/update con manejo de errores
     */
    def executeUpdate(String query, List params = []) {
        def conn = getConnection()
        try {
            if (params.isEmpty()) {
                return conn.executeUpdate(query)
            } else {
                return conn.executeUpdate(query, params)
            }
        } catch (SQLException e) {
            println "❌ Error ejecutando update: ${e.message}"
            println "Query: ${query}"
            throw e
        }
    }
    
    /**
     * Obtener último ID insertado
     */
    def getLastInsertId() {
        def result = executeQuery("SELECT LAST_INSERT_ID() as id")
        return result[0].id
    }
}

