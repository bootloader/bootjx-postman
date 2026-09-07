package com.boot.jx.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.boot.model.SafeKeyHashMap;
import com.boot.utils.ArgUtil;

public class AgentConfig implements Serializable {

	private static final long serialVersionUID = -3299316376121661559L;
	private String defaultBotName;
	private String defaultTeamCode;

	private Map<String, String> defaultAgents;

	public String getDefaultBotName() {
		return defaultBotName;
	}

	public void setDefaultBotName(String defaultBotName) {
		this.defaultBotName = defaultBotName;
	}

	public String getDefaultTeamCode() {
		return defaultTeamCode;
	}

	public void setDefaultTeamCode(String defaultTeam) {
		this.defaultTeamCode = defaultTeam;
	}

	public Map<String, String> getDefaultAgents() {
		return defaultAgents;
	}

	public void setDefaultAgents(Map<String, String> defaultAgents) {
		this.defaultAgents = defaultAgents;
	}

	public SafeKeyHashMap<String> defaultAgents() {
		if (ArgUtil.isEmpty(defaultAgents)) {
			defaultAgents = new HashMap<String, String>();
		}
		return new SafeKeyHashMap<String>(defaultAgents);
	}

	public String defaultAgent(String teamCode) {
		return defaultAgents().get(teamCode);
	}

}
