package com.boot.jx.email;

import java.util.ArrayList;
import java.util.List;

import com.boot.utils.CommonStringUtils;

public class Email {
	private List<Fragment> fragments = new ArrayList<Fragment>();

	public Email(List<Fragment> fragments) {
		this.fragments = fragments;
	}

	public List<Fragment> getFragments() {
		return fragments;
	}

	public String getVisibleText() {
		List<String> visibleFragments = new ArrayList<String>();
		for (Fragment fragment : fragments) {
			if (!fragment.isHidden())
				visibleFragments.add(fragment.getContent());
		}
		return CommonStringUtils.stripEnd(CommonStringUtils.join(visibleFragments, "\n"), null);
	}

	public String getHiddenText() {
		List<String> hiddenFragments = new ArrayList<String>();
		for (Fragment fragment : fragments) {
			if (fragment.isHidden())
				hiddenFragments.add(fragment.getContent());
		}
		return CommonStringUtils.stripEnd(CommonStringUtils.join(hiddenFragments, "\n"), null);
	}

}
