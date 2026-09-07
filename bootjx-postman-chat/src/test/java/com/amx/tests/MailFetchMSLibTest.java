package com.amx.tests;

import java.math.BigDecimal;

import com.boot.jx.scope.tnt.Tenants;
import com.boot.utils.CryptoUtil;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;

public class MailFetchMSLibTest { // Noncompliant

	static BigDecimal country = new BigDecimal(30);
	static BigDecimal customer = new BigDecimal(30333);
	static String tnt = Tenants.DEFAULT_STR;
	static String FORMAT = "%10s : %-10s : %10s";

	public static final String XAUTH_DELIMITER = CryptoUtil.getEncoder().message("%01").decodeURL().toString();

	public static void main(String[] s) throws Exception {
		MailFetchMSLibTest test = new MailFetchMSLibTest();
		test.doTest();
		System.out.println("Ok");
	}

	private void doTest() throws Exception {
		ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2); // This is the latest version
																							// of this library

		ExchangeCredentials credentials = new WebCredentials("chat@mehery.com", "bqdzbpxkpfmtxqzy","mehery.onmicrosoft.com");
		service.setCredentials(credentials);
		// this.exchangeService.setWebProxy(new WebProxy("xx.xxx.xxx.xx", 8080)); // If
		// you're behind a proxy
		service.autodiscoverUrl("chat@mehery.com");
		//service.setUrl(new URI("https://outlook.office365.com/EWS/Exchange.asmx")); // This is the standard URL

		Folder inboxFolder = Folder.bind(service, WellKnownFolderName.Inbox);

		FindItemsResults<Item> results = service.findItems(inboxFolder.getId(), new ItemView(10)); // 10 is the number
																									// of items to fetch
																									// (pagesize)

		for (Item result : results) {
			EmailMessage currentEmail = (EmailMessage) result;

			System.out.println(currentEmail.getFrom());
			// And so on
		}
	}

}
