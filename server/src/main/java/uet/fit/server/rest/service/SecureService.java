package uet.fit.server.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.util.IRegex;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.util.Base64;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.StringTokenizer;

import static uet.fit.util.AuthorizationConst.AUTHORIZATION_HEADER_KEY;
import static uet.fit.util.AuthorizationConst.AUTHORIZATION_HEADER_PREFIX;
import static uet.fit.util.AuthorizationConst.DELIMITER;

@Provider
public class SecureService implements ContainerRequestFilter { 

	private static final Logger logger = LoggerFactory.getLogger(SecureService.class);

	private final UserService userService = new UserService();

	private final String[] IGNORE_PATHS = new String[]{"", "check", "user", "config", "regression", "cia"};

	@Override
	public void filter(ContainerRequestContext containerRequestContext) {
		final UriInfo uriInfo = containerRequestContext.getUriInfo();
		final String path = uriInfo.getPath();
		final String method = containerRequestContext.getMethod();

		if (needAuthorization(uriInfo)) {
			List<String> authHeader = containerRequestContext.getHeaders().get(AUTHORIZATION_HEADER_KEY);
			if (authHeader != null && !authHeader.isEmpty()) {
				String authToken = authHeader.get(0);
				String regex = AUTHORIZATION_HEADER_PREFIX + IRegex.SPACE;
				authToken = authToken.replaceFirst(regex, SpecialCharacter.EMPTY);
				String encodedString = Base64.decodeAsString(authToken);
				StringTokenizer tokenizer = new StringTokenizer(encodedString, DELIMITER);
				String username = tokenizer.nextToken();
				String password = tokenizer.nextToken();

				if (userService.authorize(username, password)) {
					if (!path.equals("/log"))
						logger.debug("Allow " + username + " to " + method + " " + path);
				} else {
					logger.debug("Disallow " + username + " to " + method + " " + path + ": wrong user & password");
					Response unauthorizedRes = Response.status(Response.Status.UNAUTHORIZED)
							.entity("Wrong username or password. Cannot access this API")
							.build();
					containerRequestContext.abortWith(unauthorizedRes);
				}

			} else if (authHeader == null) {
				logger.debug("Disallow someone to " + method + " " + path + ": no authorization header");
				Response unauthorizedRes = Response.status(Response.Status.UNAUTHORIZED)
						.entity("No authorization. Cannot access this API")
						.build();
				containerRequestContext.abortWith(unauthorizedRes);
			}
		} else {
			logger.debug("Ignore authorizing " + method + " " + path);
		}
	}

	private boolean needAuthorization(UriInfo uriInfo) {
		boolean result = true;

		List<PathSegment> pathSegments = uriInfo.getPathSegments();
		if (!pathSegments.isEmpty()) {
			PathSegment firstSeg = pathSegments.get(0);
			String path = firstSeg.getPath();
			for (String ip : IGNORE_PATHS) {
				if (ip.equals(path)) {
					result = false;
					break;
				}
			}
		}

		return result;
	}
}
