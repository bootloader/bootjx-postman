package com.boot.jx.postman.model.ext;

import java.io.Serializable;
import java.util.List;

import com.boot.jx.postman.model.TmplElement;
import com.boot.jx.swagger.ApiMockModelProperty;

public class CommonMsgOptions implements Serializable {

	private static final long serialVersionUID = 8738939562079737536L;

	public List<TmplElement> buttons;

	@ApiMockModelProperty(example = "auto", value = "Button Display Scheme", allowableValues = "auto,list,buttons")
	public String buttonDisplayScheme;

	public String style;

}