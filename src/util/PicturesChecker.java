package util;

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
	private String PIC_EXTENSION="PNG";
	
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
				if(!f.getFileExtension().equals(PIC_EXTENSION)) {// TODO or upper case? test
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
	 * check if there is a specific diagram named DName.
	 * @param DName - the name of the model requirement diagram.
	 * @return - the checking result.
	 * @throws PictureNamingException when miss the model requirement diagram
	 */
	private boolean checkDiagramByName(String DName) throws PictureNamingException {
		if(this.pictureNames.contains(DName)) {
			return true;
		} else {
			throw new PictureNamingException("missing picture or wrong name: "+DName+DOT+PIC_EXTENSION+".");
		}
	}
	
	/**
	 * check if the picture files have qualified names.
	 * @return checking result.
	 * @throws PictureNamingException when something went wrong.
	 */
	public boolean check() throws PictureNamingException {
		if(checkFileFormat()) { 
			// checkPrefix();
			checkDiagramByName("req_Requirements");
			checkDiagramByName("bdd_ExternalInterfaces");
			checkDiagramByName("bdd_ModelStructure");
			checkDiagramByName("pkg_ModelStructure");
			checkDiagramByName("bdd_Interfaces");
			checkDiagramByName("bdd_Types");
			checkDiagramByName("uc_Actors");
			return true;
		}
		return false;
	}

}
