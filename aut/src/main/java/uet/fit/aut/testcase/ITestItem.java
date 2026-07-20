package uet.fit.aut.testcase;

import java.time.LocalDateTime;
import java.util.List;

public interface ITestItem {

    String getName();

    void setName(String name);

    void setCreationDateTime(LocalDateTime creationDateTime);

    LocalDateTime getCreationDateTime();

    void setLastModifiedTime(LocalDateTime lastModifiedTime);

    LocalDateTime getLastModifiedTime();

    String getPath();

    void setPath(String path);

    boolean isPrototypeTestcase();

    List<String> getAdditionalIncludes();
}
