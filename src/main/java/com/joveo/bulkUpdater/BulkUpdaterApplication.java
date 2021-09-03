package com.joveo.bulkUpdater;


public class BulkUpdaterApplication {

	public static void main(String[] args) {
		CliUtils.setup(args);
		ReadUtil.readFile();
	}
}
