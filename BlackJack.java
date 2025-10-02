import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Blackjack Multijugador
 * 
 * Un solo programa que funciona como servidor y cliente.
 * 
 * Características:
 * - Modo Singleplayer: Juega contra la casa
 * - Modo Multiplayer: Descubre automáticamente otros jugadores en la red local
 * - Protocolo de comunicación robusto con sincronización de turnos
 * - Detección automática de IP local (192.168.x.x)
 * 
 * @version 1.0
 * @since 2025-10-02
 */
public class BlackJack {

    // Configuración de red
    private static final int PUERTO_JUEGO = 5555;
    private static final int PUERTO_BROADCAST = 5556;
    private static final String MENSAJE_BROADCAST = "BLACKJACK_GAME";
    private static final int TIMEOUT_SERVIDOR = 60000; // 1 minuto
    private static final int TIMEOUT_BUSQUEDA = 6000;  // 6 segundos (más tiempo para subredes diferentes)
    
    // Reglas del juego
    private static final int DEALER_STAND_VALUE = 17;
    private static final int BLACKJACK_TARGET = 21;

    /** ----- Clase Card ----- */
    static class Card implements Comparable<Card> {
        public static final String[] SUITS = {"Tréboles", "Diamantes", "Corazones", "Picas"};
        public static final String[] RANKS = {null, "As", "2", "3", "4", "5", "6",
                                              "7", "8", "9", "10", "J", "Q", "K"};

        private final int rank; // 1..13
        private final int suit; // 0..3

        public Card(int rank, int suit) {
            this.rank = rank;
            this.suit = suit;
        }

        public int getRank() { return rank; }
        public int getSuit() { return suit; }

        @Override
        public String toString() {
            return RANKS[rank] + " de " + SUITS[suit];
        }

        @Override
        public int compareTo(Card that) {
            if (this.suit != that.suit) return Integer.compare(this.suit, that.suit);
            return Integer.compare(this.rank, that.rank);
        }

        public int blackjackValue() {
            if (rank >= 10) return 10;
            return rank;
        }
        
        // Para serialización simple
        public String serialize() {
            return rank + "," + suit;
        }
        
        public static Card deserialize(String data) {
            String[] parts = data.split(",");
            return new Card(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    /** ----- Clase Deck: crea 52 cartas, baraja y reparte ----- */
    static class Deck {
        private final List<Card> cards = new ArrayList<>();

        public Deck() {
            for (int suit = 0; suit < 4; suit++) {
                for (int rank = 1; rank <= 13; rank++) {
                    cards.add(new Card(rank, suit));
                }
            }
        }

        public void shuffle() {
            Collections.shuffle(cards);
        }

        public Card deal() {
            if (cards.isEmpty()) throw new NoSuchElementException("No hay más cartas");
            return cards.remove(cards.size() - 1);
        }
    }

    /** ----- Utilidades para una mano de Blackjack ----- */
    static class Hand {
        private final List<Card> cards = new ArrayList<>();

        public void add(Card c) { cards.add(c); }

        public List<Card> getCards() { return cards; }

        /** Suma con Ases flexibles (1 u 11) */
        public int bestBlackjackTotal() {
            int sum = 0;
            int aces = 0;
            for (Card c : cards) {
                sum += c.blackjackValue();
                if (c.getRank() == 1) aces++; // Ace
            }
            // Elevar algunos Ases a 11 si cabe sin pasarse de 21
            while (aces > 0 && sum + 10 <= BLACKJACK_TARGET) {
                sum += 10;
                aces--;
            }
            return sum;
        }

        public boolean isBust() { return bestBlackjackTotal() > BLACKJACK_TARGET; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Card c : cards) sb.append(c).append(", ");
            if (!cards.isEmpty()) sb.setLength(sb.length() - 2);
            sb.append(" (total: ").append(bestBlackjackTotal()).append(")");
            return sb.toString();
        }
    }

    /** ----- Juego Singleplayer (contra la casa) ----- */
    static void jugarSingleplayer() {
        Scanner in = new Scanner(System.in);
        boolean jugarOtraVez = true;
        
        while (jugarOtraVez) {
            System.out.println("\n=== Nueva Partida ===");
            Deck deck = new Deck();
            deck.shuffle();

            Hand player = new Hand();
            Hand dealer = new Hand();

            // Repartir inicial
            player.add(deck.deal());
            dealer.add(deck.deal());
            player.add(deck.deal());
            dealer.add(deck.deal());

            // Mostrar estado inicial
            System.out.println("Dealer muestra: " + dealer.getCards().get(0));
            System.out.println("Tu mano: " + player);

            // Turno del jugador
            boolean jugadorPierde = false;
            while (true) {
                if (player.isBust()) {
                    System.out.println("Te pasaste de " + BLACKJACK_TARGET + ". Pierdes.");
                    jugadorPierde = true;
                    break;
                }
                System.out.print("¿HIT (h) o STAND (s) o QUIT (q)? ");
                String ans = in.nextLine().trim().toLowerCase();
                if (ans.equals("q")) {
                    System.out.println("Abandonaste la partida.");
                    return;
                } else if (ans.startsWith("h")) {
                    player.add(deck.deal());
                    System.out.println("Tu mano: " + player);
                } else if (ans.startsWith("s")) {
                    break;
                } else {
                    System.out.println("Opción no válida. Escribe 'h', 's' o 'q'.");
                }
            }

            if (!jugadorPierde) {
                // Turno del dealer (regla: roba hasta 17 o más)
                System.out.println("\nTurno del dealer...");
                System.out.println("Dealer tenía: " + dealer);
                while (dealer.bestBlackjackTotal() < DEALER_STAND_VALUE) {
                    Card c = deck.deal();
                    dealer.add(c);
                    System.out.println("Dealer roba: " + c + " -> " + dealer);
                }
                if (dealer.isBust()) {
                    System.out.println("El dealer se pasa. ¡Ganas!");
                } else {
                    // Decisión final
                    int p = player.bestBlackjackTotal();
                    int d = dealer.bestBlackjackTotal();
                    System.out.println("\nResultado final:");
                    System.out.println("Tu total: " + p + " | Dealer: " + d);
                    if (p > d) System.out.println("¡Ganas!");
                    else if (p < d) System.out.println("Pierdes.");
                    else System.out.println("Empate.");
                }
            }
            
            System.out.print("\n¿Jugar otra vez? (s/n): ");
            String respuesta = in.nextLine().trim().toLowerCase();
            jugarOtraVez = respuesta.startsWith("s");
        }
    }

    /** ----- Descubrimiento de red ----- */
    static class NetworkDiscovery extends Thread {
        private volatile boolean running = true;
        private String miIP;
        
        public NetworkDiscovery() {
            setDaemon(true);
            try {
                miIP = obtenerIPLocal();
            } catch (Exception e) {
                miIP = "desconocida";
            }
        }
        
        // Obtener la IP real de la red local (WiFi), no VPN
        private String obtenerIPLocal() throws Exception {
            System.out.println("[INFO] Detectando interfaces de red...");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            String mejorIP = null;
            String ipAlternativa = null;
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                
                String nombreInterface = ni.getDisplayName().toLowerCase();
                System.out.println("   Interface: " + ni.getDisplayName() + " (" + ni.getName() + ")");
                System.out.println("     - Activa: " + ni.isUp());
                System.out.println("     - Loopback: " + ni.isLoopback());
                System.out.println("     - Virtual: " + ni.isVirtual());
                
                // Saltar interfaces inactivas o loopback
                if (!ni.isUp() || ni.isLoopback()) {
                    System.out.println("     [SKIP] Saltada (inactiva o loopback)");
                    continue;
                }
                
                // Priorizar interfaces WiFi/Wireless
                boolean esWiFi = nombreInterface.contains("wi-fi") || 
                               nombreInterface.contains("wireless") ||
                               nombreInterface.contains("wlan") ||
                               nombreInterface.contains("802.11");
                
                // Evitar interfaces virtuales (VPN, Docker, etc.)
                boolean esVirtual = nombreInterface.contains("vmware") ||
                                  nombreInterface.contains("virtualbox") ||
                                  nombreInterface.contains("hyper-v") ||
                                  nombreInterface.contains("docker") ||
                                  nombreInterface.contains("vpn") ||
                                  nombreInterface.contains("tap") ||
                                  nombreInterface.contains("tun") ||
                                  ni.isVirtual();
                
                if (esVirtual) {
                    System.out.println("     [SKIP] Saltada (interface virtual)");
                    continue;
                }
                
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Solo IPv4
                    if (!(addr instanceof java.net.Inet4Address)) continue;
                    String ip = addr.getHostAddress();
                    
                    System.out.println("     [IP] IP encontrada: " + ip);
                    
                    // Buscar IP de red local (192.168.x.x o 10.x.x.x)
                    boolean esIPPrivada = false;
                    if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        esIPPrivada = true;
                    } else if (ip.startsWith("172.")) {
                        // También aceptar 172.16-31.x.x (rango privado)
                        String[] parts = ip.split("\\.");
                        if (parts.length >= 2) {
                            try {
                                int second = Integer.parseInt(parts[1]);
                                if (second >= 16 && second <= 31) {
                                    esIPPrivada = true;
                                }
                            } catch (NumberFormatException e) {
                                // Ignorar si no se puede parsear
                            }
                        }
                    }
                    
                    if (esIPPrivada) {
                        if (esWiFi && mejorIP == null) {
                            mejorIP = ip;
                            System.out.println("     [WIFI] IP WiFi seleccionada: " + ip);
                        } else if (ipAlternativa == null) {
                            ipAlternativa = ip;
                            System.out.println("     [ALT] IP alternativa: " + ip);
                        }
                    }
                }
            }
            
            // Priorizar IP de WiFi, luego cualquier IP privada
            String ipFinal = mejorIP != null ? mejorIP : ipAlternativa;
            
            if (ipFinal != null) {
                System.out.println("[SELECTED] IP local detectada: " + ipFinal);
                return ipFinal;
            }
            
            // Si no encuentra IP local, usar la por defecto
            String ipDefault = InetAddress.getLocalHost().getHostAddress();
            System.out.println("[DEFAULT] Usando IP por defecto: " + ipDefault);
            return ipDefault;
        }
        
        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                System.out.println("[DISCOVERY] Iniciando servidor de descubrimiento en puerto " + PUERTO_BROADCAST);
                System.out.println("[DISCOVERY] Mi IP: " + miIP);
                
                socket = new DatagramSocket(PUERTO_BROADCAST);
                socket.setBroadcast(true);
                socket.setSoTimeout(1000); // Timeout corto para poder verificar 'running'
                
                while (running) {
                    try {
                        byte[] buffer = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String mensaje = new String(packet.getData(), 0, packet.getLength());
                        String direccionRemota = packet.getAddress().getHostAddress();
                        
                        System.out.println("[RECEIVED] Solicitud recibida de " + direccionRemota + ": " + mensaje);
                        
                        if (mensaje.startsWith(MENSAJE_BROADCAST)) {
                            // No responder a nosotros mismos
                            if (!direccionRemota.equals(miIP)) {
                                // Responder con nuestra IP
                                String respuesta = MENSAJE_BROADCAST + ":" + miIP;
                                byte[] respuestaBytes = respuesta.getBytes();
                                DatagramPacket respuestaPacket = new DatagramPacket(
                                    respuestaBytes, respuestaBytes.length, 
                                    packet.getAddress(), packet.getPort()
                                );
                                socket.send(respuestaPacket);
                                System.out.println("[SENT] Respuesta enviada a " + direccionRemota + ": " + respuesta);
                            } else {
                                System.out.println("[SELF] Ignorando solicitud propia");
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout normal, continuar
                        continue;
                    }
                }
            } catch (Exception e) {
                if (running) { // Solo mostrar error si no estamos cerrando intencionalmente
                    System.out.println("[ERROR] Error en servidor de descubrimiento: " + e.getMessage());
                }
            } finally {
                if (socket != null) {
                    socket.close();
                }
                // Solo mostrar mensaje si se detuvo intencionalmente por el usuario
                if (running) {
                    System.out.println("[STOP] Servidor de descubrimiento detenido");
                }
            }
        }
        
        public void detener() {
            running = false;
            interrupt();
        }
    }
    
    static Set<String> buscarJugadores() {
        Set<String> jugadores = new HashSet<>();
        DatagramSocket socket = null;
        try {
            System.out.println("[SEARCH] Iniciando busqueda de jugadores...");
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_BUSQUEDA);
            
            // Obtener nuestra IP para calcular broadcasts dirigidos
            String miIP = null;
            try {
                NetworkDiscovery discovery = new NetworkDiscovery();
                miIP = discovery.miIP;
                System.out.println("[LOCAL] Mi IP detectada: " + miIP);
            } catch (Exception e) {
                System.out.println("[ERROR] No se pudo detectar IP local: " + e.getMessage());
            }
            
            // Generar direcciones broadcast dinámicamente basadas en la IP local
            Set<String> direccionesBroadcast = new HashSet<>();
            
            // Broadcast global
            direccionesBroadcast.add("255.255.255.255");
            
            // Si tenemos una IP local, generar broadcasts para rangos comunes de esa clase
            if (miIP != null && !miIP.equals("desconocida")) {
                String[] partes = miIP.split("\\.");
                if (partes.length == 4) {
                    String baseIP = partes[0] + "." + partes[1];
                    
                    // Agregar broadcast de nuestra subred específica
                    String broadcastLocal = baseIP + "." + partes[2] + ".255";
                    direccionesBroadcast.add(broadcastLocal);
                    System.out.println("[LOCAL] Detectada subred local: " + broadcastLocal);
                    
                    // Agregar broadcasts de subredes cercanas (útil para configuraciones complejas)
                    try {
                        int terceraParteActual = Integer.parseInt(partes[2]);
                        for (int i = Math.max(0, terceraParteActual - 10); i <= Math.min(255, terceraParteActual + 10); i++) {
                            if (i != terceraParteActual) { // No duplicar la nuestra
                                direccionesBroadcast.add(baseIP + "." + i + ".255");
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Si no se puede parsear, ignorar subredes cercanas
                    }
                }
            }
            
            // Agregar broadcasts estándar solo si no están ya incluidos
            String[] broadcastsEstandar = {
                "192.168.1.255", "192.168.0.255", "192.168.2.255",
                "10.0.0.255", "10.0.1.255", "10.1.1.255",
                "172.16.255.255", "172.31.255.255"
            };
            
            for (String broadcast : broadcastsEstandar) {
                direccionesBroadcast.add(broadcast);
            }
            
            byte[] buffer = MENSAJE_BROADCAST.getBytes();
            
            // Enviar a todas las direcciones broadcast
            for (String broadcast : direccionesBroadcast) {
                try {
                    System.out.println("[BROADCAST] Enviando a: " + broadcast);
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        InetAddress.getByName(broadcast), PUERTO_BROADCAST
                    );
                    socket.send(packet);
                } catch (Exception e) {
                    System.out.println("   [ERROR] Error enviando a " + broadcast + ": " + e.getMessage());
                }
            }
            
            System.out.println("[LISTEN] Escuchando respuestas...");
            long inicio = System.currentTimeMillis();
            int respuestasRecibidas = 0;
            
            while (System.currentTimeMillis() - inicio < TIMEOUT_BUSQUEDA) {
                try {
                    byte[] respuestaBuffer = new byte[256];
                    DatagramPacket respuesta = new DatagramPacket(respuestaBuffer, respuestaBuffer.length);
                    socket.receive(respuesta);
                    String mensaje = new String(respuesta.getData(), 0, respuesta.getLength());
                    respuestasRecibidas++;
                    
                    System.out.println("[RESPONSE] Respuesta #" + respuestasRecibidas + " de " + 
                                     respuesta.getAddress().getHostAddress() + ": " + mensaje);
                    
                    if (mensaje.startsWith(MENSAJE_BROADCAST)) {
                        String[] partes = mensaje.split(":");
                        if (partes.length > 1) {
                            String ipJugador = partes[1];
                            jugadores.add(ipJugador);
                            System.out.println("[FOUND] Jugador encontrado: " + ipJugador);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            
            System.out.println("[COMPLETE] Busqueda completada. Jugadores encontrados: " + jugadores.size());
            for (String jugador : jugadores) {
                System.out.println("   - " + jugador);
            }
            
        } catch (Exception e) {
            System.out.println("[ERROR] Error buscando jugadores: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return jugadores;
    }

    /** ----- Modo Multiplayer ----- */
    static void modoMultiplayer() {
        Scanner in = new Scanner(System.in);
        
        System.out.println("\n=== Modo Multiplayer ===");
        System.out.println("1. Crear partida (esperar jugador)");
        System.out.println("2. Buscar partidas");
        System.out.println("3. Diagnostico de red");
        System.out.print("Opcion: ");
        String opcion = in.nextLine().trim();
        
        if (opcion.equals("1")) {
            crearPartida();
        } else if (opcion.equals("2")) {
            buscarYConectar(in);
        } else if (opcion.equals("3")) {
            diagnosticoRed();
        }
    }
    
    static void diagnosticoRed() {
        System.out.println("\n=== DIAGNOSTICO DE RED ===");
        try {
            NetworkDiscovery discovery = new NetworkDiscovery();
            System.out.println("[INFO] IP local detectada: " + discovery.miIP);
            
            // Probar puertos
            System.out.println("\n[TEST] Probando puertos...");
            if (probarPuerto(PUERTO_BROADCAST)) {
                System.out.println("[OK] Puerto " + PUERTO_BROADCAST + " (broadcast) disponible");
            } else {
                System.out.println("[ERROR] Puerto " + PUERTO_BROADCAST + " ocupado o bloqueado");
            }
            
            if (probarPuerto(PUERTO_JUEGO)) {
                System.out.println("[OK] Puerto " + PUERTO_JUEGO + " (juego) disponible");
            } else {
                System.out.println("[ERROR] Puerto " + PUERTO_JUEGO + " ocupado o bloqueado");
            }
            
            // Probar broadcast local
            System.out.println("\n[TEST] Probando broadcast local...");
            String[] partes = discovery.miIP.split("\\.");
            if (partes.length == 4) {
                String broadcastLocal = partes[0] + "." + partes[1] + "." + partes[2] + ".255";
                System.out.println("[INFO] Direccion broadcast calculada: " + broadcastLocal);
                
                try {
                    InetAddress.getByName(broadcastLocal);
                    System.out.println("[OK] Direccion broadcast es valida");
                } catch (Exception e) {
                    System.out.println("[ERROR] Direccion broadcast invalida: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.out.println("[ERROR] Error en diagnostico: " + e.getMessage());
        }
        
        System.out.println("\n[INFO] Presiona Enter para continuar...");
        new Scanner(System.in).nextLine();
    }
    
    static boolean probarPuerto(int puerto) {
        try {
            DatagramSocket socket = new DatagramSocket(puerto);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    static void crearPartida() {
        NetworkDiscovery discovery = null;
        try {
            // Iniciar descubrimiento de red SOLO cuando creamos partida
            discovery = new NetworkDiscovery();
            discovery.start();
            
            System.out.println("\n=== CREANDO PARTIDA ===");
            System.out.println("Esperando jugador...");
            System.out.println("Presiona Ctrl+C para cancelar");
            ServerSocket servidor = new ServerSocket(PUERTO_JUEGO);
            servidor.setSoTimeout(TIMEOUT_SERVIDOR);
            
            Socket cliente = servidor.accept();
            System.out.println("\n¡JUGADOR CONECTADO desde " + cliente.getInetAddress().getHostAddress() + "!");
            System.out.println("¡INICIANDO JUEGO!");
            
            // Detener descubrimiento silenciosamente una vez conectado
            if (discovery != null) {
                discovery.running = false;
                discovery.interrupt();
            }
            
            jugarMultiplayer(servidor, cliente, true);
        } catch (Exception e) {
            System.out.println("Error al crear partida: " + e.getMessage());
        } finally {
            // Detener descubrimiento silenciosamente
            if (discovery != null) {
                discovery.running = false;
                discovery.interrupt();
            }
        }
    }
    
    static void buscarYConectar(Scanner in) {
        while (true) {
            System.out.println("\n=== BUSCAR PARTIDAS ===");
            System.out.println("1. Busqueda automatica");
            System.out.println("2. Conexion manual por IP");
            System.out.println("3. Volver al menu");
            System.out.print("Selecciona una opcion: ");
            String opcion = in.nextLine().trim();
            
            if (opcion.equals("1")) {
                // Búsqueda automática
                System.out.println("\nBuscando partidas automaticamente...");
                Set<String> jugadores = buscarJugadores();
                
                if (jugadores.isEmpty()) {
                    System.out.println("No se encontraron partidas automaticamente.");
                    System.out.print("Presiona Enter para volver al menu de busqueda...");
                    in.nextLine();
                } else {
                    System.out.println("\nPartidas encontradas:");
                    List<String> listaJugadores = new ArrayList<>(jugadores);
                    for (int i = 0; i < listaJugadores.size(); i++) {
                        System.out.println((i + 1) + ". " + listaJugadores.get(i));
                    }
                    System.out.println("\n0. Volver al menu de busqueda");
                    System.out.print("Selecciona una partida: ");
                    String seleccion = in.nextLine().trim();
                    
                    if (seleccion.equals("0")) {
                        continue;
                    } else {
                        try {
                            int index = Integer.parseInt(seleccion) - 1;
                            if (index >= 0 && index < listaJugadores.size()) {
                                String ip = listaJugadores.get(index);
                                conectarAPartida(ip);
                                return;
                            } else {
                                System.out.println("Opcion no valida.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Opcion no valida.");
                        }
                    }
                }
            } else if (opcion.equals("2")) {
                // Conexión manual
                System.out.print("Introduce la IP del otro jugador: ");
                String ipManual = in.nextLine().trim();
                if (!ipManual.isEmpty()) {
                    conectarAPartida(ipManual);
                    return;
                } else {
                    System.out.println("IP no valida.");
                }
            } else if (opcion.equals("3")) {
                // Salir
                return;
            } else {
                System.out.println("Opcion no valida.");
            }
        }
    }
    
    static void conectarAPartida(String ip) {
        try {
            System.out.println("[CONNECT] Probando conectividad a " + ip + "...");
            
            // Primero probar si el puerto está abierto
            if (!probarConectividad(ip, PUERTO_JUEGO)) {
                System.out.println("[ERROR] No se puede conectar a " + ip + ":" + PUERTO_JUEGO);
                System.out.println("         Asegurate de que:");
                System.out.println("         1. El otro jugador haya creado una partida");
                System.out.println("         2. El firewall permita conexiones en el puerto " + PUERTO_JUEGO);
                System.out.println("         3. Ambos esten en la misma red o tengan conectividad directa");
                return;
            }
            
            System.out.println("[CONNECT] Conectando a " + ip + "...");
            Socket socket = new Socket(ip, PUERTO_JUEGO);
            System.out.println("\n¡CONECTADO EXITOSAMENTE!");
            System.out.println("¡INICIANDO JUEGO!");
            
            jugarMultiplayer(null, socket, false);
        } catch (Exception e) {
            System.out.println("[ERROR] Error al conectar: " + e.getMessage());
            System.out.println("        Detalles tecnicos: " + e.getClass().getSimpleName());
        }
    }
    
    static boolean probarConectividad(String ip, int puerto) {
        try {
            Socket testSocket = new Socket();
            testSocket.connect(new InetSocketAddress(ip, puerto), 3000); // 3 segundos timeout
            testSocket.close();
            return true;
        } catch (Exception e) {
            System.out.println("[DEBUG] Test de conectividad fallo: " + e.getMessage());
            return false;
        }
    }
    
    static void jugarMultiplayer(ServerSocket servidor, Socket socket, boolean esServidor) {
        BufferedReader in = null;
        PrintWriter out = null;
        Scanner scanner = null;
        
        try {
            // Configurar streams con timeouts y buffering
            socket.setSoTimeout(30000); // 30 segundos timeout
            socket.setKeepAlive(true);
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("     BLACKJACK MULTIJUGADOR - JUEGO INICIADO");
            System.out.println("=".repeat(50));
            if (esServidor) {
                System.out.println("ROL: SERVIDOR (tu eres el host)");
            } else {
                System.out.println("ROL: CLIENTE (te conectaste a una partida)");
            }
            System.out.println("=".repeat(50));
            
            boolean continuarJugando = true;
            
            while (continuarJugando) {
                try {
                    if (esServidor) {
                        System.out.println("\n¡NUEVA RONDA! Preparando cartas...");
                        // El servidor maneja la baraja
                        Deck deck = new Deck();
                        deck.shuffle();
                        
                        // Crear manos
                        Hand manoServidor = new Hand();
                        Hand manoCliente = new Hand();
                        
                        // Repartir cartas
                        manoServidor.add(deck.deal());
                        manoCliente.add(deck.deal());
                        manoServidor.add(deck.deal());
                        manoCliente.add(deck.deal());
                        
                        System.out.println("\n=== CARTAS REPARTIDAS ===");
                        System.out.println("Tu mano: " + manoServidor);
                        System.out.println("Enviando cartas al oponente...");
                        
                        // Enviar cartas al cliente
                        String cartasSerializadas = serializarMano(manoCliente);
                        out.println("CARTAS:" + cartasSerializadas);
                        out.flush(); // Asegurar que se envíe
                        
                        if (out.checkError()) {
                            System.out.println("[ERROR] No se pudieron enviar las cartas al cliente");
                            continuarJugando = false;
                            break;
                        }
                        
                        System.out.println("Cartas enviadas. Esperando jugada del oponente...");
                        
                        // Turno del cliente - manejar HIT/STAND
                        boolean clienteBust = false;
                        int totalCliente = 0;
                    
                    while (true) {
                        System.out.println("Esperando respuesta del cliente...");
                        String mensajeCliente = null;
                        
                        try {
                            mensajeCliente = in.readLine();
                        } catch (SocketTimeoutException e) {
                            System.out.println("Timeout esperando respuesta del cliente. Reintentando...");
                            continue;
                        }
                        
                        if (mensajeCliente == null) {
                            System.out.println("\nEl oponente se desconectó.");
                            continuarJugando = false;
                            break;
                        }
                        
                        if (mensajeCliente.equals("QUIT")) {
                            System.out.println("\nEl oponente abandonó la partida.");
                            continuarJugando = false;
                            break;
                        }
                        
                        System.out.println("Recibido del cliente: " + mensajeCliente);
                        
                        if (mensajeCliente.equals("HIT")) {
                            // Cliente pide carta
                            Card nuevaCarta = deck.deal();
                            manoCliente.add(nuevaCarta);
                            String respuesta = "CARTA:" + nuevaCarta.serialize();
                            out.println(respuesta);
                            out.flush();
                            
                            if (out.checkError()) {
                                System.out.println("Error enviando carta al cliente");
                                continuarJugando = false;
                                break;
                            }
                            
                            System.out.println("Carta enviada: " + nuevaCarta);
                            
                            // Verificar si se pasó
                            if (manoCliente.isBust()) {
                                clienteBust = true;
                                totalCliente = manoCliente.bestBlackjackTotal();
                                break;
                            }
                        } else if (mensajeCliente.equals("STAND")) {
                            // Cliente se planta
                            totalCliente = manoCliente.bestBlackjackTotal();
                            break;
                        }
                    }
                    
                    // Si se perdió la conexión, salir del juego
                    if (!continuarJugando) {
                        break;
                    }
                    
                    // Turno del servidor
                    System.out.println("\nTu turno:");
                    System.out.println("Tu mano: " + manoServidor);
                    
                    boolean servidorBust = false;
                    while (true) {
                        if (manoServidor.isBust()) {
                            System.out.println("Te pasaste de " + BLACKJACK_TARGET + ".");
                            servidorBust = true;
                            break;
                        }
                        System.out.print("¿HIT (h) o STAND (s) o QUIT (q)? ");
                        String ans = scanner.nextLine().trim().toLowerCase();
                        
                        if (ans.equals("q")) {
                            out.println("QUIT");
                            System.out.println("Abandonaste la partida.");
                            continuarJugando = false;
                            break;
                        } else if (ans.startsWith("h")) {
                            manoServidor.add(deck.deal());
                            System.out.println("Tu mano: " + manoServidor);
                        } else if (ans.startsWith("s")) {
                            break;
                        }
                    }
                    
                    // Esperar y leer el resultado del cliente
                    String mensajeTotalCliente = in.readLine();
                    if (mensajeTotalCliente == null || mensajeTotalCliente.equals("QUIT")) {
                        System.out.println("\nEl oponente abandonó.");
                        continuarJugando = false;
                        break;
                    }
                    
                    // Enviar resultado
                    int totalServidor = manoServidor.bestBlackjackTotal();
                    if (servidorBust) {
                        out.println("TOTAL:BUST:" + totalServidor);
                    } else {
                        out.println("TOTAL:" + totalServidor);
                    }
                    
                    // Determinar ganador
                    System.out.println("\n=== Resultado ===");
                    System.out.println("Tu total: " + totalServidor);
                    System.out.println("Oponente: " + totalCliente);
                    
                    if (servidorBust && clienteBust) {
                        System.out.println("Ambos se pasaron. Empate.");
                    } else if (servidorBust) {
                        System.out.println("Pierdes (te pasaste).");
                    } else if (clienteBust) {
                        System.out.println("¡Ganas! (oponente se pasó)");
                    } else if (totalServidor > totalCliente) {
                        System.out.println("¡Ganas!");
                    } else if (totalServidor < totalCliente) {
                        System.out.println("Pierdes.");
                    } else {
                        System.out.println("Empate.");
                    }
                    
                } else {
                    // Cliente
                    System.out.println("\n¡NUEVA RONDA! Recibiendo cartas...");
                    
                    // Recibir cartas
                    String cartasMsg = in.readLine();
                    if (cartasMsg == null || cartasMsg.equals("QUIT")) {
                        System.out.println("\nEl oponente abandonó la partida.");
                        continuarJugando = false;
                        break;
                    }
                    
                    Hand manoCliente = deserializarMano(cartasMsg.split(":")[1]);
                    
                    System.out.println("\n=== CARTAS RECIBIDAS ===");
                    System.out.println("Tu mano: " + manoCliente);
                    System.out.println("¡ES TU TURNO!");
                    
                    // Turno del cliente
                    boolean clienteBust = false;
                    while (true) {
                        if (manoCliente.isBust()) {
                            System.out.println("Te pasaste de " + BLACKJACK_TARGET + ".");
                            clienteBust = true;
                            break;
                        }
                        System.out.print("¿HIT (h) o STAND (s) o QUIT (q)? ");
                        String ans = scanner.nextLine().trim().toLowerCase();
                        
                        if (ans.equals("q")) {
                            out.println("QUIT");
                            System.out.println("Abandonaste la partida.");
                            continuarJugando = false;
                            break;
                        } else if (ans.startsWith("h")) {
                            // Solicitar carta al servidor
                            out.println("HIT");
                            String respuesta = in.readLine();
                            if (respuesta == null || respuesta.equals("QUIT")) {
                                System.out.println("\nEl oponente abandonó.");
                                continuarJugando = false;
                                break;
                            }
                            if (respuesta.startsWith("CARTA:")) {
                                Card c = Card.deserialize(respuesta.split(":", 2)[1]);
                                manoCliente.add(c);
                                System.out.println("Tu mano: " + manoCliente);
                            }
                        } else if (ans.startsWith("s")) {
                            out.println("STAND");
                            break;
                        }
                    }
                    
                    // Si se perdió la conexión durante el turno, salir del juego
                    if (!continuarJugando) {
                        break;
                    }
                    
                    // Enviar resultado
                    int totalCliente = manoCliente.bestBlackjackTotal();
                    if (clienteBust) {
                        out.println("TOTAL:BUST:" + totalCliente);
                    } else {
                        out.println("TOTAL:" + totalCliente);
                    }
                    
                    System.out.println("\nEsperando al oponente...");
                    
                    // Recibir resultado del servidor
                    String resultadoServidor = in.readLine();
                    if (resultadoServidor == null || resultadoServidor.equals("QUIT")) {
                        System.out.println("\nEl oponente abandonó.");
                        continuarJugando = false;
                        break;
                    }
                    
                    boolean servidorBust = resultadoServidor.contains("BUST");
                    String[] partes = resultadoServidor.split(":");
                    int totalServidor = Integer.parseInt(partes[partes.length - 1]);
                    
                    // Determinar ganador
                    System.out.println("\n=== Resultado ===");
                    System.out.println("Tu total: " + totalCliente);
                    System.out.println("Oponente: " + totalServidor);
                    
                    if (servidorBust && clienteBust) {
                        System.out.println("Ambos se pasaron. Empate.");
                    } else if (clienteBust) {
                        System.out.println("Pierdes (te pasaste).");
                    } else if (servidorBust) {
                        System.out.println("¡Ganas! (oponente se pasó)");
                    } else if (totalCliente > totalServidor) {
                        System.out.println("¡Ganas!");
                    } else if (totalCliente < totalServidor) {
                        System.out.println("Pierdes.");
                    } else {
                        System.out.println("Empate.");
                    }
                }
                
                // Ambos preguntan independientemente
                System.out.print("\n¿Jugar otra vez? (s/n): ");
                String respuesta = scanner.nextLine().trim().toLowerCase();
                boolean yoQuiero = respuesta.startsWith("s");
                
                if (yoQuiero) {
                    // Yo quiero jugar - envío mi decisión y espero al oponente
                    out.println("QUIERO_JUGAR");
                    System.out.println("Esperando al oponente...");
                    
                    String respuestaOponente = in.readLine();
                    
                    if (respuestaOponente == null) {
                        System.out.println("Error: Conexión perdida.");
                        continuarJugando = false;
                    } else if (respuestaOponente.equals("NO_QUIERO_JUGAR")) {
                        System.out.println("El oponente no quiere jugar otra vez.");
                        continuarJugando = false;
                    } else if (respuestaOponente.equals("QUIERO_JUGAR")) {
                        System.out.println("¡Ambos quieren jugar! Nueva partida...");
                        continuarJugando = true;
                    } else {
                        System.out.println("Error: Mensaje inesperado del oponente: " + respuestaOponente);
                        continuarJugando = false;
                    }
                } else {
                    // Yo NO quiero jugar - envío mi decisión y salgo inmediatamente
                    out.println("NO_QUIERO_JUGAR");
                    System.out.println("Has decidido no jugar más.");
                    continuarJugando = false;
                    // NO esperar respuesta del oponente - salir inmediatamente
                }
                
                } catch (Exception e) {
                    System.out.println("Error en la comunicacion: " + e.getMessage());
                    continuarJugando = false;
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error durante el juego: " + e.getMessage());
            System.out.println("Tipo de error: " + e.getClass().getSimpleName());
            if (e.getMessage().contains("aborted") || e.getMessage().contains("reset")) {
                System.out.println("La conexion se perdio. El otro jugador puede haberse desconectado.");
            }
        } finally {
            // Cerrar recursos
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (scanner != null) scanner.close();
                if (socket != null && !socket.isClosed()) socket.close();
                if (servidor != null && !servidor.isClosed()) servidor.close();
            } catch (Exception e) {
                System.out.println("Error cerrando conexiones: " + e.getMessage());
            }
        }
    }
    
    static String serializarMano(Hand mano) {
        StringBuilder sb = new StringBuilder();
        for (Card c : mano.getCards()) {
            sb.append(c.serialize()).append(";");
        }
        return sb.toString();
    }
    
    static Hand deserializarMano(String data) {
        Hand mano = new Hand();
        String[] cartas = data.split(";");
        for (String carta : cartas) {
            if (!carta.isEmpty()) {
                mano.add(Card.deserialize(carta));
            }
        }
        return mano;
    }

    /** ----- Menú Principal ----- */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n╔════════════════════════════════╗");
            System.out.println("║       BLACKJACK GAME           ║");
            System.out.println("╚════════════════════════════════╝");
            System.out.println("1. Singleplayer (vs Casa)");
            System.out.println("2. Multiplayer (vs Jugador)");
            System.out.println("3. Salir");
            System.out.print("\nSelecciona una opción: ");
            
            String opcion = in.nextLine().trim();
            
            switch (opcion) {
                case "1":
                    jugarSingleplayer();
                    break;
                case "2":
                    modoMultiplayer();
                    break;
                case "3":
                    System.out.println("¡Hasta luego!");
                    in.close();
                    return;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }
}