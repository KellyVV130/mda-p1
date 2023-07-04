package util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.edit.ui.EMFEditUIPlugin;
import org.eclipse.ocl.pivot.Constraint;
import org.eclipse.ocl.pivot.ExpressionInOCL;
import org.eclipse.ocl.pivot.internal.labels.LabelSubstitutionLabelProvider;
import org.eclipse.ocl.pivot.utilities.OCL;
import org.eclipse.ocl.pivot.utilities.ParserException;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.Stereotype;
import org.osgi.framework.Bundle;

import plugin.Activator;

public class UMLModelChecker {
	private ResourceSet rs;
	private Model r = null;
	//	private List<String> errorList;
	private List<String> firstLevelPackageRules;
	private List<String> requirementPackageRules;
	
	/**
	 * constructor
	 * 
	 * @param resource - the resource need to be checked
	 */
	public UMLModelChecker(ResourceSet resource) {
		this.rs = resource;
//		this.errorList = new ArrayList<String>();
		this.firstLevelPackageRules = setFirstLevelPackageRules();
		this.requirementPackageRules = setrequirementPackageRules();
		return;
	}
	
//	void addErrorToList(String error) {
//		this.errorList.add(error);
//	}
	
	/**
	 * Check if the root element is Model
	 * 
	 * @return true if ok, throw an exception if not ok
	 * @throws ModelFormatException
	 */
	boolean checkIfUML() throws ModelFormatException {
		if(rs.getResources().get(0).getContents().get(0) instanceof Model) {
			this.r = (Model)rs.getResources().get(0).getContents().get(0);
			return true;
		} else {
			this.r = null;
			throw new ModelFormatException(1, "顶级模型元素必须是<Model>。");
		}
	}
	
	/**
	 * Check if the first level packages are of <i>firstLevelPackageRules</i>
	 * 
	 * @return true if ok, else throw an exception
	 * @throws ModelFormatException
	 */
	boolean checkFirstLevelPackage() throws ModelFormatException {
		List<String> name_list = new ArrayList<String>();
		for (EObject eObj : r.eContents()) {
			if (eObj instanceof Package) {
				Package p = (Package)eObj;
				name_list.add(p.getName());
			} else if(!(eObj instanceof PackageImport)&&!(eObj instanceof ProfileApplication)) {
				throw new ModelFormatException(2, "定义模型元素<Model>的所有子元素需为<Package>。");
			}
		}
		for(String rule : this.firstLevelPackageRules) {
			if(!name_list.contains(rule)) {
				throw new ModelFormatException(2,"缺少"+rule+"包。");
			}
		}
		return true;
	}
	
	/**
	 * Check if the Requirement Package is good.
	 * 
	 * @return
	 * @throws ModelFormatException
	 */
	boolean checkRequirementPackage() throws ModelFormatException {
		Package rqt=null;
		List<EObject> l= filterElement((List<EObject>)r.eContents(), Package.class);
		rqt = (Package)findModelElementFromFather(l, "Requirement", Package.class);
		if(rqt!=null) {
			for (EObject eObj : rqt.eContents()) {
				if (!(eObj instanceof Package)) {
					throw new ModelFormatException(3, "Requirement包的子元素必须全部是<Package>。");
				} else {
					for(EObject j : eObj.eContents()) {
						if(j instanceof Package) {
							checkPackageSiblings(j, 4, true);
						} else {
							checkPackageSiblings(j, 4, false);
						}
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	boolean checkStructurePackage() throws ModelFormatException {
		Package stc = null;
		Stereotype BLOCK = r.getOwnedStereotype("SysML::Blocks::Block");
		

		List<EObject> l= filterElement((List<EObject>)r.eContents(), Package.class);
		stc = (Package)findModelElementFromFather(l, "Structure", Package.class);
		if(stc!=null) {
			for (EObject eObj : stc.eContents()) { // TODO other way to get the blocks
				if( eObj instanceof org.eclipse.uml2.uml.Class) {
					if(((org.eclipse.uml2.uml.Class)eObj).getAppliedStereotypes().contains(BLOCK)) {
						// Activator.debug(i+"  "+eObj.toString());
						// TODO check if the block has one layer of child block, and no other block.
						// check if there is one and one only system block.
						continue;
					}
				}
			}
		}
		return false;
	}
	
	List<String> setFirstLevelPackageRules(){
		List<String> rules = new ArrayList<String>();
		rules.add("Requirement");
		rules.add("Structure");
		rules.add("Behavior");
		rules.add("Types");
		return rules;
	}
	
	List<String> setrequirementPackageRules(){  // TODO regex?
		List<String> rules = new ArrayList<String>();
		rules.add("Requirement");
		rules.add("Structure");
		rules.add("Behavior");
		rules.add("Types");
		return rules;
	}
	
	/**
	 * The main check function that is visible to others.
	 * 
	 * @return
	 * @throws ModelFormatException
	 */
	public boolean check() throws ModelFormatException {
		checkIfUML();
//		if(r!=null) {
//			if(checkFirstLevelPackage()&&checkRequirementPackage() && checkStructurePackage()) {
//				return true;
//			} else {
//				Activator.debug("奇怪的事情发生了");
//				return false;
//			}
//		}
		
		Map<String, String> ConstraintNameMap = new HashMap<String, String>();
//		ConstraintNameMap.put("TopLevelPackageNaming", 
//				"There must be 4 packages named 'Requirements', 'Behaviors', 'Types', 'Structure'
//		at the first level under the Model. Cannot find some of them.");
		ConstraintNameMap.put("RequirementPackageExists", 
				"Couldn't find the 'Requirements' package, please check if it is of the right name.");
		ConstraintNameMap.put("PerformanceRequirementConstraint", 
				"Couldn't find the PerformanceRequirement element in 'Requirements' package, please check if it is of the right name.");
		ConstraintNameMap.put("StructurePackageExists", 
				"Couldn't find the 'Structure' package, please check if it is of the right name.");
		ConstraintNameMap.put("StructureNoPackage", 
				"There shouldn't be any packages in 'Structure' package.");
		ConstraintNameMap.put("TwoLayerOfBlock", 
				"There should be two layers of Block at most.");
		ConstraintNameMap.put("BehaviorsPackageExists", 
				"Couldn't find the 'Behaviors' package, please check if it is of the right name.");
		ConstraintNameMap.put("ExternalInterfacesExists", 
				"Couldn't find the ExternalInterfaces package in 'Behaviors' package, please check if it is of the right name.");
		ConstraintNameMap.put("BehaviorsPackageConstraint", 
				"The 'Behaviors' pakcage should at least contain one package and no non-package elements.");
		ConstraintNameMap.put("TypesPackageExists", 
				"Couldn't find the 'Types' package, please check if it is of the right name.");
		ConstraintNameMap.put("TypesPackageConstraint", 
				"There should at least be 1 DataType element and none package elements in 'Types' package.");
		
		OCL ocl = OCL.newInstance(rs);
		try {
			Bundle bundle = Platform.getBundle(Activator.getPluginID());
			Path ipath = new Path("resources/ModelConstraints4UML.ocl");
			URL url = FileLocator.find(bundle, ipath, null);
			url = FileLocator.toFileURL(url);
			File file = URIUtil.toFile(URIUtil.toURI(url));
			URI uri = URI.createFileURI(file.getPath());
			// parse the contents as an OCL document
			Resource asResource = ocl.parse(uri); 
				
			// accumulate the document constraints in constraintMap and print all constraints
			Map<String, Constraint>constraintMap = new LinkedHashMap<String,Constraint>();
		    for (TreeIterator<EObject> tit = asResource.getAllContents(); tit.hasNext(); ) {
			    	EObject next = tit.next();
			    	if (next instanceof Constraint) {
				        Constraint constraint = (Constraint)next;
				        ExpressionInOCL expressionInOCL = null;
						expressionInOCL = ocl.getSpecification(constraint);
				        if (expressionInOCL != null) {
							String name = constraint.getName();
							if (name != null) {
								constraintMap.put(name, constraint);
							}
						}
			    	}
			    }
		    
		    if(r!=null) {
		    	for(String name:constraintMap.keySet()) {
			    	Constraint eio = constraintMap.get(name);
					Boolean val = ocl.check(r, eio);
					if(!val) {
		        		throw new ModelFormatException(1, ConstraintNameMap.get(name));
					}
			    }
		    } else {
		    	throw new ModelFormatException(0, "Read the uml file fail! Maybe it doesn't exist.");
		    }
		    
		    
				    
		} catch (ParserException | URISyntaxException | IOException e) {
			ocl.dispose();
			throw new ModelFormatException(0, "Compling ocl file fail!");
		}
		
		return false;
	}
	
	public class MyDiagnostician extends Diagnostician
	{
		@Override
		public Map<Object, Object> createDefaultContext() {
			Map<Object, Object> context = super.createDefaultContext();
			context.put(EValidator.SubstitutionLabelProvider.class,
				new LabelSubstitutionLabelProvider());
			return context;
		}

		public BasicDiagnostic createDefaultDiagnostic(Resource resource) {
			return new BasicDiagnostic(EObjectValidator.DIAGNOSTIC_SOURCE, 0,
				EMFEditUIPlugin.INSTANCE.getString(
					"_UI_DiagnosisOfNObjects_message", new String[]{"1"}),
				new Object[]{resource});
		}

		public Diagnostic validate(Resource resource) {
			BasicDiagnostic diagnostics = createDefaultDiagnostic(resource);
			Map<Object, Object> context = createDefaultContext();
			for (EObject eObject : resource.getContents()) {
				validate(eObject, diagnostics, context);
			}
		    return diagnostics;
		}
	}
	
	/**
	 * Check the element's all siblings if they are all packages or all non-packages.
	 * 
	 * @param pkg - the main element whose siblings need to be checked
	 * @param l - the exception position
	 * @param flag - if the siblings should be package or not
	 * @return
	 * @throws ModelFormatException
	 */
	boolean checkPackageSiblings(EObject pkg, int l, boolean flag) throws ModelFormatException {
		EObject container = pkg.eContainer();
		for(EObject sib : container.eContents()) {
			if(flag ? !(sib instanceof Package):(sib instanceof Package)) {
				throw new ModelFormatException(l, flag?"<Package>元素的兄弟元素只能是<Package>。":
					"非<Package>元素的兄弟元素不能是<Package>。");
			}
		}
		return true;
	}
	
	/**
	 * Find the named element in Class <i>c</i>
	 * 
	 * @param father - the place to find the target object
	 * @param name - the name of the target object
	 * @param c - the target class
	 * @return
	 * @throws ClassNotFoundException 
	 */
	EObject findModelElementFromFather(List<EObject> father, String name, Class<? extends EObject> c) {
		for(EObject eObj : father) {
			if(c.isInstance(eObj)) {
				if(((NamedElement)eObj).getName().equals(name)) {
					return eObj;
				}
			}
		}
		return null;
	}
	
	/**
	 * Sselect all direct children of <i>list</i> which is of class <i>c</i>.
	 * This method does not change the original list.
	 * 
	 * @param list - the original list
	 * @param c - the target class
	 * @return ret - a list
	 * @throws ClassNotFoundException 
	 */
	List<EObject> filterElement(List<EObject> list, Class<? extends EObject> c){
		List<EObject> ret = new ArrayList<EObject>();
		for(EObject eObj : list) {
			if (c.isInstance(eObj)) {
				ret.add(eObj);
			}
		}
		return ret;
	}

}
