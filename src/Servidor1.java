public class Servidor1 {
    public static void main(String[] args){
        Servidor server = new Servidor(5000,"localhost",6000,0);
        new Thread(server).start();
    }//main
}
