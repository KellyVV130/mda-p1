package wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.uml2.uml.Model;
import org.obeonetwork.m2doc.genconf.GenconfPackage;
import org.obeonetwork.m2doc.genconf.GenconfUtils;
import org.obeonetwork.m2doc.genconf.Generation;
import org.obeonetwork.m2doc.genconf.ModelDefinition;
import org.obeonetwork.m2doc.genconf.presentation.M2docconfEditorPlugin;
import org.obeonetwork.m2doc.generator.DocumentGenerationException;
import org.obeonetwork.m2doc.ide.M2DocPlugin;
import org.obeonetwork.m2doc.parser.DocumentParserException;
import org.obeonetwork.m2doc.services.TemplateRegistry;
import org.obeonetwork.m2doc.util.M2DocUtils;
import org.osgi.framework.Bundle;

import plugin.Activator;
import util.ModelFormatException;
import util.PictureNamingException;
import util.PicturesChecker;
import util.UMLModelChecker;


public class ExportAndGenerateWizard extends Wizard implements IExportWizard {

	/** Selected file. */
	private IFile file;
	
	private TemplateSelectionPage templatePage;
    
    private UMLModelChecker checker = null;
    
    private URI umlFileUri;
	/**
	 * Constructor.
	 *
	 */
	public ExportAndGenerateWizard() {
		super();
		setWindowTitle("导出文档");
		templatePage = new TemplateSelectionPage("选择.docx模板文件", ".docx");
	}
	
	protected void registerTemplates() {
		Bundle bundle = Platform.getBundle(Activator.getPluginID());
		Path ipath = new Path("resources");
		URL url = FileLocator.find(bundle, ipath, null);
		// clear the template set
		if(TemplateRegistry.INSTANCE.getTemplates().size()!=4) {
			for (String s:TemplateRegistry.INSTANCE.getTemplates().keySet()) {
				TemplateRegistry.INSTANCE.unregisterTemplate(s);
			}
		
			try {
				url = FileLocator.toFileURL(url);
				File files = URIUtil.toFile(URIUtil.toURI(url));
				if(files.isDirectory()) {
					for(File f : files.listFiles()) {
						if(f.isFile()&&(f.getPath().endsWith(".docx")||f.getPath().endsWith(".doc"))) {
							TemplateRegistry.INSTANCE.registerTemplate(f.getName(), URI.createFileURI(f.getPath()));
						}
					}
				}
			} catch (IOException | URISyntaxException e) {
				Activator.log(IStatus.ERROR, "initialize plugin fail.");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 *
	 * @param workbench
	 * @param selection
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		
		registerTemplates();
		
		file = null;
		
		file = (IFile) selection.getFirstElement();// SelectionHelper.convertSelection2File(selection);
		
		if (file != null) {
			if(file.getFileExtension().equals("uml")) {
				umlFileUri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
			}
		} else {
			// TODO exception?
		}
	}
	
	@Override
	public void addPages() {
		addPage(templatePage);
		// gPage = new GenerationCreationPage(templatePage, variableValue);
		// addPage(gPage);
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 *
	 * @return
	 */
	@Override
	public boolean canFinish() {
		return file != null && super.canFinish();
	}
	

    private final class FinishJob extends WorkspaceJob {
    	
    	private final String templateName;

        /**
         * The variable value.
         */
        private EObject variableValue;

        /**
         * The variable name.
         */
        private final String variableName;

        /**
         * Tells if we should launch the generation.
         */
        private final boolean launchGeneration;

        /**
         * The project name.
         */
        private final String projectName;
        
        /**
         * The model uri.
         */
        final URI modelURI;

        /**
         * The genconf {@link URI}.
         */
        private final URI genconfURI;

        /**
         * The result {@link URI}.
         */
        private final URI destinationURI;

        /**
         * The validation {@link URI}.
         */
        private final URI validationURI;
        
        private final String FOLDER_NAME = "/docs/";
        
        private final String DOT=".";

        /**
         * Constructor.
         * 
         * @param name
         *            the job name
         * @param projectName
         *            the project name
         * @param templateName
         *            the template name
         */
        private FinishJob(String name, String projectName, String templateName) {
            super(name);
            this.templateName=templateName;
            this.variableName = "self";
            this.launchGeneration = true;
            this.projectName = projectName;
            
            IPath modelPath = new Path(CommonPlugin.resolve(umlFileUri).toFileString());
            modelPath=modelPath.removeLastSegments(1);
            modelURI = URI.createPlatformResourceURI(this.projectName+FOLDER_NAME, true);
            String tName = new Path(this.templateName).removeFileExtension().lastSegment();
            
            this.genconfURI = URI.createPlatformResourceURI(URI.createURI(modelURI.toPlatformString(true)+ 
            		tName + DOT + GenconfUtils.GENCONF_EXTENSION_FILE).path(),true);
            this.destinationURI = URI.createPlatformResourceURI(URI.createURI(modelURI.toPlatformString(true)+ 
            		tName + "-result." + M2DocUtils.DOCX_EXTENSION_FILE).resolve(modelURI).path(),true);
            this.validationURI = URI.createPlatformResourceURI(URI.createURI(modelURI.toPlatformString(true)+ 
            		tName + "-validation." + M2DocUtils.DOCX_EXTENSION_FILE).resolve(modelURI).path(),true);
        }
        

        private boolean fixGeneratedDocument(URI uri) throws FileNotFoundException, IOException {
        	IWorkspace workspace = ResourcesPlugin.getWorkspace();
        	IWorkspaceRoot workspaceRoot = workspace.getRoot();
        	IPath filePath = workspaceRoot.getFile(new Path(uri.toPlatformString(true))).getLocation();
        	File wordFile = filePath.toFile();
        	// File wordFile = new File("C:\\Users\\wy200\\Desktop\\test.docx");
        	if(wordFile.exists()) {
        		XWPFDocument doc = new XWPFDocument(new FileInputStream(wordFile));
        		List<XWPFParagraph> paragraphList = doc.getParagraphs();
        		Activator.debug(paragraphList.size()+"");
                for (XWPFParagraph para : paragraphList) {
                	if(para.getText().trim().length()>0) {
                		for (XWPFRun run : para.getRuns()) {
	                        String text = run.text();
	                        if(text.trim().length()>0) {
	                        	Activator.debug("run: "+text);
		                        text = text.replaceAll("An I/O Problem occured while reading", "GJ");
		                        // TODO let run to be sentence!
		                        // does not exist..
		                        run.setText(text, 0);
	                        }
	                        
	                    }
                	}
                }
                doc.write(new FileOutputStream(wordFile));
        	}
        	
        	return true;
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            Status status = new Status(IStatus.OK, M2docconfEditorPlugin.getPlugin().getSymbolicName(),
                    "M2Doc project " + projectName + " created succesfully.");
            
            try {
        		ResourceSet resourceSet = new ResourceSetImpl();
        		resourceSet.createResource(umlFileUri);
        		Resource r = resourceSet.getResource(umlFileUri, true);
                checker = new UMLModelChecker(resourceSet);
    			checker.check();
    			this.variableValue = ((Model) r.getContents().get(0));
            	final IContainer container;
                IPath containerFullPath=new Path(projectName+"/docs");
                if (containerFullPath.segmentCount() == 1) {
                    container = ResourcesPlugin.getWorkspace().getRoot().getProject(containerFullPath.lastSegment());
                } else {
                    IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(containerFullPath);
                    if(!folder.exists()) {
                    	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(containerFullPath.segment(0));
                    	if(!project.exists()) {
                    		project.create(monitor);
                    	}
                		project.open(monitor);
                		folder.create(true, true, monitor);
                    }
                    container = folder;
                }
                IPath picturesPath = new Path(projectName+"/pics");
                IFolder picturesFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(picturesPath);
                if(!picturesFolder.exists()) {
                	throw new ModelFormatException(0,"There should be a folder containing pictures in the "+projectName+" folder!");
                }
                // new class to check the picturesFolder
                PicturesChecker pc = new PicturesChecker(picturesFolder);
                pc.check();
                URI templateURI = templatePage.getSelectedTemplateURI();
                if (templateURI != null) {
                    try (InputStream is = URIConverter.INSTANCE.createInputStream(templateURI)) {
                        final IFile tfile = container.getFile(new Path(templateURI.lastSegment()));
                        if (!tfile.exists()) {
                            tfile.create(is, true, new NullProgressMonitor());
                        } else if (openConfirmationDialog(tfile)) {
                            tfile.setContents(is, true, true, new NullProgressMonitor());
                        }
                        templateURI = URI.createPlatformResourceURI(tfile.getFullPath().toString(), true);
                    } catch (IOException e) {
                        status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "can't open input stream", e);
                        Activator.getDefault().getLog().log(status);
                    } catch (CoreException e) {
                        status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "can't save file", e);
                        Activator.getDefault().getLog().log(status);
                    }
                } else {
                    status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "模板文件路径错误。");
                }
                
                // do these things in the container.
                final Generation generation = createGenconf(genconfURI, destinationURI, validationURI, templateURI,
                        variableName, this.variableValue);
                if (launchGeneration) {
                    List<URI> generatedUris = GenconfUtils.generate(generation, 
                    		M2DocPlugin.getClassProvider(), BasicMonitor.toMonitor(monitor));
                    if(generatedUris.size()>1) {
                    	// TODO exception happens.
                    } else {
                    	// TODO convert emf uri to file or name and send to apache poi, method to modify the generated documents.
                    	// fixGeneratedDocument(generatedUris.get(0));
                    }
                }
            } catch (IOException | DocumentGenerationException | DocumentParserException 
            		| ModelFormatException | PictureNamingException e) {
                status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        e.getMessage(), e);
            }

            return status;
        }
    }
    
    private Generation createGenconf(URI genconfURI, URI destinationURI, URI validationURI, URI templateURI,
            String variableName, EObject variableValue) throws IOException {
        final Generation res;

        final ResourceSet resourceSet = new ResourceSetImpl();
        final Resource resource;
        if (resourceSet.getURIConverter().exists(genconfURI, null)) {
            resource = resourceSet.getResource(genconfURI, true);
        } else {
            resource = resourceSet.createResource(genconfURI);
        }
        res = GenconfPackage.eINSTANCE.getGenconfFactory().createGeneration();
        resource.getContents().clear();
        resource.getContents().add(res);
        res.setTemplateFileName(URI.decode(templateURI.deresolve(genconfURI).toString()));
        res.setResultFileName(URI.decode(destinationURI.deresolve(genconfURI).toString()));
        res.setValidationFileName(URI.decode(validationURI.deresolve(genconfURI).toString()));

        final ModelDefinition variableDefinition = GenconfPackage.eINSTANCE.getGenconfFactory().createModelDefinition();
        res.getDefinitions().add(variableDefinition);
        variableDefinition.setKey(variableName);
        variableDefinition.setValue(resourceSet.getEObject(EcoreUtil.getURI(variableValue), true));

        GenconfUtils.initializeOptions(res);

        resource.save(null);

        return res;
    }
    /**
     * Open {@link MessageBox}.
     * 
     * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
     */
    private final class OpenMessageBoxRunnable implements Runnable {

        /**
         * The file name.
         */
        private final String fileName;

        /**
         * The result of the {@link MessageBox}.
         */
        private int result;

        /**
         * Constructor.
         * 
         * @param fileName
         *            the file name
         */
        private OpenMessageBoxRunnable(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void run() {
            final MessageBox dialog = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
            dialog.setText(fileName + " already exists");
            dialog.setMessage("Do you want to overrite it?");
            result = dialog.open();
        }

        /**
         * Gets the result of the {@link MessageBox}.
         * 
         * @return the result of the {@link MessageBox}
         */
        public int getResult() {
            return result;
        }

    }
    
    /**
     * Tells if the given {@link IFile} should be overritten.
     * 
     * @param file
     *            the {@link IFile} to prompt for
     * @return <code>true</code> if the given {@link IFile} should be overritten, <code>false</code> otherwise
     */
    private boolean openConfirmationDialog(IFile file) {
        final OpenMessageBoxRunnable openDialogRunnable = new OpenMessageBoxRunnable(file.getName());
        Display.getDefault().syncExec(openDialogRunnable);

        final int res = openDialogRunnable.getResult();
        return res == SWT.OK || res == SWT.YES;
    }

	/**
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 *
	 * @return
	 */
	@Override
	public boolean performFinish() {
		final String projectName = file.getParent().getName();
        final String templateName = templatePage.getTemplateName();

        final Job job = new FinishJob("Creating M2Doc project: " + projectName, projectName, templateName);
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.schedule();
        return true;
	}

}
