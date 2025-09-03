package es.jonay.kb.shopsystem.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import es.jonay.kb.shopsystem.api.utils.HTMLTemplates;
import es.jonay.kb.shopsystem.model.entities.Item;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;


@Service
public class MailService {
	private static final String EMAIL_ENDPOINT = "mail/send";

	private final SendGrid sendGrid;
	private final Email fromEmail;

	private static final String ADMIN_EMAIL = "jonaykb@gmail.com";

	public MailService(SendGrid sendGrid, Email fromEmail) {
		this.sendGrid = sendGrid;
		this.fromEmail = fromEmail;
	}

	public void dispatchEmail(String emailId, String subject, String body) {
		Email toEmail = new Email(emailId);
		Content content = new Content("text/html", body);
		Mail mail = new Mail(fromEmail, subject, toEmail, content);		

		Request request = new Request();
		request.setMethod(Method.POST);
		request.setEndpoint(EMAIL_ENDPOINT);
		try {
			request.setBody(mail.build());

			Response response = sendGrid.api(request);
			if (response.getStatusCode() != 202) {
				throw new RuntimeException("Error sending email: " + response.getBody());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error sending email: " + e);
		}
	}

	public void sendOutOfStockEmail(List<Item> itemsOutOfStock){
		List<String> productNames = itemsOutOfStock.stream()
            .map(Item::getName)
            .toList();
		dispatchEmail(ADMIN_EMAIL,"PRODUCTOS SIN STOCK" , HTMLTemplates.lowStockAlert(productNames));
	}

	public void sendUnderBareMinimunItemsEmail(List<Item> underBareMinimunItems) {
		dispatchEmail(ADMIN_EMAIL,"PRODUCTOS CON POCO STOCK" , HTMLTemplates.lowBareMinimunAlert(underBareMinimunItems));
	}

}