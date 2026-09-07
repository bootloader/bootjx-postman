package com.boot.jx.postman.dto;

import java.util.List;

public class BlockUsersRequest {
	 private String channelId;
	 private List<BlockUsers> block_users;  // name matching JSON key
	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	public List<BlockUsers> getBlock_users() {
		return block_users;
	}
	public void setBlock_users(List<BlockUsers> block_users) {
		this.block_users = block_users;
	}
}
