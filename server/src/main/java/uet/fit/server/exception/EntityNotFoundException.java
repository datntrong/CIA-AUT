package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.server.DAO.entity.Entity;
import uet.fit.server.DAO.entity.annotation.Table;

public class EntityNotFoundException extends Exception {

	public EntityNotFoundException(@NotNull Class<? extends Entity> clz, @NotNull Object key) {
		super("Entity of table " + getTableName(clz) + " with key " + key + " is not found");
	}

	@Nullable
	private static String getTableName(Class<? extends Entity> clz) {
		if (clz.isAnnotationPresent(Table.class)) {
			Table table = clz.getAnnotation(Table.class);
			return table.value();
		}

		return null;
	}
}
