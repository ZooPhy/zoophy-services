package edu.asu.zoophy.pipeline;

import java.io.File;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Responsible for sending ZooPhy email
 * @author devdemetri
 */
public class ZooPhyMailer {
	
	@Autowired
	private Environment env;
	
	private final String username;
	private final String password;
	private final String from;
	private final ZooPhyJob job;
	private Logger log = Logger.getLogger("BeastMailer");
	
	public ZooPhyMailer(ZooPhyJob job) {
		this.username = env.getProperty("email.user");
		this.password = env.getProperty("email.pass");
		this.from = env.getProperty("email.from");
		this.job = job;
	}
	
	/**
	 * Notifies user that their job started
	 * @throws MailerException 
	 */
	public void sendStartEmail() throws MailerException {
		log.info("Sending start email to: "+job.getReplyEmail());
		try {
	        String messageText;
	        if (!getCustomName().equals(job.getID())) {
	        	messageText = "\nThank you for submitting ZooPhy Job Name: "+getCustomName()+".\nYour results will be sent as soon as the job is finished.\nNote: The Job ID for your ZooPhy Job is: "+job.getID()+"\n\nThank You,\n\nZooPhy Lab";
	        }
	        else {
	        	messageText = "\nThank you for submitting ZooPhy Job ID: "+getCustomName()+".\nYour results will be sent as soon as the job is finished.\n\nThank You,\n\nZooPhy Lab";
	        }
	        sendEmail(messageText, null);    
		}
		catch (Exception e) {
			throw new MailerException(e.getMessage(), "Failed to send Start Email");
		}
	}
	
	/**
	 * Sends successful results to user
	 * @throws MailerException 
	 */
	public void sendSuccessEmail() throws MailerException {
		try {
			
		}
		catch (Exception e) {
			throw new MailerException(e.getMessage(), "Failed to send Success Email");
		}
	}
	
	/**
	 * Notifies user that their job failed
	 * @param reason - Reason for the job failing
	 * @throws MailerException 
	 */
	public void sendFailureEmail(String reason) {
		try {
	        String msgText;
	        String err = "";
	        if (reason != null) {
	        	err = "\nError Cause: "+reason+"\n";
	        	err += "\nYou can retry your ZooPhy job <a href=\"http://zodo.asu.edu:8080/zoophy/\">here.</a>";
	        }
	        if (!getCustomName().equals(job.getID())) {
	        	msgText = "\nThere was an error processing ZooPhy Job Name: "+getCustomName()+"."+err+"\nSorry for the inconvenience.\nNote: The Job ID for your ZooPhy Job is: "+job.getID()+"\n\nThank You,\n\nZooPhy Lab";
	        }
	        else {
	        	msgText = "\nThere was an error processing ZooPhy Job ID: "+job.getID()+"."+err+"\nSorry for the inconvenience.\n\nThank You,\n\nZooPhy Lab";
	        }
	        msgText = msgText.replaceAll("\n", "<br/>");
	        msgText = msgText.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	        sendEmail(msgText, null);
		}
		catch (Exception e) {
			log.error("Failed to send failure email to: "+job.getReplyEmail()+" : "+e.getMessage());
		}
	}
	
	/**
	 * Sends the actual email with the given text and attachments
	 * @param messageText
	 * @param attachement
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void sendEmail(String messageText, File attachement) throws AddressException, MessagingException {
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "587");
		Session session = Session.getInstance(properties,
	    new javax.mail.Authenticator() {
		  protected PasswordAuthentication getPasswordAuthentication() {
			  return new PasswordAuthentication(username, password);
		  }
	    });
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(job.getReplyEmail()));
        message.setSubject("ZooPhy Job: "+getCustomName());
        messageText = messageText.replaceAll("\n", "<br/>");
        messageText = messageText.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        message.setContent(messageText, "text/html");
        if (attachement != null) {
        	//TODO: add attachments
        }
        Transport.send(message);
	}
	
	/**
	 * @return Job Name if exists, otherwise Job ID
	 */
	private String getCustomName() {
		if (job.getJobName() != null) {
			return job.getJobName();
		}
		else {
			return job.getID();
		}
	}
	
}
