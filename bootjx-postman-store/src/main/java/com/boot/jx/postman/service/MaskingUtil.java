package com.boot.jx.postman.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBPhone;

public class MaskingUtil {
	
	public static List<CustomerProfileDoc> maskList(List<CustomerProfileDoc> docLst){
		
		
		List<CustomerProfileDoc> msk =new ArrayList<>();
		for(CustomerProfileDoc dc:docLst) {
			msk.add(mask(dc));
		}
		return msk;
	}

    public static CustomerProfileDoc mask(CustomerProfileDoc doc) {
        if (doc == null) return null;

        // Mask phones
        if (doc.getPhones() != null) {
            Set<PBPhone> maskedPhones = doc.getPhones().stream()
                    .map(MaskingUtil::maskPhone)
                    .collect(Collectors.toSet());

            doc.setPhones(maskedPhones);
        }

        // Mask emails
        if (doc.getEmails() != null) {
            Set<PBEmail> maskedEmails = doc.getEmails().stream()
                    .map(MaskingUtil::maskEmail)
                    .collect(Collectors.toSet());

            doc.setEmails(maskedEmails);
        }
        
        
        
        // ---------------- ADDITIONAL INFO ----------------
        Map<String, Object> additionalInfo = doc.getAdditionalInfo();

        if (additionalInfo != null) {

            // ALT PHONES (SAFE CAST)
            Object altPhonesObj = additionalInfo.get("alt_phones");

            if (altPhonesObj instanceof List<?>) {

                List<?> altPhonesRaw = (List<?>) altPhonesObj;

                List<PBPhone> maskedAltPhones = altPhonesRaw.stream()
                        .filter(Objects::nonNull)
                        .map(item -> (PBPhone) item)
                        .map(MaskingUtil::maskPhone)
                        .collect(Collectors.toList());

                additionalInfo.put("alt_phones", maskedAltPhones);
            }

            // ALT EMAILS (SAFE CAST)
            Object altEmailsObj = additionalInfo.get("alt_emails");

            if (altEmailsObj instanceof List<?>) {

                List<?> altEmailsRaw = (List<?>) altEmailsObj;

                List<PBEmail> maskedAltEmails = altEmailsRaw.stream()
                        .filter(Objects::nonNull)
                        .map(item -> (PBEmail) item)
                        .map(MaskingUtil::maskEmail)
                        .collect(Collectors.toList());

                additionalInfo.put("alt_emails", maskedAltEmails);
            }
        }




        return doc;
    }

    // ---------------- PHONE MASK ----------------
    private static PBPhone maskPhone(PBPhone p) {
        if (p == null) return null;

        String countryCode = p.getCountryCallingCode();
        String national = p.getNationalNumber();
        System.out.println("countryCode :"+countryCode+"\t national :"+national);

        if (national != null && national.length() >= 1) {
            String last2 = national.substring(national.length() - 0);
           // p.setPhone("+" + countryCode + "*******" + last2);
            p.setCountryCallingCode("***");
            p.setNationalNumber("**********");
            p.setPhone("**********");
        } else {
            p.setPhone("**********");
        }

        return p;
    }

    // ---------------- EMAIL MASK ----------------
    private static PBEmail maskEmail(PBEmail e) {
        if (e == null || e.getEmail() == null) return e;

        String email = e.getEmail();
        if (!email.contains("@")) {
            e.setEmail("*******");
            return e;
        }

        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];

        String maskedName = name.length() <= 1
                ? name.charAt(0) + "*"
                : name.substring(0, 1) + "***";

        String maskedDomain = domain.replaceAll("(^.).*?(\\.)", "$1***$2");

       // e.setEmail(maskedName + "@" + maskedDomain);
        e.setEmail("**********");

        return e;
    }
}