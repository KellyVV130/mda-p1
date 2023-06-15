package util;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

public class PicturesChecker {
	private IFolder folder = null;
	
	public PicturesChecker(IFolder picturesFolder) {
		this.folder = picturesFolder;
	}
	
	private boolean checkRequirementDiagram(String reqDName) {
		return this.folder.getFile(reqDName) != null;
	}
	
	public boolean check() {
		return checkRequirementDiagram("req_Requirements");
	}

}
