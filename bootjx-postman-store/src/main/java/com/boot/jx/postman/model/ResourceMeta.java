package com.boot.jx.postman.model;

import java.util.HashMap;
import java.util.Map;

import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;

public interface ResourceMeta {

    public void setMeta(Map<String, Object> meta);

    public Map<String, Object> getMeta();

    public default Map<String, Object> meta() {
	if (!ArgUtil.is(getMeta())) {
	    this.setMeta(new HashMap<String, Object>());
	}
	return this.getMeta();
    }

    default boolean agentAllowed() {
	return MapModel.from(getMeta()).keyEntry("agentAllowed").asBoolean();
    }

    default void agentAllowed(boolean enabled) {
	meta().put("agentAllowed", enabled);
    }

}
