# Blackjack Multijugador

Juego de Blackjack en consola con **modo Singleplayer** y **Multiplayer** con descubrimiento automático de jugadores en red local.

## Características

- **Modo Singleplayer**: Juega contra la casa con reglas oficiales de Blackjack
- **Modo Multiplayer**: Juega contra otro jugador en tiempo real
- **Descubrimiento automático**: Encuentra partidas en la red local sin configuración
- **Protocolo robusto**: Sincronización perfecta de turnos y estados
- **Detección mejorada de WiFi**: Especialmente optimizada para redes WiFi de Windows
- **Logging detallado**: Información de debug para diagnóstico de problemas de red
- **Interfaz en español**: Todo en español con terminología profesional
- **Un solo archivo**: Fácil de compartir y ejecutar
- **Script automático**: Compila y ejecuta con un solo click

## Mejoras para WiFi (v2.0)

Este juego ha sido mejorado específicamente para funcionar mejor en redes WiFi:

- **Detección avanzada de interfaces**: Reconoce interfaces WiFi por nombre (Wi-Fi, Wireless, WLAN, 802.11)
- **Filtrado de interfaces virtuales**: Evita VPN, Docker, VMware y otras interfaces virtuales
- **Múltiples direcciones broadcast**: Prueba varias direcciones de broadcast para mejor compatibilidad
- **Timeout extendido**: 4 segundos de búsqueda para dar tiempo a redes WiFi más lentas
- **Logging completo**: Muestra qué interfaces de red se detectan y por qué se seleccionan o descartan

## Requisitos

- **Java JDK 8 o superior** instalado
- Para Multiplayer: Computadoras en la **misma red WiFi/LAN**

## Solución de problemas WiFi

Si no detecta otros jugadores en WiFi:

1. **Verifica el output de debug**: El juego muestra qué interfaces de red detecta
2. **Asegúrate de estar en la misma red**: Ambos jugadores deben estar en la misma red WiFi
3. **Firewall de Windows**: Puede que necesites permitir Java en el firewall
4. **Red de invitados**: Algunas redes WiFi de invitados bloquean comunicación entre dispositivos

### ¿Cómo verificar si tienes Java?

Abre PowerShell o CMD y escribe:
```powershell
java -version
```

Si no tienes Java, descárgalo de: https://www.oracle.com/java/technologies/downloads/

## 🚀 Instalación y Ejecución

### Método Rápido (Recomendado)

1. Descarga o clona este repositorio
2. **Doble click en `play.bat`**
3. ¡Listo! El juego se compilará y ejecutará automáticamente

### Método Manual

```powershell
javac WhiteJack.java
java WhiteJack
```

## 🎯 Cómo Jugar

### Modo Singleplayer

1. Ejecuta el programa
2. Selecciona opción `1` (Singleplayer)
3. Juega contra la casa
4. Comandos:
   - `h` = HIT (Pedir carta)
   - `s` = STAND (Plantarse)
   - `q` = QUIT (Salir)
   - `s` = Plantarse

### Modo Multiplayer

#### 🖥️ Jugador 1 (Host):
1. Ejecuta el programa (`play.bat`)
2. Selecciona `2` (Multiplayer)
3. Selecciona `1` (Crear partida)
4. Espera a que se conecte el otro jugador
5. Tu IP aparecerá automáticamente para el otro jugador

#### 💻 Jugador 2 (Cliente):
1. Ejecuta el programa en otra computadora
2. Selecciona `2` (Multiplayer)
3. Selecciona `2` (Buscar partidas)
4. Verás una lista de partidas disponibles con sus IPs
5. Selecciona el número de la partida
6. Si no aparece, presiona `0` para refrescar

#### Durante el juego:
- `h` = HIT (Pedir carta)
- `s` = STAND (Plantarse)
- `q` = QUIT (Salir - notifica al oponente)

**Nota**: Ambos jugadores deben confirmar si quieren jugar otra vez. Si uno dice "no", ambos vuelven al menú.

## 🌐 Configuración de Red

### Puertos utilizados:
- **Puerto 5555**: Comunicación del juego (TCP)
- **Puerto 5556**: Descubrimiento automático (UDP Broadcast)

### Firewall de Windows:

Si no encuentras partidas, permite Java en el firewall:

1. Busca "Firewall de Windows" en el inicio
2. Click en "Permitir una aplicación"
3. Busca o agrega Java
4. Marca "Privada" y "Pública"

## 🔧 Solución de Problemas

**❌ No encuentra partidas:**
- ✅ Verifica que ambas computadoras estén en la **misma red WiFi**
- ✅ Desactiva temporalmente el firewall/antivirus para probar
- ✅ Asegúrate de que el Host haya creado la partida primero
- ✅ Ambos deben ejecutar el mismo programa

**❌ Error de compilación:**
- ✅ Verifica que Java **JDK** esté instalado (no solo JRE)
- ✅ Ejecuta `javac -version` para verificar

**❌ Detecta IP incorrecta (VPN):**
- ✅ El programa ahora detecta automáticamente tu IP local (192.168.x.x)
- ✅ Si usas VPN, desconéctala para jugar en red local

## 📁 Estructura del Proyecto

```
WhiteJack/
├── WhiteJack.java     # Código fuente principal (719 líneas)
├── play.bat           # Ejecutable único (compila y ejecuta)
├── README.md          # Documentación
└── .gitignore         # Archivos ignorados por Git
```

## 🎓 Proyecto Académico

Desarrollado para el curso **CMP-3002** en la Universidad San Francisco de Quito.

**Fecha**: Octubre 2025  
**Versión**: 1.0

## 📝 Reglas del Blackjack

- 🎯 El objetivo es llegar lo más cerca posible a **21** sin pasarse
- 🃏 Las cartas **J, Q, K** valen **10**
- 🅰️ El **As** vale **1 u 11** (lo que sea mejor para tu mano)
- ❌ Si te pasas de **21**, pierdes automáticamente
- 🏠 El dealer roba hasta tener **17** o más
- ⚖️ En caso de empate, nadie gana

## 🛠️ Características Técnicas

- **Lenguaje**: Java 8+
- **Arquitectura**: Cliente-Servidor con roles intercambiables
- **Protocolo**: TCP para juego, UDP para descubrimiento
- **Detección de IP**: Filtra IPv6 y VPN, solo IPv4 local
- **Sincronización**: Protocolo de confirmación mutua para nueva partida
- **Manejo de errores**: Control de desconexiones y timeouts

## 📄 Licencia

Este proyecto es de código abierto y está disponible para uso educativo.

---

**¡Disfruta el juego! 🎰**

