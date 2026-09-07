package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;

public class InBoundContactProfile {

    @ApiMockModelProperty(example = "John Doe", value = "Name collected from Channel", required = false)
    public String name;

    @ApiMockModelProperty(example = "919988776655", value = "Mobile Number collected from Channel", required = false)
    public String phone;

    @ApiMockModelProperty(example = "abc@xyz.com", value = "Email Id collected from Channel", required = false)
    public String email;

}
