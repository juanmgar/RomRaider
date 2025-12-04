# 🎮 ROM Raider

**ROM Raider** es una aplicación de escritorio desarrollada en JavaFX que permite gestionar colecciones de ROMs de videojuegos clásicos.  
Permite organizar plataformas y juegos, descargar metadatos desde Internet y mantener una copia sincronizada en la nube.

> 📚 Proyecto desarrollado como trabajo final del  
> **Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM)**  
> (modalidad a distancia, **IES Nº1 de Gijón**).

---

## 🧩 Características principales

- 🗂️ **Gestión de plataformas y ROMs**
    - CRUD completo de plataformas (consolas/emuladores) y ROMs asociadas.
    - Campos específicos: extensión de fichero, carpeta por plataforma, descripción, imagen, flags *favorito* y *jugado*.

- 🔍 **Pantalla principal unificada**
    - Panel de plataformas + panel de ROMs + panel de detalle.
    - Búsqueda por texto y filtros por estado (jugado / no jugado) y favoritos.
    - Detalle con carátula, descripción y acciones de edición/eliminación.

- 🌐 **Autenticación con Supabase**
    - Registro e inicio de sesión mediante correo y contraseña (backend Supabase).
    - Opción *Remember me* con restauración de sesión usando *refresh token*.
    - Cierre de sesión que limpia la sesión local y los tokens almacenados.

- 🔄 **Sincronización nube ↔ local (Supabase + H2)**
    - Cada usuario tiene su propia colección en Supabase (tablas `plataformas`, `roms`, `sync_status`).
    - Sincronización manual iniciada por el usuario con botón **Sync**.
    - Estrategia basada en timestamps:
        - Primera sincronización: descarga completa desde la nube.
        - Sincronizaciones posteriores: compara *última sync*, *última edición local* y *última actualización remota* para decidir si:
            - Descargar colección remota, o
            - Subir colección local.
    - Registro de la última sincronización en local y en la tabla `sync_status` remota.

- 📡 **Modo offline completo**
    - Toda la gestión de plataformas y ROMs funciona sin conexión (H2 embebida).
    - Se puede continuar usando la aplicación sin autenticarse.
    - Las operaciones dependientes de Internet (login, RAWG, sincronización) muestran mensajes informativos cuando no hay red.
    - El usuario puede gestionar colecciones locales, exportarlas/importarlas en XML y decidir más adelante si quiere conectarlas con la nube.

- 🧬 **Metadatos automáticos desde RAWG.io**
    - Integración con la API pública de **RAWG.io**.
    - Búsqueda por título y recuperación de:
        - descripción en texto plano (`description_raw`),
        - imagen principal (`background_image`).
    - Botones de “Update info” para completar o actualizar los datos de un juego.

- 📤📥 **Importación y exportación en XML (JAXB)**
    - Exportación de la colección completa (plataformas + ROMs) a un fichero XML.
    - Importación de colecciones previamente exportadas, reconstruyendo la base de datos local.
    - Pensado tanto para backup como para migrar colecciones entre equipos.

- 🌍 **Internacionalización (i18n)**
    - Textos de la interfaz gestionados con ficheros `*.properties`.
    - Aplicación disponible, al menos, en **español** e **inglés**.
    - Configurable desde el menú de preferencias.

- 📊 **Estadísticas de la colección**
    - Cálculo de totales por:
        - número de ROMs por plataforma,
        - jugados vs no jugados,
        - favoritos vs no favoritos.
    - Visualización mediante gráficos (barras y/o circulares).

- 🧪 **Pruebas y calidad**
    - Pruebas unitarias (JUnit 5) sobre utilidades clave (propiedades, imágenes, sonido, XML…).
    - Informes de cobertura con **JaCoCo**.
    - Análisis estático con **SonarCloud**.

- ⚙️ **CI/CD y empaquetado multiplataforma**
    - Workflows de **GitHub Actions** que:
        - Inyectan las credenciales (`secrets.properties`) antes del build.
        - Ejecutan `mvn verify` + análisis SonarCloud.
        - Descargan JavaFX SDK para Windows, Linux y macOS.
        - Generan ZIPs listos para usar en cada plataforma (JAR + JavaFX + script de arranque).

---

## 🏗️ Arquitectura general

ROM Raider sigue una arquitectura por capas y combina una base de datos local con servicios en la nube:

```mermaid
flowchart LR
    subgraph Desktop["Aplicación de Escritorio (JavaFX)"]
        UI["UI JavaFX (FXML + CSS)"]
        Controllers["Controladores JavaFX\n(MainController, LoginController, etc.)"]
        Services["Servicios de dominio\n(PlataformaService, RomService, ... )"]
        DBLocal["DB local H2 + JPA/Hibernate"]
    end

    subgraph Cloud["Nube"]
        Supabase["Supabase\n(PostgreSQL + Auth + REST)"]
        RAWG["RAWG.io API\nMetadatos videojuegos"]
    end

    UI --> Controllers
    Controllers --> Services
    Services --> DBLocal

    Services -->|"Autenticación, sincronización"| Supabase
    Services -->|"Metadatos de ROMs"| RAWG
