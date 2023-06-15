package util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class PicturesChecker {
	/**
	 * picture files container.
	 */
	private IFolder folder = null;
	
	/**
	 * list of picture files.
	 */
	private List<IFile> pictures = null;
	
	/**
	 * list of picture file names without file extension.
	 */
	private List<String> pictureNames = null;
	
	/**
	 * list of picture file prefixes. In picture file name are before the underscore.
	 */
	private ArrayList<String> prefixes = null;
	
	/**
	 * the file extension of picture file in lower case.
	 */
	private String PIC_EXTENSION="png";
	
	/**
	 * the dot.
	 */
	private String DOT = ".";
	
	/**
	 * the underscore.
	 */
	private String UNDERSCORE="_";
	
	/**
	 * the constructor
	 * @param picturesFolder - folder containing picture files. 
	 * Should be in the same folder of the uml file
	 * @throws CoreException
	 */
	public PicturesChecker(IFolder picturesFolder) throws CoreException {
		this.folder = picturesFolder;
		pictures = new ArrayList<IFile>();
		prefixes = new ArrayList<String>();
		prefixes.add("req");
		prefixes.add("bdd");
		prefixes.add("uc");
		prefixes.add("ibd");
		prefixes.add("sd");
		prefixes.add("act");
		prefixes.add("stm");
		prefixes.add("par");
		prefixes.add("pkg");
		
		for(IResource ir : this.folder.members())
		{
			IPath p = ir.getFullPath();
			File f = p.toFile();
			if(f.exists()) {
				pictures.add(folder.getFile(p));
			}
		}
	}
	
	/**
	 * check if all picture files are of <i>PIC_EXTENSION</i>
	 * @return - the result of comparison.
	 */
	private boolean checkFileFormat() {
		pictureNames = new ArrayList<String>();
		for(IFile f:this.pictures) {
			if(f.getFullPath().toFile().isFile()) {
				if(!f.getFileExtension().toLowerCase().equals(PIC_EXTENSION)) {
					return false;//TODO throw exception
				}else {
					pictureNames.add(f.getFullPath().removeFileExtension().lastSegment());
				}
			}
		}
		return true;
	}
	
	/**
	 * check if all the picture file prefixes are allowed.
	 * @return - the result of checking.
	 */
	private boolean checkPrefix() {
		for(String s:pictureNames) {
			// regex get prefix of s
			String pre = s.split(UNDERSCORE)[0];
			if(!prefixes.contains(pre)) {
				return false;// TODO exception
			}
		}
		return true;
	}
	
	/**
	 * check if there is a model requirement diagram.
	 * @param reqDName - the name of the model requirement diagram.
	 * @return - the checking result.
	 */
	private boolean checkRequirementDiagram(String reqDName) {
		// this.pictures.contains(new Path(reqDName+DOT+PIC_EXTENSION));
		return this.pictureNames.contains(reqDName);
	}
	
	/**
	 * check if the picture files have qualified names.
	 * @return - checking result.
	 */
	public boolean check() {
		if(checkFileFormat() && checkPrefix()) {
			return checkRequirementDiagram("req_Requirements");
		}
		return false;
	}

}
