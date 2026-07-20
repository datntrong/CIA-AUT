package uet.fit.client.common;

import uet.fit.util.Base64;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

import static uet.fit.util.AuthorizationConst.*;

public class ClientFilterRequest implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		String username = User.getInstance().getUsername();
		String password = User.getInstance().getPassword();
		String rawString = username + DELIMITER + password;
		String encodedString = Base64.encodeAsString(rawString);
		String authorizationValue = AUTHORIZATION_HEADER_PREFIX + " " + encodedString;
		clientRequestContext.getHeaders().add(AUTHORIZATION_HEADER_KEY, authorizationValue);
	}
}
