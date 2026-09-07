package com.boot.jx.postman.model.ext;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommonMsg {

    @JsonProperty("type")
    @ApiMockModelProperty(example = "image", value = "Message Type")
    public String type;

}
