package uet.fit.aut.config;

import uet.fit.aut.config.pro.ProDefineNode;
import uet.fit.aut.config.pro.ProDependNode;
import uet.fit.aut.config.pro.ProHeaderNode;
import uet.fit.aut.config.pro.ProIncludeNode;
import uet.fit.aut.config.pro.ProSourceNode;
import uet.fit.aut.config.pro.ProVPathNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {

    /**
     * All files are added into SOURCES
     */
    private List<ProSourceNode> sources = new ArrayList<>();

    /**
     * All files are added into HEADERS
     */
    private List<ProHeaderNode> headers = new ArrayList<>();

    /**
     * All defines are added into DEFINES
     */
    private List<ProDefineNode> defines = new ArrayList<>();

    /**
     * All folder are added into INCLUDEPATH
     */
    private List<ProIncludeNode> includes = new ArrayList<>();

    /**
     * All folder are added into DEPENDPATH
     */
    private List<ProDependNode> depends = new ArrayList<>();

    /**
     * All folder are added into VPATH
     */
    private List<ProVPathNode> vPaths = new ArrayList<>();

    /**
     * The *.pro file location
     */
    private String proPath;

    private String projectPath;

    /**
     * The location of installed qt framework
     */
    private String qtDir;

    /**
     * Destination directory location
     */
    private String destDir;

    public List<ProSourceNode> getSources() { return sources; }

    public List<ProHeaderNode> getHeaders() { return headers; }

    public List<ProDefineNode> getDefines() { return defines; }

    public void setSources(List<ProSourceNode> sources){ this.sources = sources; }

    public void setHeaders(List<ProHeaderNode> headers){ this.headers = headers; }

    public void setDefines(List<ProDefineNode> defines){ this.defines = defines; }

    public List<ProIncludeNode> getIncludes() { return includes; }

    public List<ProDependNode> getDepends() { return depends; }

    public List<ProVPathNode> getvPaths() { return vPaths; }

    public void setIncludes(List<ProIncludeNode> includes){ this.includes = includes; }

    public void setDepends(List<ProDependNode> depends){ this.depends = depends; }

    public void setVPaths(List<ProVPathNode> vPaths){ this.vPaths = vPaths; }

    public void setProPath(String proPath) {
        this.proPath = proPath;
    }

    public String getProPath(){
        return proPath;
    }

    public void setQtDir(String qtDir) {
        this.qtDir = qtDir;
    }

    public String getQtDir() {
        return qtDir;
    }

    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }

    public String getDestDir() {
        return destDir;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    @Override
    public ProjectConfig clone() {
        ProjectConfig clone = new ProjectConfig();
        clone.setDestDir(getDestDir());
        clone.setProPath(getProPath());
        clone.setProjectPath(getProjectPath());
        clone.setQtDir(getQtDir());
        clone.setSources(getSources());
        clone.setHeaders(getHeaders());
        clone.setDefines(getDefines());
        clone.setVPaths(getvPaths());
        clone.setDepends(getDepends());
        clone.setIncludes(getIncludes());
        return clone;
    }
}
