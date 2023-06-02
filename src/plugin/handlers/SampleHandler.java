package plugin.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.obeonetwork.m2doc.ide.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.obeonetwork.m2doc.generator.DocumentGenerationException;
import org.obeonetwork.m2doc.parser.DocumentParserException;
import org.obeonetwork.m2doc.parser.ValidationMessageLevel;
import org.obeonetwork.m2doc.template.DocumentTemplate;
import org.obeonetwork.m2doc.util.*;

import plugin.Activator;
import wizard.ExportAndGenerateWizard;

public class SampleHandler extends AbstractHandler {
	private String templateName="D:\\papyrus\\CoCoME-doc\\CoCoME-doc";
	private String modelName = "D:\\papyrus\\CoCoME\\CoCoME.uml";
	private String modelProjectName = "CoCoME";

	private ExportAndGenerateWizard wizard = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Activator.debug("handler execution starts.");
//		wizard = new ExportAndGenerateWizard();
//		WizardDialog dialog = new WizardDialog(getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
//        dialog.create();
//        dialog.open();
        
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		InputDialog dialog = new InputDialog(window.getShell(), "选择模板", "选择模板", templateName, null);
//		if(dialog.open()==Window.OK) {
//			try {
//				this.templateName = dialog.getValue();
//				InputDialog dialog2 = new InputDialog(window.getShell(), "选择模型", "选择模型", modelProjectName, null);
//				if(dialog2.open()==Window.OK) {
//					this.modelProjectName = dialog2.getValue();
//					this.modelName = "D:\\papyrus\\"+this.modelProjectName+"\\"+this.modelProjectName+".uml";
//					this.generateImages(event);
//					 this.generateDoc(event);
//				}
//				
//			} catch(IOException ioe) {
//				Activator.log(ioe);
//				System.err.println("there is an io exception.");
//				ioe.printStackTrace();
//			} catch(DocumentParserException dpe) {
//				Activator.log(dpe);
//				System.err.println("there is a parsing exception.");
//				dpe.printStackTrace();
//			} catch(ClassNotFoundException cnfe) {
//				Activator.log(cnfe);
//				System.err.println("there is a missing class exception.");
//				cnfe.printStackTrace();
//			} catch(DocumentGenerationException dge) {
//				Activator.log(dge);
//				System.err.println("there is a generating exception.");
//				dge.printStackTrace();
//			} 
//		}
		return null;
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
