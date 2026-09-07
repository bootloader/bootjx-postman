package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;

public class ContactBusinessProfile {

    @ApiMockModelProperty(example = "C34567", value = "Unique Id assigned to Contact by Core Business Application",
	    required = false)
    public String profileId;

    @ApiMockModelProperty(example = "John Doe", value = "Name collected from Business", required = false)
    public String name;

    @ApiMockModelProperty(example = "919988776655", value = "Mobile Number collected from Business", required = false)
    public String phone;

    @ApiMockModelProperty(example = "abc@xyz.com", value = "Email Id collected from Business", required = false)
    public String email;

}
