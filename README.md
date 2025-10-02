# Blackjack Multijugador

Juego de Blackjack en consola con **modo Singleplayer** y **Multiplayer** con descubrimiento automático de jugadores en red local.

## Características

- **Modo Singleplayer**: Juega contra la casa con reglas oficiales de Blackjack
- **Modo Multiplayer**: Juega contra otro jugador en tiempo real
- **Descubrimiento automático**: Encuentra partidas en la red local sin configuración con IP (192.168.x.x)
- **Un solo archivo**: Fácil de compartir y ejecutar

## Requisitos

- **Java JDK 8 o superior** instalado
- Para Multiplayer: Computadoras en la **misma red WiFi/LAN**

### Si no tienes Java, el programa te notificará de ello

Si no tienes Java, descárgalo de: https://www.oracle.com/java/technologies/downloads/

## Instalación y Ejecución

1. Descarga o clona este repositorio
2. **Doble click en `play.bat`**
3. ¡Listo! El juego se compilará y ejecutará automáticamente

## Cómo Jugar

### Singleplayer

1. Ejecuta el programa
2. Selecciona opción `1` (Singleplayer)
3. Juega contra la casa
4. Comandos:
   - `h` = HIT (Pedir carta)
   - `s` = STAND (Plantarse)
   - `q` = QUIT (Salir)

### Multiplayer

#### Jugador 1 (Host):
1. Ejecuta el programa (`play.bat`)
2. Selecciona `2` (Multiplayer)
3. Selecciona `1` (Crear partida)
4. Espera a que se conecte el otro jugador
* Tu IP aparecerá automáticamente para el otro jugador
6. ¡Juega!

#### Jugador 2 (Cliente):
1. Ejecuta el programa en otra computadora
2. Selecciona `2` (Multiplayer)
3. Selecciona `2` (Buscar partidas)
4. Verás una lista de partidas abiertas con sus IPs
5. Selecciona el número de la partida
* Si no aparece, presiona `0` para refrescar
6. ¡Juega!

#### Durante el juego:
- `h` = HIT (Pedir carta)
- `s` = STAND (Plantarse)
- `q` = QUIT (Salir)

## Funcionamiento del código

### Puertos utilizados:
- **Puerto 5555**: Comunicación del juego (TCP)
- **Puerto 5556**: Descubrimiento automático (UDP Broadcast)

### Firewall de Windows:

Si no encuentras partidas, permite Java en el firewall:

1. Busca "Firewall de Windows" en el inicio
2. Click en "Permitir una aplicación"
3. Busca o agrega Java
4. Marca "Privada" y "Pública"

## Estructura del Proyecto

```
WhiteJack/
├── WhiteJack.java     # Código fuente principal (719 líneas)
├── play.bat           # Ejecutable único (compila y ejecuta)
├── README.md          # Documentación
└── .gitignore         # Archivos ignorados por Git
```

## Proyecto Académico

Desarrollado para el curso **CMP-3002.3694** en la Universidad San Francisco de Quito.

**Fecha**: 2 de octubre 2025  
**Versión**: 1.0

## Reglas del Blackjack

- El objetivo es llegar lo más cerca posible a **21** sin pasarse
- Las cartas **J, Q, K** valen **10**
- El **As** vale **1 u 11** (lo que sea mejor para tu mano)
- Si te pasas de **21**, pierdes automáticamente
- En caso de empate, nadie gana

## Características Técnicas

- **Lenguaje**: Java 8+
- **Arquitectura**: Cliente-Servidor con roles intercambiables
- **Protocolo**: TCP para juego, UDP para descubrimiento
- **Detección de IP**: Filtra IPv6 y VPN, solo IPv4 local
- **Sincronización**: Protocolo de confirmación mutua para nueva partida
- **Manejo de errores**: Control de desconexiones y timeouts

## Licencia

Este proyecto es de código abierto y está disponible para uso educativo.

---

**¡Disfruta el juego!**


