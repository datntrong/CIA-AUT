package uet.fit.server.app;

import uet.fit.server.rest.resource.BasicResource;
import uet.fit.server.rest.resource.CiaResource;
import uet.fit.server.rest.resource.ConfigPage;
import uet.fit.server.rest.resource.EnvironmentResource;
import uet.fit.server.rest.resource.FunctionResource;
import uet.fit.server.rest.resource.LogResource;
import uet.fit.server.rest.resource.RepoResource;
import uet.fit.server.rest.resource.TestCaseResource;
import uet.fit.server.rest.resource.UserCodeResource;
import uet.fit.server.rest.resource.UserResource;
import uet.fit.server.rest.service.SecureService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class HelloApplication extends Application {

	private final Set<Object> singletons = new HashSet<>();

	public HelloApplication() {
		// Register our hello service
		singletons.add(new BasicResource());
		singletons.add(new EnvironmentResource());
		singletons.add(new LogResource());
		singletons.add(new RepoResource());
		singletons.add(new UserResource());
		singletons.add(new FunctionResource());
		singletons.add(new TestCaseResource());
		singletons.add(new CiaResource());
		singletons.add(new ConfigPage());
		singletons.add(new UserCodeResource());
	}

	@Override
	public Set<Class<?>> getClasses() {
		return Set.of(SecureService.class);
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}