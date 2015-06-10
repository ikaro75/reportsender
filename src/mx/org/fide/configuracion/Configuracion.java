/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.org.fide.configuracion;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import mx.org.fide.archivo.Archivo; 
import mx.org.fide.modelo.Fallo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Daniel
 */
public class Configuracion {
    private LinkedHashMap parametros = new LinkedHashMap();
    private String configuracionActual;
    
    public Configuracion(String archivo) throws Fallo{
        try {
            /*ClassLoader classLoader = Thread.currentThread().getContextClassgetClass().getClassLoader().getResource(".").getPath()Loader();
            URL url = classLoader.getResource("com/administrax/configuracion"); 
            * String path = ;
            * */
            File xml = new File(archivo);

            //Eliminar esta linea cuando entre a producción
            //xml = new File("configuracion.xml");
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();
            NodeList nlConfiguracionEmail = doc.getElementsByTagName("configuracion_email");
            
            if (nlConfiguracionEmail.getLength()==0) {
                    throw new Fallo("Faltan nodos en la etiqueta <configuracion_email>");
            }
            
            Node nConfiguracionEmail = nlConfiguracionEmail.item(0);
            if (nConfiguracionEmail.getNodeType() != Node.ELEMENT_NODE) {
                    throw new Fallo("Tipo de nodo no válido <reporte>");
            }
            
            Element eConfiguracionEmail = (Element) nConfiguracionEmail;
            parametros.put("iniciotls_habilitado", getValue("iniciotls_habilitado", eConfiguracionEmail));
            parametros.put("servidor_smtp", getValue("servidor_smtp", eConfiguracionEmail));
            parametros.put("puerto_smtp", getValue("puerto_smtp", eConfiguracionEmail));
            parametros.put("usuario_smtp", getValue("usuario_smtp", eConfiguracionEmail));
            parametros.put("password_smtp", getValue("password_smtp", eConfiguracionEmail));

            NodeList nlReportes = doc.getElementsByTagName("reporte");
            
            for (int i = 0; i < nlReportes.getLength(); i++) {
                LinkedHashMap lhReporte = new LinkedHashMap();
                Node nReporte = nlReportes.item(i);

                if (nReporte.getNodeType() != Node.ELEMENT_NODE) {
                    throw new Fallo("Tipo de nodo no válido <reporte>");
                }
                
                Element eReporte = (Element) nReporte;
                String nombreReporte= getValue("nombre", eReporte);
                
                NodeList nodeDb = eReporte.getElementsByTagName("db");

                if (nodeDb.getLength() == 0) {
                    throw new Fallo("Falta nodo db, verifique");
                }
                
                Element db = (Element) nodeDb.item(0);
                
                lhReporte.put("db_server", getValue("servidor", db));
                lhReporte.put("db_name", getValue("nombre", db));
                lhReporte.put("db_user", getValue("usuario", db));
                lhReporte.put("db_pw", getValue("password", db));
                lhReporte.put("db_type", getValue("tipo", db));                

                lhReporte.put("consulta_para_generar_reportes", getValue("consulta_para_generar_reportes", eReporte));
                lhReporte.put("plantilla_reporte", getValue("plantilla_reporte", eReporte));
                lhReporte.put("nombre_archivo", getValue("nombre_archivo", eReporte));
                lhReporte.put("asunto_email", getValue("asunto_email", eReporte));
                lhReporte.put("mensaje_email", getValue("mensaje_email", eReporte));
                
                NodeList nlParametros = eReporte.getElementsByTagName("parametros");        
                LinkedHashMap lhParametros = new LinkedHashMap();
                for (int j = 0; j < nlParametros.getLength(); j++) {
                    Node nParametro = nlParametros.item(j);
                    Element eParametro = (Element) nParametro;
                    lhParametros.put(getValue("parametro", eParametro),getValue("tipo_dato", eParametro).concat("|").concat(getValue("valor", eParametro)));
                }
               
               lhReporte.put("parametros", lhParametros);
               
               NodeList nlEnviarA = eReporte.getElementsByTagName("enviar_a");
               
               if (nlEnviarA.getLength() == 0) {
                    throw new Fallo("Falta nodo enviar_a, verifique");
               }
               
               ArrayList<String> alEmails = new ArrayList<String>();
               
               for (int j = 0; j < nlEnviarA.getLength(); j++) {
                    Node nEnviarA = nlEnviarA.item(j);
                    Element eEnviarA = (Element) nEnviarA;
                    alEmails.add(getValue("email", eEnviarA));
               }
               
               lhReporte.put("enviar_a", alEmails); 
                //Se agrega al final a la tabla de parametros
               parametros.put(nombreReporte, lhReporte);
            }
        } catch (FileNotFoundException  fe) {
            throw new Fallo(fe.getMessage());
        } catch (Exception ex) {
            throw new Fallo(ex.getMessage());
        } 
    }
    
    private static String getValue(String tag, Element element) throws Fallo {
        if (element.getElementsByTagName(tag).item(0)==null) {
            throw new Fallo("No se encontró la etiqueta ".concat(tag.toUpperCase()).concat(" en el documento XML"));
        }
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        
        if (node==null)
            return null;
        else            
            return node.getNodeValue();
    }
    
    /*public Configuracion() throws Fallo{
        Archivo archivo = new Archivo();
        try {
            archivo.lee("/com/administrax/archivo/configuracion.properties");
            parametros.put("db_server", archivo.getPropiedades().getProperty("db_server"));
            parametros.put("db_name", archivo.getPropiedades().getProperty("db_name"));
            parametros.put("db_user", archivo.getPropiedades().getProperty("db_user"));
            parametros.put("db_pw", archivo.getPropiedades().getProperty("db_pw"));
            parametros.put("db_type", archivo.getPropiedades().getProperty("db_type"));
            parametros.put("enterprise_logo", archivo.getPropiedades().getProperty("logo"));
            parametros.put("enterprise_name", archivo.getPropiedades().getProperty("logo"));
            parametros.put("smtp_user", archivo.getPropiedades().getProperty("smtp_user"));
            parametros.put("starttls_enabled", archivo.getPropiedades().getProperty("starttls_enabled"));
            parametros.put("smtp_host", archivo.getPropiedades().getProperty("smtp_host"));
            parametros.put("smtp_auth", archivo.getPropiedades().getProperty("smtp_auth"));
            parametros.put("smtp_pw", archivo.getPropiedades().getProperty("smtp_pw"));
            
        } catch(Exception e) {
            System.out.println(e.getStackTrace());
            throw new Fallo("Error al leer el archivo de configuracion:".concat(e.getMessage()) );
        }    
            
        
    }*/

    public LinkedHashMap getParametros() {
        return parametros;
    }

    public void setParametros(LinkedHashMap parametros) {
        this.parametros = parametros;
    }

    public String getConfiguracionActual() {
        return configuracionActual;
    }

    public void setConfiguracionActual(String configuracionActual) {
        this.configuracionActual = configuracionActual;
    }
    
}
