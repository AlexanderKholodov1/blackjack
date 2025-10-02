import java.util.*;
import java.io.*;
import java.net.*;

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
    private static final int TIMEOUT_BUSQUEDA = 4000;  // 4 segundos (más tiempo para WiFi)
    
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
                System.out.println("[STOP] Servidor de descubrimiento detenido");
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
            
            // Intentar múltiples direcciones de broadcast
            String[] direccionesBroadcast = {
                "255.255.255.255",  // Broadcast global
                "192.168.1.255",    // Broadcast típico WiFi casa
                "192.168.0.255",    // Broadcast alternativo WiFi casa
                "10.0.0.255",       // Broadcast red corporativa
                "172.31.255.255"    // Broadcast rango 172.16-31.x.x
            };
            
            byte[] buffer = MENSAJE_BROADCAST.getBytes();
            
            for (String broadcast : direccionesBroadcast) {
                try {
                    System.out.println("[BROADCAST] Enviando broadcast a: " + broadcast);
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        InetAddress.getByName(broadcast), PUERTO_BROADCAST
                    );
                    socket.send(packet);
                } catch (Exception e) {
                    System.out.println("   [ERROR] Error enviando a " + broadcast + ": " + e.getMessage());
                }
            }
            
            // También intentar broadcast dirigido a nuestra subred
            try {
                String miIP = new NetworkDiscovery().miIP;
                if (miIP != null && !miIP.equals("desconocida")) {
                    String[] partes = miIP.split("\\.");
                    if (partes.length == 4) {
                        String broadcastLocal = partes[0] + "." + partes[1] + "." + partes[2] + ".255";
                        System.out.println("[BROADCAST] Enviando broadcast local a: " + broadcastLocal);
                        DatagramPacket packet = new DatagramPacket(
                            buffer, buffer.length,
                            InetAddress.getByName(broadcastLocal), PUERTO_BROADCAST
                        );
                        socket.send(packet);
                    }
                }
            } catch (Exception e) {
                System.out.println("   [ERROR] Error con broadcast local: " + e.getMessage());
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
        System.out.print("Opción: ");
        String opcion = in.nextLine().trim();
        
        if (opcion.equals("1")) {
            crearPartida();
        } else if (opcion.equals("2")) {
            buscarYConectar(in);
        }
    }
    
    static void crearPartida() {
        NetworkDiscovery discovery = null;
        try {
            // Iniciar descubrimiento de red SOLO cuando creamos partida
            discovery = new NetworkDiscovery();
            discovery.start();
            
            System.out.println("\nEsperando jugador...");
            ServerSocket servidor = new ServerSocket(PUERTO_JUEGO);
            servidor.setSoTimeout(TIMEOUT_SERVIDOR);
            
            Socket cliente = servidor.accept();
            System.out.println("¡Jugador conectado desde " + cliente.getInetAddress().getHostAddress() + "!");
            
            // Detener descubrimiento una vez conectado
            if (discovery != null) discovery.detener();
            
            jugarMultiplayer(servidor, cliente, true);
        } catch (Exception e) {
            System.out.println("Error al crear partida: " + e.getMessage());
        } finally {
            if (discovery != null) discovery.detener();
        }
    }
    
    static void buscarYConectar(Scanner in) {
        while (true) {
            System.out.println("\nBuscando partidas...");
            Set<String> jugadores = buscarJugadores();
            
            if (jugadores.isEmpty()) {
                System.out.println("No se encontraron partidas.");
            } else {
                System.out.println("\nPartidas disponibles:");
                List<String> listaJugadores = new ArrayList<>(jugadores);
                for (int i = 0; i < listaJugadores.size(); i++) {
                    System.out.println((i + 1) + ". " + listaJugadores.get(i));
                }
                System.out.println("\n0. Refrescar");
                System.out.println("q. Volver al menú");
                System.out.print("Selecciona una opción: ");
                String seleccion = in.nextLine().trim();
                
                if (seleccion.equals("q")) {
                    return;
                } else if (seleccion.equals("0")) {
                    continue;
                } else {
                    try {
                        int index = Integer.parseInt(seleccion) - 1;
                        if (index >= 0 && index < listaJugadores.size()) {
                            String ip = listaJugadores.get(index);
                            conectarAPartida(ip);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Opción no válida.");
                    }
                }
            }
            
            System.out.print("\nPresiona Enter para refrescar (o 'q' para salir): ");
            String cmd = in.nextLine().trim();
            if (cmd.equals("q")) return;
        }
    }
    
    static void conectarAPartida(String ip) {
        try {
            System.out.println("Conectando a " + ip + "...");
            Socket socket = new Socket(ip, PUERTO_JUEGO);
            System.out.println("¡Conectado!");
            
            jugarMultiplayer(null, socket, false);
        } catch (Exception e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }
    
    static void jugarMultiplayer(ServerSocket servidor, Socket socket, boolean esServidor) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            
            boolean continuarJugando = true;
            
            while (continuarJugando) {
                if (esServidor) {
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
                    
                    // Enviar cartas al cliente
                    out.println("CARTAS:" + serializarMano(manoCliente));
                    
                    System.out.println("\n=== Nueva Partida ===");
                    System.out.println("Tu mano: " + manoServidor);
                    System.out.println("Esperando al oponente...");
                    
                    // Turno del cliente - manejar HIT/STAND
                    boolean clienteBust = false;
                    int totalCliente = 0;
                    
                    while (true) {
                        String mensajeCliente = in.readLine();
                        if (mensajeCliente == null || mensajeCliente.equals("QUIT")) {
                            System.out.println("\nEl oponente abandonó la partida.");
                            return;
                        }
                        
                        if (mensajeCliente.equals("HIT")) {
                            // Cliente pide carta
                            Card nuevaCarta = deck.deal();
                            manoCliente.add(nuevaCarta);
                            out.println("CARTA:" + nuevaCarta.serialize());
                            
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
                            return;
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
                        return;
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
                    System.out.println("\n=== Nueva Partida ===");
                    
                    // Recibir cartas
                    String cartasMsg = in.readLine();
                    if (cartasMsg == null || cartasMsg.equals("QUIT")) {
                        System.out.println("\nEl oponente abandonó la partida.");
                        break;
                    }
                    
                    Hand manoCliente = deserializarMano(cartasMsg.split(":")[1]);
                    
                    System.out.println("Tu mano: " + manoCliente);
                    
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
                            return;
                        } else if (ans.startsWith("h")) {
                            // Solicitar carta al servidor
                            out.println("HIT");
                            String respuesta = in.readLine();
                            if (respuesta == null || respuesta.equals("QUIT")) {
                                System.out.println("\nEl oponente abandonó.");
                                return;
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
            }
            
            socket.close();
            if (servidor != null) servidor.close();
            
        } catch (Exception e) {
            System.out.println("Error durante el juego: " + e.getMessage());
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