package util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import plugin.Activator;

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
			IPath p = ir.getLocation();
			if(p!=null) {
				IFile f = folder.getFile(ir.getLocation().toFile().getName());
				pictures.add(f);
			}
		}
	}
	
	/**
	 * check if all picture files are of <i>PIC_EXTENSION</i>
	 * @return - the result of comparison.
	 * @throws PictureNamingException when the picture file extension is wrong
	 */
	private boolean checkFileFormat() throws PictureNamingException {
		pictureNames = new ArrayList<String>();
		for(IFile f:this.pictures) {
			if(f.getLocation().toFile().isFile()) {
				if(!f.getFileExtension().toLowerCase().equals(PIC_EXTENSION)) {// TODO or upper case? test
					throw new PictureNamingException("wrong picture file extension: "+
				f.getFileExtension()+", should be "+PIC_EXTENSION+" .");
				}else {
					pictureNames.add(f.getLocation().removeFileExtension().lastSegment());
				}
			}
		}
		return true;
	}
	
	/**
	 * check if all the picture file prefixes are allowed.
	 * @return - the result of checking.
	 * @throws PictureNamingException when prefix is not allowed.
	 */
	private boolean checkPrefix() throws PictureNamingException {
		for(String s:pictureNames) {
			// regex get prefix of s
			String pre = s.split(UNDERSCORE)[0];
			if(!pre.equals(s)&&!prefixes.contains(pre)) {
				throw new PictureNamingException("the picture file has illegal prefix "+pre+" .");
			}
		}
		return true;
	}
	
	/**
	 * check if there is a model requirement diagram.
	 * @param reqDName - the name of the model requirement diagram.
	 * @return - the checking result.
	 * @throws PictureNamingException when miss the model requirement diagram
	 */
	private boolean checkRequirementDiagram(String reqDName) throws PictureNamingException {
		// this.pictures.contains(new Path(reqDName+DOT+PIC_EXTENSION));
		if(this.pictureNames.contains(reqDName)) {
			return true;
		} else {
			throw new PictureNamingException("missing picture or wrong name: "+reqDName+DOT+PIC_EXTENSION+" .");
		}
	}
	
	/**
	 * check if there is a external interfaces diagram.
	 * @param eIDName - the name of the diagram.
	 * @return true if ok
	 * @throws PictureNamingException when the checking result is not ok.
	 */
	private boolean checkExternalInterfacesDiagram(String eIDName) throws PictureNamingException {
		if(this.pictureNames.contains(eIDName)) {
			return true;
		} else {
			throw new PictureNamingException("missing picture or wrong name: "+eIDName+DOT+PIC_EXTENSION+" .");
		}
	}
	
	/**
	 * check if there is a top level bdd.
	 * @param mSDName the bdd name
	 * @return true if there is.
	 * @throws PictureNamingException if there is not.
	 */
	private boolean checkModelStructureDiagram(String mSDName) throws PictureNamingException {
		if(this.pictureNames.contains(mSDName)) {
			return true;
		} else {
			throw new PictureNamingException("missing picture or wrong name: "+mSDName+DOT+PIC_EXTENSION+" .");
		}
	}
	
	/**
	 * check if the picture files have qualified names.
	 * @return checking result.
	 * @throws PictureNamingException when something went wrong.
	 */
	public boolean check() throws PictureNamingException {
		if(checkFileFormat()) { //  && checkPrefix()
			checkRequirementDiagram("req_Requirements");
			checkExternalInterfacesDiagram("bdd_ExternalInterfaces");
			checkModelStructureDiagram("bdd_ModelStructure");
			return true;
		}
		return false;
	}

}
