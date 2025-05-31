# 🎮 ROM Raider

**ROM Raider** es una aplicación de escritorio desarrollada en JavaFX que permite gestionar colecciones de ROMs de videojuegos clásicos. El sistema está pensado para que los usuarios puedan organizar, describir, visualizar y actualizar metadatos de sus ROMs de forma local y sencilla.

> 📚 Este proyecto ha sido desarrollado como trabajo final del **Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM)**, modalidad a distancia, en el **IES Nº1 de Gijón**.

---

## 🧩 Características principales

- 🗂️ Gestión de plataformas (consolas) y sus ROMs asociadas.
- 🖼️ Visualización de carátulas de los juegos.
- 📝 Descripciones personalizadas y obtenidas desde la API de RAWG.io.
- ⭐ Marcado de ROMs como favoritas o jugadas.
- 🔒 Inicio de sesión básico (offline/hardcoded).
- 🔁 Botón de sincronización para actualizaciones controladas.
- 💾 Persistencia mediante base de datos local H2 con acceso JPA/Hibernate.
- 🌐 Preparado para exportación de la colección a XML con JAXB.

---

## 🚀 Tecnologías utilizadas

- Java 17
- JavaFX 21
- JPA + Hibernate
- H2 Database
- SLF4J + Logback
- OkHttp (para conexión con la API de RAWG)
- JAXB (exportación XML)

