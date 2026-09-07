package com.amx.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

import com.boot.jx.scope.tnt.Tenants;
import com.boot.utils.CryptoUtil;

public class MailFetchTest { // Noncompliant

	static BigDecimal country = new BigDecimal(30);
	static BigDecimal customer = new BigDecimal(30333);
	static String tnt = Tenants.DEFAULT_STR;
	static String FORMAT = "%10s : %-10s : %10s";

	public static final String XAUTH_DELIMITER = CryptoUtil.getEncoder().message("%01").decodeURL().toString();

	public static void main(String[] s) throws Exception {
		MailFetchTest test = new MailFetchTest();
		test.doTest();
		System.out.println("Ok");
	}

	private void doTest() throws MessagingException, IOException {

		final String username = "chat@mehery.com";
		final String passwd = "Gur29470";
		Properties props = new Properties();

		props.put("mail.store.protocol", "pop3");
		
//		props.put("mail.pop3s.ssl.enable", "false");
//		props.put("mail.pop3s.host", "outlook.office365.com");
//		props.put("mail.pop3s.starttls.enable", "false");
//		props.put("mail.pop3s.auth", "true");
//		props.put("mail.pop3s.port", "110");
//		props.put("mail.pop3s.disablecapa", "true");
		
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "false");
		props.put("mail.smtp.host", "smtp.office365.com");
		props.put("mail.smtp.port", "25");

		props.put("mail.host", "outlook.office365.com");
		props.put("mail.pop3.ssl.enable", "false");
		props.put("mail.pop3.starttls.enable", "false");
		props.put("mail.pop3.auth", "true");
		props.put("mail.pop3.port", "110");
		
		props.put("mail.debug", "true");

		System.out.println("=========" + CryptoUtil.getEncoder().message(
				"UABOADIAUABSADAAMQBDAEEAMAAyADMAOAAuAEkATgBEAFAAUgBEADAAMQAuAFAAUgBPAEQALgBPAFUAVABMAE8ATwBLAC4AQwBPAE0A")
				.decodeBase64().toString());

		System.out.println("=========" + CryptoUtil.getEncoder()
				.message("dXNlcj10ZXN0QGNvbnRvc28ub25taWNyb3NvZnQuY29tAWF1dGg9QmVhcmVy"
						+ "IEV3QkFBbDNCQUFVRkZwVUFvN0ozVmUwYmpMQldaV0NjbFJDM0VvQUEBAQ==")
				.decodeBase64().encodeURL().toString());

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
//
//				StringJoiner sj = new StringJoiner(XAUTH_DELIMITER);
//				sj.add("user=" + username);
//				sj.add("auth=Bearer " + passwd);
//				sj.add("").add("");
//				System.out.println("pass  " + sj.toString());
//
//				String pass = CryptoUtil.getEncoder().message(sj.toString()).encodeBase64().toString();
//
//				System.out.println(
//						"=========" + CryptoUtil.getEncoder().message(pass).decodeBase64().encodeURL().toString());

				return new PasswordAuthentication(username, passwd);
			}
		});

		Store store = session.getStore("pop3s");
		store.connect();
		Folder emailFolder = store.getFolder("INBOX");

		emailFolder.open(Folder.READ_ONLY);

		// retrieve the messages from the folder in an array and print it
		Message[] messages = emailFolder.getMessages();
		System.out.println("messages.length---" + messages.length);

		for (int i = 0, n = messages.length; i < n; i++) {
			Message message = messages[i];
			System.out.println("---------------------------------");
			System.out.println("Email Number " + (i + 1));
			System.out.println("Subject: " + message.getSubject());
			System.out.println("From: " + message.getFrom()[0]);
		}

		// close the store and folder objects
		emailFolder.close(false);
		store.close();

	}
}
