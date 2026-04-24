# api-gestion 

Sistema de gestión de proyectos y dashboard analítico desarrollado con Spring Boot. Este proyecto proporciona una API robusta para la administración de proyectos, hitos y métricas de seguimiento.

##  Tecnologías

- **Lenguaje:** Java 25
- **Framework:** Spring Boot 4.0.5
- **Persistencia:** Spring Data JPA + Hibernate
- **Base de Datos:** PostgreSQL
- **Documentación:** Swagger / OpenAPI 3
- **Seguridad:** OAuth2 Resource Server
- **Herramientas:** Maven, Lombok

##  Requisitos Previos

- **Java JDK 25** o superior.
- **Maven 3.8+**
- **PostgreSQL** (configurado con el esquema).

##  Configuración y Ejecución

### 1. Clonar el repositorio
```bash
git clone <url-del-repositorio>
cd api-gestion
```

### 2. Configurar Propiedades
El archivo `application.properties` está excluido del repositorio por seguridad. Debes crear uno basado en la plantilla:

```bash
cp src/main/resources/application.properties.template src/main/resources/application.properties
```

Edita `src/main/resources/application.properties` con tus credenciales locales de base de datos.

### 3. Compilar y Ejecutar
```bash
mvn clean install
mvn spring-boot:run
```

##  Documentación de la API

Una vez que la aplicación esté corriendo, puedes acceder a la interfaz de Swagger para explorar y probar los endpoints:

 [http://localhost:8080/swagger-ui.html]

### Endpoints Principales:
- **/api/dashboard**: Métricas y estadísticas consolidadas.


## Seguridad

El proyecto está configurado como un **OAuth2 Resource Server**. Asegúrate de configurar las propiedades de validación de JWT (como `spring.security.oauth2.resourceserver.jwt.issuer-uri`) si planeas habilitar la seguridad en producción.

---
**Código configurado base sobre este proyecto**
