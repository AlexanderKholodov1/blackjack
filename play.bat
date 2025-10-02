@echo off
echo ============================================
echo       BLACKJACK - Compilar y Jugar
echo ============================================
echo.

echo Compilando BlackJack.java...
javac BlackJack.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilacion exitosa! Iniciando juego...
    echo ============================================
    echo.
    java BlackJack
) else (
    echo.
    echo ERROR: No se pudo compilar el juego.
    echo.
    echo Posibles causas:
    echo - Java JDK no esta instalado
    echo - Java no esta en el PATH del sistema
    echo - Hay errores en el codigo Java
    echo.
    echo Para instalar Java JDK:
    echo 1. Ve a https://www.oracle.com/java/technologies/downloads/
    echo 2. Descarga Java JDK
    echo 3. Instala y reinicia la computadora
)

echo.
echo Presiona cualquier tecla para salir...
pause > nul