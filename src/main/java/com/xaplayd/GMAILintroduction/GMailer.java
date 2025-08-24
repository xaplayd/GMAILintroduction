package com.xaplayd.GMAILintroduction;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class GMailer {

	private static final String TEST_EMAIL = "suporte07adservi@gmail.com";
	private final Gmail service;

	public GMailer() throws Exception {
		NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
		service = new Gmail.Builder(httpTransport, jsonFactory, getCredentials(httpTransport, jsonFactory))
				.setApplicationName("Test Mailer").build();

	}

	private static Credential getCredentials(final NetHttpTransport httpTransport, GsonFactory jsonFactory)
			throws IOException {

		Path path = Path.of("C:/ws/data/secret.json");

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory,
				new InputStreamReader(new FileInputStream(path.toFile()), "UTF-8"));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory,
				clientSecrets, Set.of(GmailScopes.GMAIL_SEND))
				.setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile())).setAccessType("offline")
				.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	private void sendMail(String subject, String messageText) throws Exception {

	    Properties props = new Properties();
	    Session session = Session.getDefaultInstance(props, null);
	    MimeMessage email = new MimeMessage(session);
	    email.setFrom(new InternetAddress(TEST_EMAIL));
	    email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(TEST_EMAIL));
	    email.setSubject(subject, "UTF-8");

	    // Texto simples (fallback)
	    String plainText = "Olá!\n\nEste é um e-mail de teste.\n\nAcesse: https://www.google.com\n\nAtenciosamente,\nGMailer Bot";

	    // HTML formatado
	    String htmlContent = """
	        <html>
	          <body style="font-family: Arial, sans-serif; color: #333;">
	            <h2 style="color: #2e6c80;">Olá!</h2>
	            <p>Este é um <b>e-mail de teste</b> enviado com <span style="color: green;">formatação HTML</span>.</p>
	            <p>Você pode incluir:</p>
	            <ul>
	              <li>Listas</li>
	              <li><b>Negrito</b>, <i>itálico</i></li>
	              <li>Links: <a href="https://www.google.com">Google</a></li>
	            </ul>
	            <p>Atenciosamente,<br><i>GMailer Bot</i></p>
	          </body>
	        </html>
	    """;

	    // Cria a parte alternativa (text + html)
	    jakarta.mail.Multipart multipart = new jakarta.mail.internet.MimeMultipart("alternative");

	    // Parte texto simples
	    jakarta.mail.internet.MimeBodyPart textPart = new jakarta.mail.internet.MimeBodyPart();
	    textPart.setText(plainText, "utf-8");

	    // Parte HTML
	    jakarta.mail.BodyPart htmlPart = new jakarta.mail.internet.MimeBodyPart();
	    htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

	    // Adiciona as partes ao multipart
	    multipart.addBodyPart(textPart);
	    multipart.addBodyPart(htmlPart);

	    // Define o conteúdo do e-mail
	    email.setContent(multipart);

	    // Codifica e envia
	    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	    email.writeTo(buffer);
	    byte[] rawMessageBytes = buffer.toByteArray();
	    String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
	    Message msg = new Message();
	    msg.setRaw(encodedEmail);

	    try {
	        msg = service.users().messages().send("me", msg).execute();
	        System.out.println("Message ID: " + msg.getId());
	        System.out.println(msg.toPrettyString());
	    } catch (GoogleJsonResponseException e) {
	        GoogleJsonError error = e.getDetails();
	        if (error.getCode() == 403) {
	            System.err.println("Unable to send message " + e.getDetails());
	        } else {
	            throw e;
	        }
	    }
	}

	public static void main(String[] args) throws Exception {
		new GMailer().sendMail("E-mail com HTML + Texto", "");

	}

}
