import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

class Manejador implements Runnable{
    protected Socket cl = null;
    protected int numeroCliente;
    public Manejador(Socket cl) {
        numeroCliente = 0;
        this.cl = cl;
    }

    @Override
    public void run() {}
}

public class Servidor implements Runnable{
    protected int          puerto   = 5000;
    protected ServerSocket s = null;
    protected boolean      detenido    = false;
    protected Thread       runningThread= null;
    protected ExecutorService pool = Executors.newFixedThreadPool(100);
    protected Ventana1 v1;
    protected int numCliente;
    protected Hashtable informarVacio,informarSincronizacion,Di,Ti,informarReinicio;
    public ReentrantLock rl,rl2;
    PrintWriter pwgeneral;
    BufferedReader brgeneral;
    public ArrayList<String> peticiones;
    public int tiempo=6000;
    public boolean reiniciar;
    
    public Servidor(int puerto, String servidor1, int puerto1, int i){
        numCliente = 0;
        v1 = new Ventana1(i);
        v1.setTitle("Práctica 3 - Repartir Libros, Servidor");
        v1.setResizable(false);
        this.puerto = puerto;
        informarVacio = new Hashtable();
        informarSincronizacion = new Hashtable();
        informarReinicio = new Hashtable();
        Di = new Hashtable();
        Ti = new Hashtable();
        peticiones = new ArrayList<String>();
        rl = new ReentrantLock();
        rl2 = new ReentrantLock();
        esperar(servidor1,puerto1);
        v1.setVisible(true);
        v1.refrescarLibros();
        v1.reiniciar.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                rl.lock();
                Enumeration llaves = informarReinicio.keys();
                while (llaves.hasMoreElements()) {
                    int aux = (int) llaves.nextElement();
                    informarReinicio.remove(aux);
                    informarReinicio.put(aux,"1");
                    System.out.println(aux+", Informando \"reinicio\"");
                }
                rl.unlock();
            }
        });
        //sincronizar();
    }
    
    public void esperar(String servidor1, int puerto1){
        try {
            Socket cl = new Socket(servidor1,puerto1);
            System.out.println("Conexion establecida..");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(cl.getOutputStream()));
            BufferedReader br = new BufferedReader(new InputStreamReader(cl.getInputStream()));
            pw.println("-1");
            pw.flush();
            int numeroCliente = Integer.valueOf(br.readLine());
            System.out.println("Numero de cliente: "+numeroCliente);
            pw.println("servidor");
            pw.flush();
            String msj,hora,ip,nomCl,nomLib;
            int i,aux;
            while(true){
                msj = br.readLine();
                if(msj.substring(0, 3).equals("reg")){
                    peticiones.add(msj);
                    i=4;
                    while(msj.charAt(i)!=':'){
                        i++;
                    }
                    nomCl = msj.substring(4,i);
                    hora = msj.substring(i+1);
                    System.out.println(msj);
                    System.out.println(nomCl);
                    System.out.println(hora);
                    v1.cbd.regresarLibros(nomCl,hora);
                    v1.refrescarLibros();
                }else if(msj.equals("sincronizar")){
                    String horaServ = br.readLine();
                    System.out.println(horaServ);
                    //Ahora iniciamos un proceso para calcular la diferencia entre la hora del servidor y del cliente
                    pw.println("sincronizar");
                    pw.flush();
                    pw.println(v1.r1.getHora2());
                    pw.flush();
                }else if(msj.equals("sincronizardos")){
                    String ti = br.readLine();
                    //Actualizar hora
                    System.out.println(ti);
                }else{
                    peticiones.add(msj);
                    hora = msj.substring(0,8);
                    i=9;
                    while(msj.charAt(i)!=':'){
                        i++;
                    }
                    ip = msj.substring(9,i);
                    i++;
                    aux = i;
                    while(msj.charAt(i)!=':'){
                        i++;
                    }
                    aux = Integer.valueOf(msj.substring(aux,i));
                    if(aux>numCliente)
                        numCliente = aux;
                    i++;
                    aux = i;
                    while(msj.charAt(i)!=':'){
                        i++;
                    }
                    nomCl = msj.substring(aux,i);
                    while(msj.charAt(i)!=':'){
                        i++;
                    }
                    i+=7;
                    aux = i;
                    while(msj.charAt(i)!=','){
                        i++;
                    }
                    nomLib = msj.substring(aux,i);
                    System.out.println(msj);
                    System.out.println(hora);
                    System.out.println(ip);
                    System.out.println(nomCl);
                    System.out.println(nomLib);
                    System.out.println(numCliente);
                    v1.cbd.guardaRegistro(hora,ip,nomCl,nomLib);
                }
            }//while
        } catch (IOException ex) {}
    }
    public void sincronizar(){
        Thread sinc = new Thread(){
            @Override
            public void run(){
                try {
                    while(true){
                        boolean b=false;
                        Thread.sleep(tiempo);
                        rl.lock();
                        Enumeration llaves = informarSincronizacion.keys();
                        while (llaves.hasMoreElements()) {
                            int aux = (int) llaves.nextElement();
                            informarSincronizacion.remove(aux);
                            informarSincronizacion.put(aux,"1");
                            System.out.println(aux+", Enviado \"sincronizar\"");
                        }
                        rl.unlock();
                        while(!b){
                            b = true;
                            rl.lock();
                            llaves = informarSincronizacion.keys();
                            while (llaves.hasMoreElements()) {
                                int aux = (int) llaves.nextElement();
                                //System.out.println("Aqui while aux="+aux+", val="+informarSincronizacion.get(aux));
                                if(!informarSincronizacion.get(aux).equals("0"))
                                    b = false;
                            }
                            rl.unlock();
                        }
                        //Iniciar proceso para calcular el Ti de cada cliente
                        //
                        rl.lock();
                        llaves = informarSincronizacion.keys();
                        while (llaves.hasMoreElements()) {
                            int aux = (int) llaves.nextElement();
                            informarSincronizacion.remove(aux);
                            informarSincronizacion.put(aux,"2");
                            Ti.remove(aux);
                            Ti.put(aux,String.valueOf(aux));
                            System.out.println("Ti="+aux);
                        }
                        rl.unlock();
                        //Esperar cierta cantidad de tiempo
                    }
                } catch (InterruptedException ex) {}
            }
        };
        sinc.start();
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        iniciaServidor();
        while(! detenido()){
            Socket cl = null;
            try {
                cl = this.s.accept();
                System.out.println("\nConexion aceptada..");
            } catch (IOException e) {
                if(detenido()) {
                    System.out.println("Servidor detenido.") ;
                    break;
                }throw new RuntimeException("Error al aceptar nueva conexion", e);
            }//catch
            this.pool.execute(new Manejador(cl){
                PrintWriter pw;
                BufferedReader br;
                Boolean salir;
                @Override
                public void run() {
                    try {
                        pw = new PrintWriter(new OutputStreamWriter(cl.getOutputStream()));
                        br = new BufferedReader(new InputStreamReader(cl.getInputStream()));
                        salir = false;
                        numeroCliente = Integer.valueOf(br.readLine());
                        //System.out.println(numeroCliente);
                        if(numeroCliente==-1){
                            numeroCliente = numCliente;
                            numCliente++;
                        }
                        informarVacio.put(numeroCliente,"0");
                        informarSincronizacion.put(numeroCliente,"0");
                        informarReinicio.put(numeroCliente,"0");
                        Di.put(numeroCliente,"0");
                        Ti.put(numeroCliente,"0");
                        pw.println(String.valueOf(numeroCliente));
                        pw.flush();
                        System.out.println("Cliente conectado: "+String.valueOf(cl.getInetAddress()).substring(1)+":"+cl.getPort()+", id asignada: "+String.valueOf(numeroCliente));                  
                        Thread enviar = new Thread(){
                            @Override
                            public void run(){
                                while(true){
                                    rl.lock();
                                    if(informarVacio.get(numeroCliente).equals("1")){
                                        informarVacio.remove(numeroCliente);
                                        informarVacio.put(numeroCliente,"0");
                                        System.out.println("Todos los libros han sido prestados");
                                        pw.println("vacia");
                                        pw.flush();
                                    }
                                    rl.unlock();
                                    rl.lock();
                                    if(informarReinicio.get(numeroCliente).equals("1")){
                                        informarReinicio.remove(numeroCliente);
                                        informarReinicio.put(numeroCliente,"0");
                                        System.out.println("Reiniciando cliente "+numeroCliente);
                                        pw.println("reiniciar");
                                        pw.flush();
                                    }
                                    rl.unlock();
                                    /*rl.lock();
                                    if(informarSincronizacion.get(numeroCliente).equals("1")){
                                        informarSincronizacion.remove(numeroCliente);
                                        informarSincronizacion.put(numeroCliente,"3");
                                        pw.println("sincronizar");
                                        pw.flush();
                                        pw.println(v1.r1.getHora2());
                                        pw.flush();
                                    }
                                    rl.unlock();
                                    rl.lock();
                                    if(informarSincronizacion.get(numeroCliente).equals("2")){
                                        informarSincronizacion.remove(numeroCliente);
                                        informarSincronizacion.put(numeroCliente,"0");
                                        System.out.println("Enviando sincronizar 2.1");
                                        pw.println("sincronizardos");
                                        pw.flush();
                                        System.out.println("Enviando sincronizar 2.2");
                                        pw.println("nada");
                                        pw.flush();
                                        System.out.println("Enviando sincronizar 2.3");
                                    }
                                    rl.unlock();*/
                                }
                            }
                        };
                        enviar.start();
                        while(!salir){
                            String linea = br.readLine();
                            if(linea.compareToIgnoreCase("vacio")==0){
                                if(v1.cbd.esLibrosVacio()){
                                    //Informar al cliente que el prestamo de libros ha terminado
                                    pw.println("vacia");
                                    pw.flush();
                                    pw.println("vacia");
                                }else{
                                    pw.println("no");
                                    pw.flush();
                                    pw.println("no");
                                }
                                pw.flush();
                            }else if(linea.compareToIgnoreCase("libro")==0){
                                //auxiliar es la hora
                                String auxiliar = br.readLine();
                                //auxiliar2 es la IP
                                String auxiliar2 = String.valueOf(cl.getInetAddress()).substring(1);
                                String[] resp = v1.cbd.pedirLibro(auxiliar2,auxiliar,"Cliente"+String.valueOf(numeroCliente));
                                System.out.println("\n"+resp[0]);
                                System.out.println(resp[1]);
                                v1.panel.dibujar(resp[1]);
                                System.out.println("Envio la informacion del cliente");
                                pw.println(resp[0]);
                                pw.flush();
                                rl2.lock();
                                peticiones.add(auxiliar+":"+auxiliar2+":"+numCliente+":"+"Cliente"+String.valueOf(numeroCliente)+":"+resp[0]);
                                rl2.unlock();
                                v1.refrescarLibros();
                                if(v1.cbd.esLibrosVacio()){
                                    System.out.println("\nLa base de datos está vacia");
                                    //Informar al cliente que el prestamo de libros ha terminado
                                    rl.lock();
                                    Enumeration llaves = informarVacio.keys();
                                    while (llaves.hasMoreElements()) {
                                        int aux = (int) llaves.nextElement();
                                        informarVacio.remove(aux);
                                        informarVacio.put(aux,"1");
                                    }
                                    rl.unlock();
                                }
                            }else if(linea.compareToIgnoreCase("reiniciar")==0){
                                //Agregar la peticion al Servidor2
                                String hora = br.readLine();
                                v1.cbd.regresarLibros("Cliente"+String.valueOf(numeroCliente),hora);
                                rl2.lock();
                                peticiones.add("reg:Cliente"+numeroCliente+":"+hora);
                                rl2.unlock();
                                v1.refrescarLibros();
                            }else if(linea.compareToIgnoreCase("sincronizar")==0){
                                String horaCliente = br.readLine();
                                System.out.println(String.valueOf(numeroCliente)+", Di="+horaCliente);
                                Di.put(numeroCliente,horaCliente);
                                rl.lock();
                                informarSincronizacion.remove(numeroCliente);
                                informarSincronizacion.put(numeroCliente,"0");
                                rl.unlock();
                            }else if(linea.compareToIgnoreCase("servidor")==0){
                                System.out.println("Servidor 2 Iniciado");
                                enviar.stop();
                                rl.lock();
                                informarVacio.remove(numeroCliente);
                                informarReinicio.remove(numeroCliente);
                                rl.unlock();
                                enviar = new Thread(){
                                    public void run(){
                                        while(true){
                                            try {
                                                pwgeneral = new PrintWriter("auxiliar1.txt");
                                                pwgeneral.close();
                                                if(peticiones.size()>0){
                                                    rl2.lock();
                                                    for(int i=0;i<peticiones.size();i++){
                                                        pw.println(peticiones.get(i));
                                                        pw.flush();
                                                    }
                                                    peticiones.clear();
                                                    rl2.unlock();
                                                    new File("auxiliar.txt").delete();
                                                }
                                            } catch (FileNotFoundException ex) {}
                                            rl.lock();
                                            if(informarSincronizacion.get(numeroCliente).equals("1")){
                                                informarSincronizacion.remove(numeroCliente);
                                                informarSincronizacion.put(numeroCliente,"0");
                                                pw.println("sincronizar");
                                                pw.flush();
                                                pw.println(v1.r1.getHora2());
                                                pw.flush();
                                            }
                                            rl.unlock();
                                            rl.lock();
                                            if(informarSincronizacion.get(numeroCliente).equals("2")){
                                                informarSincronizacion.remove(numeroCliente);
                                                informarSincronizacion.put(numeroCliente,"0");
                                                pw.println("sincronizardos");
                                                pw.flush();
                                                pw.println(Ti.get(numeroCliente));
                                                pw.flush();
                                            }
                                            rl.unlock();
                                        }
                                    }
                                };
                                enviar.start();
                            }
                        }//while
                        System.out.println("Solicitud procesada: ");
                    } catch (IOException e) {}
                }
            });
        }//while
        this.pool.shutdown();
        System.out.println("Servidor detenido.") ;
    }


    private synchronized boolean detenido() {
        return this.detenido;
    }

    public synchronized void stop(){
        this.detenido = true;
        try {
            this.s.close();
        } catch (IOException e) {
            throw new RuntimeException("Error al cerrar el socket del servidor", e);
        }
    }

    private void iniciaServidor() {
        try {
            this.s = new ServerSocket(this.puerto);
            System.out.println("Servicio iniciado.. esperando cliente..");
        } catch (IOException e) {
            throw new RuntimeException("No puede iniciar el socket en el puerto: "+s.getLocalPort(), e);
        }
    }
}//class