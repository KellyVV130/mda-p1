package util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	 * the constructor
	 * @param picturesFolder - folder containing picture files. 
	 * Should be in the same folder of the uml file
	 * @throws CoreException
	 */
	public PicturesChecker(IFolder picturesFolder) {
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
		
		try {
			for(IResource ir : this.folder.members())
			{
				IPath p = ir.getLocation();
				if(p!=null) {
					IFile f = folder.getFile(ir.getLocation().toFile().getName());
					pictures.add(f);
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					throw new PictureNamingException("错误图片格式："+
				f.getFileExtension()+"，应为"+PIC_EXTENSION+"。");
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
			String pattern = "^([\\u4e00-\\u9fa5|\\d|a-zA-Z]+)_[\\u4e00-\\u9fa5|\\d|a-zA-Z]+$";
	        Pattern r = Pattern.compile(pattern);
	        Matcher m = r.matcher(s);
			String pre = "";
	        if(m.find()) {
	        	pre = m.group(1);
	        }
			if(pre.length()==0) {
				throw new PictureNamingException("图片命名格式不合法:"+s
						+"，应为<图片种类缩写>_<图片描述的元素名称>。");
			}
			else if(!prefixes.contains(pre)) {
				throw new PictureNamingException("图片前缀不合法："+pre+"。");
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
			throw new PictureNamingException("缺少该图片或命名错误："+DName+DOT+PIC_EXTENSION+"。");
		}
	}
	
	/**
	 * check if the picture files have qualified names.
	 * @return checking result.
	 * @throws PictureNamingException when something went wrong.
	 */
	public boolean check() throws PictureNamingException {
		if(checkFileFormat()) { 
			checkPrefix();
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
