package com.boot.jx.postman.doc;

public class ScriptusMediaDoc extends MediaDoc {
	private static final long serialVersionUID = 1L;
	public static final String COLLECTION_NAME = "SCRIPTUS_MEDIA";

	private String module;
	private String dir;
	private String subDir;
	private String path;

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public ScriptusMediaDoc module(String module) {
		this.module = module;
		return this;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public ScriptusMediaDoc dir(String dir) {
		this.dir = dir;
		return this;
	}

	public String getSubDir() {
		return subDir;
	}

	public void setSubDir(String subDir) {
		this.subDir = subDir;
	}

	public ScriptusMediaDoc subDir(String subDir) {
		this.subDir = subDir;
		return this;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ScriptusMediaDoc path(String path) {
		this.path = path;
		return this;
	}
}

