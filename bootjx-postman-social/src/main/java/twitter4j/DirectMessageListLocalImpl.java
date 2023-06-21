package twitter4j;

import java.util.ArrayList;

public class DirectMessageListLocalImpl extends ArrayList<DirectMessage>
		implements DirectMessageList, ResponseList<DirectMessage> {

	private static final long serialVersionUID = -3811190270818517732L;

	public DirectMessageListLocalImpl(int length) {
		super(length);
	}

	@Override
	public int getAccessLevel() {
		return 0;
	}

	@Override
	public RateLimitStatus getRateLimitStatus() {
		return null;
	}

	@Override
	public String getNextCursor() {
		return null;
	}

}
