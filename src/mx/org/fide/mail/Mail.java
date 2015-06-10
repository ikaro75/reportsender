package mx.org.fide.mail;

import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mail {
    String error;
    Properties properties = new Properties();
    
    public Mail () {
        
    }
    
    public Mail(String starttls, String user,String auth, String pass, String separator, String host, String port) {
        properties.put("mail.smtp.starttls.enable", starttls);
        properties.put("mail.smtp.user", user);
        properties.put("mail.smtp.auth", auth);
        properties.put("pass", pass);        
        properties.put("separator", separator);
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
    }
    
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
    public static void main(String[] args) {
        Mail demo = new Mail("false","noresponder@fide.org.mx","true","noresponder",";","10.55.210.34","25");
        try {
            demo.sendEmail("noresponder@fide.org.mx", "daniel.martinez05@cfe.gob.mx", 
                           "Prueba",  "1,2,3" ,null); 
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    public void sendEmail(String from, String to, String subject, String bodyText, String filename) throws Exception{
       
        Session session = Session.getDefaultInstance(properties, null);
        //session.setDebug(true);
        Transport t = session.getTransport("smtp");
        
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(properties.get("mail.smtp.user").toString()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setSentDate(new Date());
            
            //
            // Set the email message text.
            //
            MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(bodyText);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            
            if (filename!=null) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(filename);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(filename);
                multipart.addBodyPart(attachmentPart);
            }
            
            message.setContent(multipart);
            if (properties.getProperty("mail.smtp.auth").equals("true"))
                t.connect(properties.getProperty("mail.smtp.user"),properties.getProperty("pass"));
           
            t.sendMessage(message, message.getAllRecipients()); 
            
            //Transport.send(message);
        } catch (MessagingException e) {             System.out.println(e.getMessage());
             throw new Exception (e.getMessage()); 
            //e.printStackTrace();
        } finally {
            t.close();
        }
    }
}