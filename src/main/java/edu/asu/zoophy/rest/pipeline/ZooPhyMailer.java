package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Responsible for sending ZooPhy email
 * @author devdemetri
 */
public class ZooPhyMailer {
	
	private final String USERNAME;
	private final String PASSWORD;
	private final String FROM;
	
	private final ZooPhyJob job;
	private final Logger log;
	
	public ZooPhyMailer(ZooPhyJob job) throws PipelineException {
		log = Logger.getLogger("ZooPhyMailer"+job.getID());
		this.job = job;
		PropertyProvider property = PropertyProvider.getInstance();
		USERNAME = property.getProperty("email.user");
		PASSWORD = property.getProperty("email.pass");
		FROM = property.getProperty("email.from");
	}
	
	/**
	 * Notifies user that their job started
	 * @throws MailerException 
	 */
	public void sendStartEmail() throws MailerException {
		log.info("Sending start email to: "+job.getReplyEmail());
		try {
	        String messageText;
	        messageText = "\nThank you for submitting ZooPhy Job Name: "+getCustomName()+".\nYour results will be sent as soon as the job is finished.";
	        if (!getCustomName().equals(job.getID())) {
	        	messageText += "\nNote: The Job ID for your ZooPhy Job is: "+job.getID();
	        }
	        if (job.isUsingGLM()) {
	        	messageText += "\nNote: GLM features were enabled for this ZooPhy Job.";
	        }
	        else {
	        	messageText += "\nNote: GLM features were NOT enabled for this ZooPhy Job.";
	        }
	        messageText += "\n\nThank You,\n\nZooPhy Lab";
	        sendEmail(messageText, null);    
		}
		catch (Exception e) {
			throw new MailerException(e.getMessage(), "Failed to send Start Email");
		}
	}
	
	/**
	 * Sends successful results to user
	 * @param results - Array of Files to send to user. results[0] must contain the .tree file. results[1] contains the GLB figure PDF if applicable.
	 * @throws MailerException 
	 */
	public void sendSuccessEmail(File[] results) throws MailerException {
		log.info("Sending results email to: "+job.getReplyEmail());
		try {
			String messageText;
			if (!getCustomName().equals(job.getID())) {
				messageText = "\nHere are your results for ZooPhy Job Name: "+getCustomName()+".\nNote: The Job ID for your ZooPhy Job is: "+job.getID();
	        }
	        else {
	        	messageText = "\nHere are your results for ZooPhy Job ID: "+job.getID()+".";
	        }
			messageText += "\nThe SpreaD3 simulation for your job is available <a href=\"https://zodo.asu.edu/spread3/"+job.getID()+"/renderers/d3/d3renderer/index.html\">here</a>.";
			messageText += "\nFor viewing the attached .tree file, we recommend downloading the latest version of <a href=\"http://tree.bio.ed.ac.uk/software/figtree/\">FigTree</a>.\n";
			messageText += "\nThank You,\nZooPhy Lab";
			sendEmail(messageText, results);
		}
		catch (Exception e) {
			throw new MailerException(e.getMessage(), "Failed to send Success Email");
		}
	}
	
	/**
	 * Sends a time estimate to user
	 * @param finishTime
	 * @param finalUpdate
	 */
	public void sendUpdateEmail(String finishTime, boolean finalUpdate) {
		log.info("Sending update email to: "+job.getReplyEmail());
		try {
			String messageText = "Your ZooPhy job is estimated to finish by "+finishTime+".";
	    	if (!finalUpdate) {
	    		messageText += "\nAnother time estimate will be sent midway through the process.";
	    	}
	    	//TODO: Email filters seem to be auto-clicking links and killing jobs. This route is not feasible. Find a work around. 
	    	//messageText += "\nIf the estimated finish time is too long, you can stop the job by clicking <a href=\"http://zodo.asu.edu:7007/stop?id="+job.getID()+"\">here</a>.";
	    	messageText += "\n\nThank You,\nZooPhy Lab";
	    	sendEmail(messageText, null);
		}
		catch (Exception e) {
			log.log(Level.WARNING, "Failed to send update email to: "+job.getReplyEmail()+" : "+e.getMessage());
		}
	}
	
	/**
	 * Notifies user that their job failed
	 * @param reason - Reason for the job failing
	 * @throws MailerException 
	 */
	public void sendFailureEmail(String reason) {
		log.info("Sending failure email to: "+job.getReplyEmail());
		try {
	        String msgText;
	        String err = "";
	        if (reason != null) {
	        	err = "\nError Cause: "+reason+"\n";
	        	err += "\nYou can retry your ZooPhy job <a href=\"https://zodo.asu.edu/zoophy/\">here.</a>";
	        }
	        if (!getCustomName().equals(job.getID())) {
	        	msgText = "\nThere was an error processing ZooPhy Job Name: "+getCustomName()+"."+err+"\nSorry for the inconvenience.\nNote: The Job ID for your ZooPhy Job is: "+job.getID()+"\n\nThank You,\n\nZooPhy Lab";
	        }
	        else {
	        	msgText = "\nThere was an error processing ZooPhy Job ID: "+job.getID()+"."+err+"\nSorry for the inconvenience.\n\nThank You,\n\nZooPhy Lab";
	        }
	        sendEmail(msgText, null);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to send failure email to: "+job.getReplyEmail()+" : "+e.getMessage());
		}
	}
	
	/**
	 * Sends the actual email with the given text and attachments
	 * @param messageText
	 * @param results
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void sendEmail(String messageText, File[] results) throws AddressException, MessagingException {
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "587");
		Session session = Session.getInstance(properties,
	    new javax.mail.Authenticator() {
		  protected PasswordAuthentication getPasswordAuthentication() {
			  return new PasswordAuthentication(USERNAME, PASSWORD);
		  }
	    });
		Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(job.getReplyEmail()));
        message.setSubject("ZooPhy Job: "+getCustomName());
        messageText = messageText.replaceAll("\n", "<br/>");
        messageText = messageText.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		if (results == null || results.length == 0) {
	        message.setContent(messageText, "text/html");
	        Transport.send(message);
		}
		else {
			File treeFile = results[0];
			File glmFile = results[1];
	        Multipart multipart = new MimeMultipart();
	        BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(messageText, "text/html");
	        multipart.addBodyPart(messageBodyPart);
	        BodyPart treeBodyPart = new MimeBodyPart();
	        DataSource treeSource = new FileDataSource(treeFile.getAbsolutePath());
	        treeBodyPart.setDataHandler(new DataHandler(treeSource));
	        String treeEnding = "";
	        if (treeFile.getName().contains(".")) {
	        	treeEnding = treeFile.getName().substring(treeFile.getName().lastIndexOf(".")).trim();
	        }
	        treeBodyPart.setFileName(getCustomName()+treeEnding);
	        multipart.addBodyPart(treeBodyPart);
	        if (glmFile != null) {
	        	BodyPart glmBodyPart = new MimeBodyPart();
		        DataSource glmSource = new FileDataSource(glmFile.getAbsolutePath());
		        glmBodyPart.setDataHandler(new DataHandler(glmSource));
		        String glmEnding = "_figure";
		        if (glmFile.getName().contains(".")) {
		        	glmEnding += glmFile.getName().substring(glmFile.getName().lastIndexOf(".")).trim();
		        }
		        glmBodyPart.setFileName(getCustomName()+glmEnding);
		        multipart.addBodyPart(glmBodyPart);
	        }
	        message.setContent(multipart);
	        Transport.send(message);
	        log.info("Email successfully sent to :"+job.getReplyEmail());
		}
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
