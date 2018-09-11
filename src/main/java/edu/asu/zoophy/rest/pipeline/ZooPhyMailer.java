package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * @author devdemetri, kbhangal
 */
public class ZooPhyMailer {
	
	private final String USERNAME;
	private final String PASSWORD;
	private final String FROM;
	private final String DIRECTORY;
	
	private final ZooPhyJob job;
	private final Logger log;
	
	public ZooPhyMailer(ZooPhyJob job) throws PipelineException {
		log = Logger.getLogger("ZooPhyMailer"+job.getID());
		this.job = job;
		PropertyProvider property = PropertyProvider.getInstance();
		USERNAME = property.getProperty("email.user");
		PASSWORD = property.getProperty("email.pass");
		FROM = property.getProperty("email.from");
		DIRECTORY = property.getProperty("job.files.dir");//"/Users/bhangal/Desktop/Zoophy/zoophy/zoophy-services/ZooPhyJobs/";
		
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
			// warning message
			messageText += "\n\nPlease note that ZooPhy is not a black box and should not be treated as such.\nThe generated results require assumptions that may prove to be incorrect given the selected data. Please carefully inspect your results and interpret them in light of the most current literature.\nWe recommend that you replicate your results using the same parameters and, in addition, test alternative assumptions to ensure that your results will stand up to scientific scrutiny.\n\n";
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
	        Multipart multipart = new MimeMultipart();
	        BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(messageText, "text/html");
	        multipart.addBodyPart(messageBodyPart);
	        
	        File zipFile = zipFile(results);
	        BodyPart zipBodyPart = new MimeBodyPart();
	        DataSource zipSource = new FileDataSource(zipFile.getAbsolutePath());
	        zipBodyPart.setDataHandler(new DataHandler(zipSource));
	        String zipEnding = "";
	        if (zipFile.getName().contains(".")) {
	        		zipEnding = zipFile.getName().substring(zipFile.getName().lastIndexOf(".")).trim();
	        }
	        zipBodyPart.setFileName(getCustomName()+zipEnding);
	        multipart.addBodyPart(zipBodyPart);
	        
	        message.setContent(multipart);
	        Transport.send(message);
	        log.info("Email successfully sent to :"+job.getReplyEmail());
		}
	}
	
	private File zipFile(File[] files) {
        try {
            String zipFileName =  getCustomName().concat(".zip");
            File zFile = new File(DIRECTORY + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zFile));
            
            for(File file: files) {
            		if(file != null) {
	            		zos.putNextEntry(new ZipEntry(file.getName()));
	                byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
	                zos.write(bytes, 0, bytes.length);
	                zos.closeEntry();
            		}
            }
            
            zos.close();
            return zFile;
 
        } catch (FileNotFoundException e) {
        	log.log(Level.SEVERE, "The file does not exist for: "+job.getReplyEmail()+" : "+e.getMessage());
        } catch (IOException e) {
        	log.log(Level.SEVERE, "I/O error for: "+job.getReplyEmail()+" : "+e.getMessage());
        }
        
        return null;
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
