public class Servidor2 {
    public static void main(String[] args){
        Servidor server2 = new Servidor(6000,"localhost",5000,2);
        new Thread(server2).start();
    }//main
}
