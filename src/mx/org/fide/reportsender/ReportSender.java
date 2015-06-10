package mx.org.fide.reportsender;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mx.org.fide.configuracion.Configuracion;
import mx.org.fide.jasper.ReporteJasper;
import mx.org.fide.mail.Mail;
import mx.org.fide.modelo.Conexion;
import mx.org.fide.modelo.Conexion.DbType;
import mx.org.fide.modelo.Fallo;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperRunManager;

public class ReportSender {

    private Configuracion configuracion = null;
    private ArrayList<ReporteJasper> reportes = new ArrayList<ReporteJasper>();
    private Conexion cx = null;

    public ReportSender(String archivo) throws Fallo {
        try {
            System.out.println("Cargando configuracion...");
            configuracion = new Configuracion(archivo);
        } catch (Fallo ex) {
            System.out.print(ex.getMessage());
            Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
            throw new Fallo(ex.getMessage());
        }
    }

    public void sendReports() {
        OutputStream archivo = null; 
        JRExporter exporter = null;
   
        // 1. Configura mail para envío
        System.out.println("Configurando cuenta de correo...");
        Mail mail = new Mail(this.configuracion.getParametros().get("iniciotls_habilitado").toString(),
                this.configuracion.getParametros().get("usuario_smtp").toString(),
                "true",
                this.configuracion.getParametros().get("password_smtp").toString(),
                ";",
                this.configuracion.getParametros().get("servidor_smtp").toString(),
                this.configuracion.getParametros().get("puerto_smtp").toString());

        //2. Crea un arreglo con las claves para recorrer la configuración de envío de reportes;
        Set<String> parametros = this.configuracion.getParametros().keySet();

        for (String parametro : parametros) {
            //Omite los parámetros de configuración relacionados al correo
            if (parametro.equals("iniciotls_habilitado")
                    || parametro.equals("usuario_smtp")
                    || parametro.equals("password_smtp")
                    || parametro.equals("servidor_smtp")
                    || parametro.equals("puerto_smtp")) {
                continue;
            }

            LinkedHashMap lhReportes = (LinkedHashMap) this.configuracion.getParametros().get(parametro);

            //3. Crea conexión a la base de datos
            Conexion cx = new Conexion(lhReportes.get("db_server").toString(),
                    lhReportes.get("db_name").toString(),
                    lhReportes.get("db_user").toString(),
                    lhReportes.get("db_pw").toString(),
                    DbType.valueOf(lhReportes.get("db_type").toString())
            );

            try {
                System.out.println("Procesando reporte '".concat(parametro).concat("'"));
                System.out.println("Recuperando consulta de la base de datos...");
                //4. Se debe resolver el cursor en el que se basa la generación del reporte
                ResultSet rs = cx.getRs(lhReportes.get("consulta_para_generar_reportes").toString());
                while (rs.next()) {
                   //5.  Se crea el reporte con nombre y plantilla 
                   
                   String nombreArchivo = lhReportes.get("nombre_archivo").toString();
                   
                   if (nombreArchivo.startsWith("%")) 
                       nombreArchivo = rs.getString(nombreArchivo.replace("%", ""));
                   
                   ReporteJasper reporte = new ReporteJasper(parametro,lhReportes.get("plantilla_reporte").toString());
                   //6. Se pasan parámetros al reporte
                   LinkedHashMap lhParametrosReporte = (LinkedHashMap) lhReportes.get("parametros");
                   Set<String> parametrosReporte = lhParametrosReporte.keySet();
                   for (String parametroReporte : parametrosReporte) {
                       //Es necesario validar tipos de datos
                       String tipoDato=lhParametrosReporte.get(parametroReporte).toString().split("\\|")[0];
                       String valor=lhParametrosReporte.get(parametroReporte).toString().split("\\|")[1]; 
                       if (tipoDato.equals("Integer")) {
                            //Se requiere validar si el valor es fijo o viene del cursor   
                            if (valor.startsWith("%"))   
                                reporte.getParametros().put(parametroReporte, rs.getInt(valor.replace("%", "")));
                            else
                                reporte.getParametros().put(parametroReporte, Integer.parseInt(valor));
                       } else if (tipoDato.equals("Date")) {
                            //Se requiere validar si el valor es fijo o viene del cursor   
                            if (valor.startsWith("%"))   
                                reporte.getParametros().put(parametroReporte, rs.getDate(valor.replace("%", "")));
                            else {
                                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                                java.util.Date theDate = formatter.parse(valor);
                                reporte.getParametros().put(parametroReporte, new java.sql.Date(theDate.getTime()));                           
                            }    
                       } else {
                           
                       }
                   }
                   //7. Se crea el archivo con el nombre especificado
                   System.out.println("Generando el archivo ".concat("C:\\reportes\\".concat(nombreArchivo).concat(".pdf")));
                   archivo = new FileOutputStream("C:\\reportes\\".concat(nombreArchivo).concat(".pdf"));
                   
                   reporte.setPrint(cx); 
                   //JasperExportManager.exportReportToPdfStream(reporte.getPrint(), "C:\\reportes\\".concat(nombreArchivo).concat(".pdf"));
                   JasperExportManager.exportReportToPdfStream(reporte.getPrint(),archivo);
                           
                   //9. Se anexa el arhcivo y se envía el mensaje
                   ArrayList<String> destinatarios = (ArrayList<String>) lhReportes.get("enviar_a");
                   for (String destinatario : destinatarios) {
                       System.out.println("Enviando mensaje con adjunto ".concat("C:\\reportes\\".concat(nombreArchivo).concat(".pdf")));
                       mail.sendEmail(this.configuracion.getParametros().get("usuario_smtp").toString(),
                                      destinatario, 
                                      lhReportes.get("asunto_email").toString(),
                                      lhReportes.get("mensaje_email").toString(),
                                      "C:\\reportes\\".concat(nombreArchivo).concat(".pdf"));                                
                   }
                   
           
                }
                System.out.println("");
            } catch (Fallo e)  {
                System.out.println("Error: ".concat(e.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, e);
            } catch(SQLException e) {
                System.out.println("Error al ejecutar consulta: ".concat(e.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, e);                
            } catch (ParseException ex) {
                System.out.println("Error al convertir fecha: ".concat(ex.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JRException ex) {
                System.out.println("Error al exportar reporte: ".concat(ex.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                 System.out.println("Archivo no encontrado: ".concat(ex.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                System.out.println("Error general: ".concat(ex.getMessage()));
                Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        System.out.println("Programa terminado exitosamente");
    }

    public static void main(String[] args) {

        try {
            if(args.length == 0) {
                System.out.println("Se requiere especificar el archivo de configuracion");
                System.exit(0);
            }    
            ReportSender rs = new ReportSender(args[0]); //ReportSender -"FIDE produccion"
            rs.sendReports();
        } catch (Exception ex) {
            System.out.print(ex.getMessage());
            Logger.getLogger(ReportSender.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
