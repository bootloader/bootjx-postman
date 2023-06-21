package com.boot.jx.postman.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.boot.utils.ArgUtil;

public class TagDocument implements Serializable {
	private static final long serialVersionUID = -7943342223656346570L;

	public TagDocument() {
		super();
		this.sentimentScore = 0;
	}

	List<String> categories;
	List<String> persons;
	List<String> locations;
	List<String> cities;
	List<String> countries;
	List<String> organizations;
	List<String> sentiments;
	List<String> langs;

	int sentimentScore = 0;

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> categories() {
		if (!ArgUtil.is(this.categories)) {
			this.categories = new ArrayList<String>();
		}
		return this.categories;
	}

	public List<String> getPersons() {
		return persons;
	}

	public void setPersons(List<String> persons) {
		this.persons = persons;
	}

	public List<String> persons() {
		if (!ArgUtil.is(this.persons)) {
			this.persons = new ArrayList<String>();
		}
		return this.persons;
	}

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public List<String> locations() {
		if (!ArgUtil.is(this.locations)) {
			this.locations = new ArrayList<String>();
		}
		return this.locations;
	}

	public List<String> getSentiments() {
		return sentiments;
	}

	public void setSentiments(List<String> sentiments) {
		this.sentiments = sentiments;
	}

	public List<String> sentiments() {
		if (!ArgUtil.is(this.sentiments)) {
			this.sentiments = new ArrayList<String>();
		}
		return this.sentiments;
	}

	public List<String> getCities() {
		return cities;
	}

	public void setCities(List<String> cities) {
		this.cities = cities;
	}

	public List<String> cities() {
		if (!ArgUtil.is(this.cities)) {
			this.cities = new ArrayList<String>();
		}
		return this.cities;
	}

	public List<String> getCountries() {
		return countries;
	}

	public void setCountries(List<String> countries) {
		this.countries = countries;
	}

	public List<String> countries() {
		if (!ArgUtil.is(this.countries)) {
			this.countries = new ArrayList<String>();
		}
		return this.countries;
	}

	public List<String> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<String> organizations) {
		this.organizations = organizations;
	}

	public List<String> organizations() {
		if (!ArgUtil.is(this.organizations)) {
			this.organizations = new ArrayList<String>();
		}
		return this.organizations;
	}

	public int getSentimentScore() {
		return sentimentScore;
	}

	public void setSentimentScore(int sentimentScore) {
		this.sentimentScore = sentimentScore;
	}

	public List<String> getLangs() {
		return langs;
	}

	public void setLangs(List<String> langs) {
		this.langs = langs;
	}

	public List<String> langs() {
		if (!ArgUtil.is(this.langs)) {
			this.langs = new ArrayList<String>();
		}
		return this.langs;
	}
}
