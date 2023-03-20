package plugin.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.obeonetwork.m2doc.ide.*;
import org.eclipse.papyrus.infra.gmfdiag.export.actions.ExportAllDiagramsParameter;
import org.eclipse.papyrus.infra.gmfdiag.export.engine.ExportAllDiagramsEngine;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.IService;
import org.eclipse.acceleo.query.runtime.ServiceUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.obeonetwork.m2doc.genconf.GenconfFactory;
import org.obeonetwork.m2doc.genconf.GenconfPackage;
import org.obeonetwork.m2doc.genconf.GenconfUtils;
import org.obeonetwork.m2doc.genconf.Generation;
import org.obeonetwork.m2doc.generator.DocumentGenerationException;
import org.obeonetwork.m2doc.parser.DocumentParserException;
import org.obeonetwork.m2doc.parser.ValidationMessageLevel;
import org.obeonetwork.m2doc.template.DocumentTemplate;
import org.obeonetwork.m2doc.util.*;

import plugin.Activator;

public class SampleHandler extends AbstractHandler {
	private String templateName="D:\\papyrus\\CoCoME-doc\\CoCoME-doc";
	private String modelName = "D:\\papyrus\\CoCoME\\CoCoME.uml";
	private String modelProjectName = "CoCoME";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Activator.debug("handler execution starts.");
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InputDialog dialog = new InputDialog(window.getShell(), "选择模板", "选择模板", templateName, null);
		if(dialog.open()==Window.OK) {
			try {
				this.templateName = dialog.getValue();
				InputDialog dialog2 = new InputDialog(window.getShell(), "选择模型", "选择模型", modelProjectName, null);
				if(dialog2.open()==Window.OK) {
					this.modelProjectName = dialog2.getValue();
					this.modelName = "D:\\papyrus\\"+this.modelProjectName+"\\"+this.modelProjectName+".uml";
					this.generateImages(event);
					 this.generateDoc(event);
				}
				
			} catch(IOException ioe) {
				Activator.log(ioe);
				System.err.println("there is an io exception.");
				ioe.printStackTrace();
			} catch(DocumentParserException dpe) {
				Activator.log(dpe);
				System.err.println("there is a parsing exception.");
				dpe.printStackTrace();
			} catch(ClassNotFoundException cnfe) {
				Activator.log(cnfe);
				System.err.println("there is a missing class exception.");
				cnfe.printStackTrace();
			} catch(DocumentGenerationException dge) {
				Activator.log(dge);
				System.err.println("there is a generating exception.");
				dge.printStackTrace();
			} 
		}
		return null;
	}
	
	public Boolean generateImages(ExecutionEvent event) throws ExecutionException {
		Activator.debug("enters generateImage().");
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject modelProject = myWorkspaceRoot.getProject(modelProjectName);
		
		if (modelProject.exists() && !modelProject.isOpen()) {
			try {
				modelProject.open(null);
			} catch (CoreException e) {
				Activator.log(e);
			}
			
		}
		
		// code from papyrus
		// ExportAllDiagramsEngine exportAllDiagrams = new ExportAllDiagramsEngine();
		ImageGeneration exportAllDiagrams = new ImageGeneration();

		IFile file = modelProject.getFile(modelProjectName+".di");
		URI diFileUri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
		ExportAllDiagramsParameter parameter = new ExportAllDiagramsParameter(diFileUri);
		// ExportDiagramsPage page = new ExportDiagramsPage(file.getParent());
		
		if(parameter != null) {
			IResource outputDirectory = modelProject;
			if(outputDirectory.isAccessible()) {
				parameter.setOutputDirectory(outputDirectory);
				// parameter.setExportFormat(exportPage.getExporter());
				// TODO qualifiedName = button.getSelection()
				parameter.setQualifiedName(true);

				exportAllDiagrams.initialise(parameter);
				// TODO wait for the pictures generated.
				IJobManager jobMan = Job.getJobManager(); 
				exportAllDiagrams.exportDiagramsToImages(null);
//				Job[] jobList = jobMan.find(null);
//				for(Job j:jobList) {
//					Activator.debug("the name of the job is : "+j.getName());
//					Activator.debug("the group of the job is : "+j.getJobGroup());
//				}
//				try {
//					Activator.debug("generate image job waiting...");
//					jobMan.join(exportAllDiagrams, null);
//					Activator.debug("generate image job done.");
//				} catch (OperationCanceledException | InterruptedException e) {
//					Activator.log(e);
//				}
				Activator.debug("export image ok.");
			} else {
				Activator.log(IStatus.ERROR, "export image has occurred an error.");
				
			}
		}
			
		Activator.debug("generateImage() exits.");
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public Boolean generateDoc(ExecutionEvent event) throws ExecutionException, IOException, 
	DocumentParserException, ClassNotFoundException, DocumentGenerationException {
		final URI templateURI = URI.createFileURI(templateName+"."+M2DocUtils.DOCX_EXTENSION_FILE); // the URI of the template
		final URI modelURI = URI.createFileURI(modelName);
        
		// can be empty, if you are using a Generation use GenconfUtils.getOptions(generation)
		
		final Map<String, String> options = new HashMap<>();
		// TODO template properties?
//		options.put("nsURI", "http://www.eclipse/org/uml2/5.0.0/UML");
//		options.put("TemplateURI", templateName+"."+M2DocUtils.DOCX_EXTENSION_FILE);
		List<Exception> exceptions = new ArrayList<>();
		        
		final ResourceSet resourceSetForModels = M2DocUtils.createResourceSetForModels(exceptions , "key", new ResourceSetImpl(), options);
		//resourceSetForModels.getResource(modelURI, true);
		final Resource r = resourceSetForModels.getResource(modelURI, true);
		final EObject value = r.getContents().get(0);

		// if you are using a Generation, use GenconfUtils.getQueryEnvironment(resourceSetForModels, generation)
		final IQueryEnvironment queryEnvironment = M2DocUtils.getQueryEnvironment(resourceSetForModels, templateURI, options);
		// TODO query environment of AQL
//		final Set<IService> s = ServiceUtils.getServices(queryEnvironment, value.eINSTANCE);
//		ServiceUtils.registerServices(queryEnvironment, s);
		        
		final IClassProvider classProvider = M2DocPlugin.getClassProvider(); //  new ClassProvider(this.getClass().getClassLoader())
        final Monitor monitor = new BasicMonitor.Printing(System.out);
		try (DocumentTemplate template = M2DocUtils.parse(resourceSetForModels.getURIConverter(), templateURI, queryEnvironment, classProvider, monitor)) {
			
		    // validate
			final ValidationMessageLevel validationLevel = M2DocUtils.validate(template, queryEnvironment, monitor);
			if (validationLevel != ValidationMessageLevel.OK) {
			    final URI validationResulURI = URI.createFileURI(templateName+"-validation."+M2DocUtils.DOCX_EXTENSION_FILE); // some place to serialize the result of the validation
			    M2DocUtils.serializeValidatedDocumentTemplate(resourceSetForModels.getURIConverter(), template, validationResulURI);
			    Activator.log(IStatus.ERROR, template.getBody().getValidationMessages().toString());
			}
			
			//generate
			final Map<String, Object> variables = new HashMap<>(); // your variables and values
			//variables.put("self", classProvider.getClass("CoCoME", null));
			variables.put("self", value);
			
		} catch(Exception e){
			Activator.log(e);
		} finally {
		
		    M2DocUtils.cleanResourceSetForModels("key", resourceSetForModels);
		    Activator.debug("generateDoc() exits.");
		}
		return true;
	}
	
	
}
