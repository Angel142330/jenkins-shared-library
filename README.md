# Jenkins Shared Library - Validadores

Biblioteca compartida de Jenkins para validaciones por rama (feature, develop, master).

## 📋 Funciones Disponibles

### `validadorPipeline`
Orquestador principal que ejecuta validaciones según la rama.

**Uso:**
```groovy
@Library('shared-library') _

validadorPipeline(
    branch: env.BRANCH_NAME,
    exceptionList: []
)
```

### `kiuwanValidator`
Validador de calidad de código Kiuwan.

### `pruebasUnitarias`
Ejecutor de pruebas unitarias (lee pom.xml).

### `pruebasRegresion`
Ejecutor de pruebas de regresión.

## 🌿 Comportamiento por Rama

- **feature/***: Solo Kiuwan
- **develop**: Pruebas unitarias según pom.xml
- **master**: TODAS las validaciones

## 📖 Documentación

Ver documentación completa en el repositorio del proyecto.

