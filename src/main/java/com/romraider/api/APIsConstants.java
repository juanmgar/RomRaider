package com.romraider.api;

public final class APIsConstants {

    private APIsConstants() {}

    // Headers
    public static final String APIKEY = "apikey";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String PREFER = "Prefer";
    public static final String RETURN_REPRESENTATION = "return=representation";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SUB = "sub";

    // Methods
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";

    // Common fields
    public static final String ID = "id";
    public static final String USER_ID = "user_id";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    // Plataforma fields
    public static final String NOMBRE = "nombre";
    public static final String EXTENSION_ROM  = "extension_rom";
    public static final String CARPETA = "carpeta";
    public static final String PLATAFORMA_ID = "plataforma_id";

    // ROM fields
    public static final String TITULO = "titulo";
    public static final String DESCRIPCION = "descripcion";
    public static final String IMAGEN = "imagen";
    public static final String FAVORITO = "favorito";
    public static final String JUGADO = "jugado";
    public static final String RUTA = "ruta";

    // Query
    public static final String SELECT_ALL = "&select=*";
}
