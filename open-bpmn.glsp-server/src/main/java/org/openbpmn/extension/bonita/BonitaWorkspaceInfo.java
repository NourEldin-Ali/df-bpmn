package org.openbpmn.extension.bonita;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * This class to get the Bonita project based on the workspace of Bonita.
 * @author Ali Nour Eldin
 *
 */
public class BonitaWorkspaceInfo {
	String bonitaWorkSpacePath;

	public BonitaWorkspaceInfo(String workspacePath) {
		bonitaWorkSpacePath = workspacePath;
	}
	
	/**
	 * This function to get list of string of the path of each Bonita workspace
	 * @return
	 */
	public String[] getProjects() {
		String[] strForlderArray = {};
		List<String> listOfProjects = new ArrayList<String>();
		//access to workcpace forlder
		File folder = new File(bonitaWorkSpacePath);
		//validate the path
		if(!folder.canRead()) {
			return strForlderArray;
		}
		//get list of files and forlders
		File[] listOfFiles = folder.listFiles();
		//validate the files/forlder type
		//we need only folders
		for (File file : listOfFiles) {
			if (file.isDirectory()) {
				//ignore this folders, are not a projects
				if (!file.getName().contentEquals(".metadata") && !file.getName().contentEquals("tomcat")
						&& !file.getName().contentEquals("server_configuration")) {
					listOfProjects.add(file.getPath());
				}
			}
		}

		strForlderArray = new String[listOfProjects.size()];
		listOfProjects.toArray(strForlderArray);
		return strForlderArray;
	}
}
