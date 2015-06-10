/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.org.fide.modelo;

/**
 *
 * @author Daniel
 */
public class Fallo extends Exception {
    Fallo(){}
    
    public Fallo(String mensaje){
        super(mensaje);
    }
}
