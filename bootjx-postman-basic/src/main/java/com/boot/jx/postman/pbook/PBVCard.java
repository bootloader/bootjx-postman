package com.boot.jx.postman.pbook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PBVCard implements Serializable {
	private static final long serialVersionUID = -673836125257348776L;

	public PBName name;
	public List<PBPhone> phones;
	public List<PBEmail> emails;
	public List<PBAddress> addresses;
	public List<PBWebsite> urls;
	public List<PBSocial> ims;
	public List<PBWork> work;
	public List<PBDate> dates;
	public List<PBLocation> locations;

	public List<PBPhone> getPhones() {
		return phones;
	}

	public void setPhones(List<PBPhone> phones) {
		this.phones = phones;
	}

	public List<PBEmail> getEmails() {
		return emails;
	}

	public void setEmails(List<PBEmail> emails) {
		this.emails = emails;
	}

	public List<PBAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<PBAddress> addresses) {
		this.addresses = addresses;
	}

	public List<PBWebsite> getUrls() {
		return urls;
	}

	public void setUrls(List<PBWebsite> urls) {
		this.urls = urls;
	}

	public List<PBPhone> phones() {
		if (this.phones == null) {
			this.phones = new ArrayList<PBPhone>();
		}
		return this.phones;
	}

	public List<PBEmail> emails() {
		if (this.emails == null) {
			this.emails = new ArrayList<PBEmail>();
		}
		return this.emails;
	}

	public List<PBAddress> addresses() {
		if (this.addresses == null) {
			this.addresses = new ArrayList<PBAddress>();
		}
		return this.addresses;
	}

	public List<PBWebsite> urls() {
		if (this.urls == null) {
			this.urls = new ArrayList<PBWebsite>();
		}
		return this.urls;
	}

	public PBName getName() {
		return name;
	}

	public void setName(PBName name) {
		this.name = name;
	}

	public List<PBSocial> getIms() {
		return ims;
	}

	public void setIms(List<PBSocial> ims) {
		this.ims = ims;
	}

	public List<PBSocial> ims() {
		if (this.ims == null) {
			this.ims = new ArrayList<PBSocial>();
		}
		return this.ims;
	}

	public List<PBWork> getWork() {
		return work;
	}

	public void setWork(List<PBWork> work) {
		this.work = work;
	}

	public List<PBWork> work() {
		if (this.work == null) {
			this.work = new ArrayList<PBWork>();
		}
		return this.work;
	}

	public List<PBDate> getDates() {
		return dates;
	}

	public void setDates(List<PBDate> dates) {
		this.dates = dates;
	}

	public List<PBDate> dates() {
		if (this.dates == null) {
			this.dates = new ArrayList<PBDate>();
		}
		return this.dates;
	}

	public List<PBLocation> getLocations() {
		return locations;
	}

	public void setLocations(List<PBLocation> locations) {
		this.locations = locations;
	}

	public List<PBLocation> locations() {
		if (this.locations == null) {
			this.locations = new ArrayList<PBLocation>();
		}
		return this.locations;
	}

	public PBVCard locations(PBLocation location) {
		this.locations().add(location);
		return this;
	}
}
